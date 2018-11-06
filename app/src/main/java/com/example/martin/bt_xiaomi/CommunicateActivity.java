package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.example.martin.bt_xiaomi.Constants.TAG_COMMUNICATION;
import static com.example.martin.bt_xiaomi.Constants.TAG_CONNECT;

public class CommunicateActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    private CommunicationThread communicationChannel;
    private AcceptThread serverThread;
    private ConnectThread clientThread;
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

        // This is where you do your work in the UI thread.
        // Your worker tells you in the message what to do.
        handlerUIThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.arg1 == 1)
                    Toast.makeText(getApplicationContext(), "Message accepted!", Toast.LENGTH_SHORT).show();
                else if (message.arg1 == 2)
                    Toast.makeText(getApplicationContext(), (String)message.obj, Toast.LENGTH_SHORT).show();
            }
        };

        //todo delete cusom message send, no need for that
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

        //start measure
        Button btnMeasure = (Button) findViewById(R.id.btnMeasure);
        btnMeasure.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (communicationChannel != null){
                    try {
                        communicationChannel.startMeasurement();

                        // todo maybe we will need to create a small delay for reading after measure command
                        communicationChannel.setRead(true);
                    } catch (IOException e) {
                        Log.i(TAG_COMMUNICATION, "Unable to communicate with Faros device");
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Communication not running!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //stop measure
        Button btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (communicationChannel != null){
                    communicationChannel.setRead(false);
                    // todo send stop measurment command for device
                } else {
                    Toast.makeText(getApplicationContext(), "Communication not running!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //get what version is used
        Button btnVersion = (Button) findViewById(R.id.btnVersion);
        btnVersion.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (communicationChannel != null){
                    try {
                        communicationChannel.getFwVerson();
                    } catch (IOException e) {
                        Log.i(TAG_COMMUNICATION, "Unable to get version from Faros device");
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
        //restart if running
        killServerClientThreads();

        serverThread = new AcceptThread(adapter, handler);
        serverThread.run();

        //wait till communication channel is created
        while (serverThread.getMyCommChanel() == null) {
            android.os.SystemClock.sleep(100);
        }

        if (serverThread.getMyCommChanel() != null) {
            //communication thread created
            communicationChannel = serverThread.getMyCommChanel();
            communicationChannel.setRead(true);
            communicationChannel.start();
            killServerClientThreads();
        }
    }

    private synchronized void startClient(BluetoothDevice connectedDev, Handler handler) {
        Log.i(TAG_CONNECT, "btnClient clicked");
        //restart if running
        killServerClientThreads();

        clientThread = new ConnectThread(connectedDev, handler);
        clientThread.run();

        //wait till communication channel is created
        while (clientThread.getMyCommChanel() == null) {
            android.os.SystemClock.sleep(100);
        }

        if (clientThread.getMyCommChanel() != null) {
            //communication thread created
            communicationChannel = clientThread.getMyCommChanel();
            communicationChannel.start();
            killServerClientThreads();
        }
    }

    //close threads so it wont drain our cpu/battery
    public void killServerClientThreads(){
        if (clientThread != null)
            clientThread = null;

        if (serverThread != null)
            serverThread = null;
    }

    //end communication
    public void closeSockets(){
        if (clientThread != null)
            clientThread.cancel();

        if (serverThread != null)
            serverThread.cancel();
    }

}