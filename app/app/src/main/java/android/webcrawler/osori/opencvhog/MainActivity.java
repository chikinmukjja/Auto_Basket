package android.webcrawler.osori.opencvhog;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.webcrawler.osori.opencvhog.Common.Constant;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public native int loadCascade();
    public native int calcOpticalFlow(long mPrev, long mCurr);
    public native int hogDetection(long mCurr);

    private static final int TIME_INTERVAL    = 3000;        //  통신 간격
    private static final int MAX_HEIGHT_SIZE  = 500;         //  높이
    private static final int MAX_WIDTH_SIZE   = 500;         //  가로
    private static final String TAG           = "KKJ_DEBUG";

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mCurr;              // 이전 이미지
    private Mat mPrev   = null;     // 이후 이미지

    /** 블루투스 통신과 관련된 변수 */
    private String mAddress;
    private BluetoothSocket mSocket         = null;     // 통신에 사용하는 Socket
    private OutputStream mOutputStream      = null;
    private boolean connected               = false;    // 현재 블루투스 기기와 연결되어 있는 경우 true

    /** 스케쥴러로 3초마다 한번 씩 블루투스로 메시지 전송 */
    private ScheduledJob job;
    private Timer jobScheduler;

    /** Background thread 로 메시지를 보내는 스레드와 OpticalFlow, Hog 를 실행하는 스레드 */
    private BackGroundThread backGroundThread   = null;
    private SendMessageThread sendMessageThread = null;

    /** */
    public static boolean isSetting     = false;                    // 값이 true가 되면 optical flow, hog 계산
    public static Mat mPrevGray         = null;                     // 이전 화면 흑백
    public static Mat mCurrGray         = null;                     // 현재 화면 흑백
    public static Mat mCurrRgba         = null;                     // 현재 화면 칼라
    public static Object lock           = new Object();             // 락 변수
    public static int opticalSum    = 0;
    public static int foundSum      = 0;
    public static int frameNum      = 1;
    public static  Queue<String> MessageQueue;    // Message Queue

    static {
        System.loadLibrary("opencv_java");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("opticalFlow");// Load Native module
                    load_cascade();
                    backGroundThread = new BackGroundThread();
                    backGroundThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void load_cascade(){
        try {
            InputStream is      = getResources().openRawResource(R.raw.hogcascade_pedestrians);

            File mCascadeFile   = new File("/sdcard/hogcascade_pedestrians.xml");
            mCascadeFile.createNewFile();
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            loadCascade();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle bundleData = intent.getBundleExtra("DATA");
        if(bundleData == null){
            finish();
        }else{
            mAddress = bundleData.getString("ADDRESS");
        }


         BluetoothAdapter mBTAdapter  = BluetoothAdapter.getDefaultAdapter();
         BluetoothDevice mDevice     = mBTAdapter.getRemoteDevice(mAddress);
         if(mDevice != null){
         // 서버 블루투스에 연결 시도
             new ConnectedBluetoothDevice(mDevice).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
         }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase)
                findViewById(R.id.activity_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(MAX_WIDTH_SIZE, MAX_HEIGHT_SIZE);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        /** 스케쥴러 등록 */
        job             = new ScheduledJob();
        jobScheduler    = new Timer();

        jobScheduler.scheduleAtFixedRate(job, TIME_INTERVAL, TIME_INTERVAL);
    }

    private class ScheduledJob extends TimerTask {

        public void run() {
            double foundAverage;
            double opticalAverage;

            synchronized (lock){
                foundAverage = foundSum/(double)frameNum;
                opticalAverage = opticalSum/(double)frameNum;

                foundSum    = 0;
                frameNum    = 1;
                opticalSum  = 0;
            }
            if(connected == true && sendMessageThread != null){
                Log.d(TAG, "op : " + opticalAverage + "found" + foundAverage);
                sendMessageThread.sendMessage("" + opticalAverage + " " + foundAverage);
             }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
                    mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        if(mSocket != null && mSocket.isConnected()){
            // 현재 소켓이 연결 중인 경우, 소켓을 close 한다.
            try {
                mSocket.close();
            }catch(IOException closeException){}
        }
        if(sendMessageThread != null){
            // AsyncTask 종료
            sendMessageThread.cancel(true);
        }
        if(jobScheduler != null){
            jobScheduler.cancel();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        mCurr =  inputFrame.gray();
        if (frame != null) {
            if(mPrev != null){
                synchronized (lock) {
                    frameNum++;
                    opticalSum += calcOpticalFlow(mPrev.getNativeObjAddr(),
                            mCurr.getNativeObjAddr());
                    foundSum += hogDetection(mCurr.getNativeObjAddr());
                }
            }
            if(mPrev != null)   mPrev.release();
            mPrev = mCurr;
        }
        return frame;
    }

    /** BluetoothDevice 에 socket 연결을 시도하는 AsyncTask */
    private class ConnectedBluetoothDevice extends AsyncTask<String, Integer, Boolean>
    {
        private BluetoothDevice mDevice;

        public ConnectedBluetoothDevice(BluetoothDevice device){
            this.mDevice = device;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if(mDevice != null){
                try{
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(Constant.UUID);
                    }catch (IllegalArgumentException e){
                        Log.e(TAG, "Can't create uuid from String");
                        return false;
                    }
                    mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
                    try{
                        mSocket.connect();
                    }catch(IOException connectException){
                        try{
                            mSocket = (BluetoothSocket)mDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mDevice, 1);
                            mSocket.connect();
                        }catch(Exception exception){
                            Log.e(TAG, "BluetoothSocket connection Exception");
                            return false;
                        }
                        return true;
                    }
                }catch (IOException e){
                    e.printStackTrace();
                    Log.e(TAG, "BluetoothSocket connection Exception");
                    return false;
                }
                return true;
            }else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result == true){
                // 블루투스 연결 성공
                Toast.makeText(getApplicationContext(), "성공", Toast.LENGTH_SHORT).show();
                try{
                    mOutputStream = mSocket.getOutputStream();
                    sendMessageThread = new SendMessageThread();
                    sendMessageThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    connected = true;
                }catch (IOException e){
                    Toast.makeText(getApplicationContext(),"실패",Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "getOutputStream() Exception");
                    finish();
                }
            }else{
                // 블루투스 연결 실패
                Toast.makeText(getApplicationContext(),"실패",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /** 블루투스 서버로 메시지를 보낼 때 사용하는 AsyncTask */
    private class SendMessageThread extends AsyncTask<String, Integer, Boolean>{

        private Object messageLock;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MessageQueue    = new LinkedList<>();
            messageLock     = new Object();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            while (true) {
                 if(isCancelled() == true){
                    // AsyncTask 종료
                    break;
                }
                if(!MessageQueue.isEmpty()) {
                    String mSendMessage;
                    synchronized (messageLock) {
                        mSendMessage = MessageQueue.poll();
                    }
                    try {
                        byte[] bytes = mSendMessage.getBytes("UTF-8");
                        if (mOutputStream != null) {
                            mOutputStream.write(bytes);
                        }
                        Log.d(TAG, "sendMessage : " + mSendMessage);
                    } catch (Exception e) {}
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            // nothing to do
        }

        public void sendMessage(String message){
            if(message != null) {
                synchronized (messageLock) {
                    MessageQueue.offer(message);
                }
            }
        }
    }

    private class BackGroundThread extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }


        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            while (true) {
                if (MainActivity.isSetting == true) {
                    int opticalValue    = 0;
                    int found           = 0;


                    if(MainActivity.mPrevGray != null &&
                            MainActivity.mCurrGray != null) {
                            opticalValue = calcOpticalFlow(MainActivity.mPrevGray.getNativeObjAddr(),
                                MainActivity.mCurrGray.getNativeObjAddr());
                    }

                    if(MainActivity.mCurrRgba != null) {
                        found = hogDetection(MainActivity.mCurrRgba.getNativeObjAddr());
                    }

                    synchronized (lock) {
                        frameNum++;
                        opticalSum += opticalValue;
                        foundSum += found;
                    }

                    MainActivity.mPrevGray = null;
                    MainActivity.mCurrGray = null;
                    MainActivity.mCurrRgba = null;

                    isSetting = false;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // TODO Auto-generated method stub
        }

    }   // BackGroundThread

 }

