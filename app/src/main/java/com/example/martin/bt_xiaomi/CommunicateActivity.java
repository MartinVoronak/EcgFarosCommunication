package com.example.martin.bt_xiaomi;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CommunicateActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter;
    private static final String CONNECT_TAG = "BT_Connected";

    AcceptThread acceptThread;
    ConnectThread connectThread;
    Handler handlerUIThread;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (acceptThread!=null){
            acceptThread.cancel();
        }

        //stop thread after closing the app
        if (connectThread != null) {
            connectThread.cancel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothDevice connectedDev = (BluetoothDevice) getIntent().getParcelableExtra("paired_device");
        Log.i(CONNECT_TAG, "device selected: "+connectedDev.getName()+" "+connectedDev.getAddress());

        handlerUIThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                // This is where you do your work in the UI thread.
                // Your worker tells you in the message what to do.
                //todo read message text
                if (message.arg1 == 1)
                    Toast.makeText(getApplicationContext(), "Message accepted!", Toast.LENGTH_SHORT).show();
                else if (message.arg1 == 2)
                    Toast.makeText(getApplicationContext(), (String)message.obj, Toast.LENGTH_SHORT).show();
            }
        };

        final EditText msgEdit = (EditText) findViewById(R.id.msgEdit);
        Button btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = msgEdit.getText().toString();

                if (connectThread != null) {
                    connectThread.sendMessage(msg);
                } else if (acceptThread != null) {
                    acceptThread.sendMessage(msg);
                } else {
                    Toast.makeText(getApplicationContext(), "Thread not running!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button btnClient = (Button) findViewById(R.id.btnClient);
        btnClient.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startClient(connectedDev, handlerUIThread);
            }
        });

        Button btnServer = (Button) findViewById(R.id.btnServer);
        btnServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startServer(mBluetoothAdapter, handlerUIThread);
            }
        });
    }

    public synchronized void startServer(BluetoothAdapter adapter, Handler handler){
        Log.i(CONNECT_TAG, "btnServer clicked");

        if (connectThread!=null)
            connectThread.cancel();

        acceptThread = new AcceptThread(adapter, handler);
        acceptThread.run();
    }

    public synchronized void startClient(BluetoothDevice connectedDev, Handler handler) {
        Log.i(CONNECT_TAG, "btnClient clicked");

        if (connectThread != null)
            connectThread.cancel();

        connectThread = new ConnectThread(connectedDev, handler);
        connectThread.run();
    }

}