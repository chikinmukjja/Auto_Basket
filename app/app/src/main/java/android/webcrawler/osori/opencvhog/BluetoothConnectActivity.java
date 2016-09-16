package android.webcrawler.osori.opencvhog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.webcrawler.osori.opencvhog.Adapter.BluetoothDeviceAdapter;
import android.webcrawler.osori.opencvhog.Model.BluetoothDeviceData;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by 건주 on 2016-02-28.
 * 블루투스 연결하기 전 액티비티로 연결을 시도할 기기를 선택한다.
 */

public class BluetoothConnectActivity extends FragmentActivity implements View.OnClickListener{

    private BluetoothAdapter mBTAdapter;

    /** 리스트뷰 변수와 어댑터 변수 */
    private ListView paringDeviceListView;
    private ListView discoverDeviceListVIew;
    private BluetoothDeviceAdapter paringDeviceAdapter;
    private BluetoothDeviceAdapter discoverDeviceAdapter;

    private BroadcastReceiver mDiscoveredDeviceReceiver;    // Discover 된 device 정보를 받기위한 브로드캐스트 리시버
    private Button btnDeviceSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        /** 객체 설정 */
        paringDeviceListView        = (ListView)findViewById(R.id.activity_bluetooth_connect_listView);
        discoverDeviceListVIew      = (ListView)findViewById(R.id.activity_bluetooth_connect_listView_discovery);

        /** 폰트 추가 */
        Typeface fontArial = Typeface.createFromAsset(getAssets(), "fonts/arial.ttf");
        ((TextView)findViewById(R.id.activity_bluetooth_connect_textView_title)).setTypeface(fontArial);

        /** 페어링 리스트뷰 헤더 추가 */
        View header = getLayoutInflater().inflate(R.layout.header_list_element_bluetooth_device, null);
        TextView headerTextView = (TextView)header.findViewById(R.id.header_list_element_bluetooth_device_textView_title);
        headerTextView.setTypeface(fontArial);
        paringDeviceListView.addHeaderView(header);

        /** Discovered 리스트뷰 헤더 추가 */
        View DiscoverHeader = getLayoutInflater().inflate(R.layout.header_list_element_bluetooth_device_discovery, null);
        ((TextView)DiscoverHeader.findViewById(R.id.header_list_element_bluetooth_device_discovery_textView_title)).setTypeface(fontArial);
        btnDeviceSearch = (Button)DiscoverHeader.findViewById(R.id.header_list_element_bluetooth_device_discovery_btn_search);
        btnDeviceSearch.setOnClickListener(this);
        discoverDeviceListVIew.addHeaderView(DiscoverHeader);

        /** 어댑터 생성 및 설정 */
        paringDeviceAdapter = new BluetoothDeviceAdapter(this, R.layout.list_element_bluetooth_device,
                R.id.list_element_bluetooth_device_textView_name, new ArrayList<BluetoothDeviceData>());
        if(paringDeviceAdapter != null){
            paringDeviceListView.setAdapter(paringDeviceAdapter);
        }

        discoverDeviceAdapter = new BluetoothDeviceAdapter(this, R.layout.list_element_bluetooth_device,
                R.id.list_element_bluetooth_device_textView_name, new ArrayList<BluetoothDeviceData>());
        if(discoverDeviceAdapter != null){
            discoverDeviceListVIew.setAdapter(discoverDeviceAdapter);
        }

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        /** 페어링 된 Device 추가 */
        getParingDevice();

        /** 브로드캐스팅 리시버 생성 */
        mDiscoveredDeviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device != null){
                        BluetoothDeviceData data = new BluetoothDeviceData(device.getName(), device.getAddress());
                        discoverDeviceAdapter.add(data);
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mDiscoveredDeviceReceiver, intentFilter);

        /** 리스트뷰 onItemClickListener 설정 */
        paringDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mBTAdapter.isDiscovering()){
                    // 블루투스 어댑터가 검색 중인 경우에 검색을 중지
                    mBTAdapter.cancelDiscovery();
                }
                String address = paringDeviceAdapter.getItem((int)id).getAddress();

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("ADDRESS", address);
                intent.putExtra("DATA", bundle);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /** 동적 리시버 해제 */
        if(mDiscoveredDeviceReceiver != null) {
            unregisterReceiver(mDiscoveredDeviceReceiver);
        }
    }

    /** 페이링 된 디바이스를 검색하고 검색 된 디바이스를 ArrayAdapter 에 추가하는 함수*/
    private void getParingDevice(){
        Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for (BluetoothDevice device : pairedDevices) {
                BluetoothDeviceData bluetoothDeviceData = new BluetoothDeviceData(device.getName(), device.getAddress());
                paringDeviceAdapter.add(bluetoothDeviceData);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            // 디바이스 검색 버튼
            case R.id.header_list_element_bluetooth_device_discovery_btn_search:
                if(mBTAdapter.isDiscovering()){
                    // 현재 디바이스 검색 중
                    mBTAdapter.cancelDiscovery();
                    btnDeviceSearch.setText("검색");
                }else{
                    // 현재 디바이스 검색 중이 아님
                    mBTAdapter.startDiscovery();
                    btnDeviceSearch.setText("중지");
                }
                break;
        }
    }
}
