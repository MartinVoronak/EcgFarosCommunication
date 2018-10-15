package com.example.martin.bt_xiaomi;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayList<BluetoothDevice> myBtDevices = new ArrayList<>();
    ArrayList<BluetoothDevice> discoverdBleDevices = new ArrayList<>();

    ArrayAdapter<BluetoothDevice> discoveredAdapter;
    ArrayAdapter<BluetoothDevice> pairedAdapter;
    ListView listViewDiscovered;
    ListView listViewPaired;
    Button buttonScan, buttonRefresh;
    ProgressDialog progressScanDevices;
    ProgressDialog progressPairDevice;

    BluetoothDevice myBtDevice;
    private final String TAG = "BT_device";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
        showPaired();

        //todo listener for buttons (switch)
        //scan devices
        buttonScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

                //start receive bluetooth devices
                registerReceiver(mReceiver, filter);
                bluetoothAdapter.startDiscovery();
            }
        });

        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPaired();
            }
        });
    }

    private void setup(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //adapter for already paired devices
        pairedAdapter = new BasicAdapter(this, myBtDevices);
        listViewPaired = (ListView)findViewById(R.id.listview_devices);
        listViewPaired.setAdapter(pairedAdapter);

        listViewPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                BluetoothDevice selected = (BluetoothDevice) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), ConnectedActivity.class);
                intent.putExtra("paired_device", selected);
                startActivity(intent);
            }
        });

        //adapter for found devices
        discoveredAdapter = new BasicAdapter(this, discoverdBleDevices);
        listViewDiscovered = (ListView)findViewById(R.id.listview_scanned);
        listViewDiscovered.setAdapter(discoveredAdapter);

        listViewDiscovered.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pairDevice((BluetoothDevice) parent.getItemAtPosition(position));
                discoveredAdapter.remove(discoveredAdapter.getItem(position));
            }
        });

        //button for scan
        buttonScan = findViewById(R.id.scan);
        buttonRefresh = findViewById(R.id.refresh);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    private void showPaired(){
        //reset list
        myBtDevices.clear();
        pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                myBtDevices.add(device);
                Log.i(TAG, "bt info: " + device.getName() + "  add: " + device.getAddress() + "  state: " + device.getBondState());
            }
        }

        pairedAdapter.notifyDataSetChanged();
    }


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                //reset list of found unpaired devices
                discoveredAdapter.clear();
                discoveredAdapter.notifyDataSetChanged();
                //discovery starts, we can show progress dialog or perform other tasks
                progressScanDevices = new ProgressDialog(context);
                progressScanDevices.setCanceledOnTouchOutside(false);
                progressScanDevices.setMessage("finding devices nearby");
                progressScanDevices.show();

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
                discoveredAdapter.notifyDataSetChanged();
                progressScanDevices.dismiss();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "FOUND BT DEVICE AROUND: "+device.getName());
                discoverdBleDevices.add(device);
                discoveredAdapter.notifyDataSetChanged();
            }
        }
    };

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            showPaired();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
