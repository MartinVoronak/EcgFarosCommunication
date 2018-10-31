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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.martin.bt_xiaomi.Constants.TAG_COMMUNICATION;
import static com.example.martin.bt_xiaomi.Constants.TAG_CONNECT;

public class CommunicateActivity extends AppCompatActivity {


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;

    private CommunicationThread communicationChannel;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private Handler handlerUIThread;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //stop threads after closing the app
        killServerClientThreads();
        closeSockets();

        if (communicationChannel != null) {
            communicationChannel.close();
            communicationChannel = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothDevice connectedDev = (BluetoothDevice) getIntent().getParcelableExtra("paired_device");
        Log.i(TAG_CONNECT, "device selected: "+connectedDev.getName()+" "+connectedDev.getAddress());

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

                if (communicationChannel != null) {
                    communicationChannel.write(msg);
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

        Button btnMeasure = (Button) findViewById(R.id.btnMeasure);
        btnMeasure.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (communicationChannel != null){
                    try {
                        communicationChannel.startMeasurement();
                    } catch (IOException e) {
                        Log.i(TAG_COMMUNICATION, "Unable to communicate with Faros device");
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Communication not running!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private synchronized void startServer(BluetoothAdapter adapter, Handler handler) {
        Log.i(TAG_CONNECT, "btnServer clicked");
        killServerClientThreads();

        acceptThread = new AcceptThread(adapter, handler);
        acceptThread.run();

        //wait till communication channel is created
        while (acceptThread.getMyCommChanel() == null) {
            android.os.SystemClock.sleep(100);
        }

        if (acceptThread.getMyCommChanel() != null) {
            //communication thread created
            communicationChannel = acceptThread.getMyCommChanel();
            communicationChannel.start();
            killServerClientThreads();
        }
    }

    private synchronized void startClient(BluetoothDevice connectedDev, Handler handler) {
        Log.i(TAG_CONNECT, "btnClient clicked");
        killServerClientThreads();

        connectThread = new ConnectThread(connectedDev, handler);
        connectThread.run();

        //wait till communication channel is created
        while (connectThread.getMyCommChanel() == null) {
            android.os.SystemClock.sleep(100);
        }

        if (connectThread.getMyCommChanel() != null) {
            //communication thread created
            communicationChannel = connectThread.getMyCommChanel();
            communicationChannel.start();
            killServerClientThreads();
        }
    }

    public void killServerClientThreads(){
        if (connectThread != null)
            connectThread = null;

        if (acceptThread != null)
            acceptThread = null;
    }

    public void closeSockets(){
        if (connectThread != null)
            connectThread.cancel();

        if (acceptThread != null)
            acceptThread.cancel();
    }

}