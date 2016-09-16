package android.webcrawler.osori.opencvhog.Adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.webcrawler.osori.opencvhog.Model.BluetoothDeviceData;
import android.webcrawler.osori.opencvhog.R;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by 건주 on 2016-02-28.
 * 블루투스 기기 리스트를 보여주기 위한 어댑터
 */
public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDeviceData> {

    public BluetoothDeviceAdapter(Context context, int resource, int textViewResourceId, ArrayList<BluetoothDeviceData> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemLayout = super.getView(position, convertView, parent);

        ViewHolder viewHolder = (ViewHolder)itemLayout.getTag();
        // 뷰 홀더 설정
        if(viewHolder == null) {
            viewHolder = new ViewHolder();

            /** 객체 설정 */
            viewHolder.textViewName = (TextView)itemLayout.findViewById(R.id.list_element_bluetooth_device_textView_name);
            viewHolder.textViewAddress = (TextView)itemLayout.findViewById(R.id.list_element_bluetooth_device_textView_mac);

            /** 폰트 설정 */
            Typeface fontArial = Typeface.createFromAsset(getContext().getAssets(), "fonts/arial.ttf");
            viewHolder.textViewName.setTypeface(fontArial);
            viewHolder.textViewAddress.setTypeface(fontArial);

            itemLayout.setTag(viewHolder);
        }

        String name = getItem(position).getName();
        String address = getItem(position).getAddress();

        if(name != null){
            viewHolder.textViewName.setText("Name : " + name);
        }

        if(address != null){
            viewHolder.textViewAddress.setText("Address : " + address);
        }

        return itemLayout;
    }

    private class ViewHolder{
        TextView textViewName, textViewAddress;
    }
}
