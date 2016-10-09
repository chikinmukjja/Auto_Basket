package android.webcrawler.osori.opencvhog.Common;


/**
 * Created by 건주 on 2016-02-20.
 * 공통된 상수 값들을 정리한 클래스입니다. 여러 액티비티에서 사용되는 상수값들을 이 클래스에서 정의해 줍니다
 */
public class Constant {
    public static final String UUID = "927e221b-32bf-47de-959c-01d17ece7008";   // UUID 값(서버와 클라이언트가 통일 되어야 한다)
    public static final String TAG  = "KKJ_DEBUG";

    public static final int MAX_HEIGHT_SIZE  = 400;         //  높이
    public static final int MAX_WIDTH_SIZE   = 500;         //  가로

    public static final int DIVIDE           = 4;           //  화면 분할 개수
    public static final int MAX_FRAME_NUMBER = 170;         //  Frame 수(1~170)
}
