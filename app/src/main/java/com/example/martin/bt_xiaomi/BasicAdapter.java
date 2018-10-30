package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/*
This Adapter lets you show row as you specified by layer - R.layout.custom_row_devices
inpput: ArrayList<BluetoothDevice> items
output: every single row of list on screen

 */
public class BasicAdapter extends ArrayAdapter<BluetoothDevice>{

    ArrayList<BluetoothDevice> items = null;

    BasicAdapter(Context context, ArrayList<BluetoothDevice> menuAdapter){
        super(context, R.layout.custom_row_devices , menuAdapter);
        this.items=menuAdapter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater listInflater = LayoutInflater.from(getContext());
        View customView = listInflater.inflate(R.layout.custom_row_devices, parent, false);

        BluetoothDevice item = items.get(position);
        TextView dataText = (TextView) customView.findViewById(R.id.listSqlDataRow);
        dataText.setText(item.getName() + "\n" + item.getAddress());

        return customView;
    }
}
