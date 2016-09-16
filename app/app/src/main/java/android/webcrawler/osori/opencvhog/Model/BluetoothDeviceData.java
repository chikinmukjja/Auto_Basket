package android.webcrawler.osori.opencvhog.Model;

/**
 * Created by 건주 on 2016-02-28.
 * 블루투스 디바이스 정보 저장을 위한 클래스
 */
public class BluetoothDeviceData {
    private String name;        // Bluetooth Device 이름
    private String address;     // Bluetooth Device 맥 주소

    public BluetoothDeviceData(String name, String address){
        this.name       = name;
        this.address    = address;
    }

    public String getName(){
        return this.name;
    }

    public String getAddress(){
        return this.address;
    }
}
