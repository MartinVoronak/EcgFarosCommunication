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

public class ConnectedActivity extends AppCompatActivity {

    //https://www.programcreek.com/java-api-examples/?code=hardik-dadhich/bluetooth-chat-appliction/bluetooth-chat-appliction-master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatService.java#
    //https://developer.android.com/guide/topics/connectivity/bluetooth

    //private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter;
    private static final String CONNECT_TAG = "BT_Connected";

    //todo send after connection ?
    public static final String MSG_DRV_WBA_CONNECTED = "coonected_to_wba";
    public static final String DRV_WBA_MESSAGE = "DRV_WBA_MESSAGE";

    AcceptThread acceptThread;
    ConnectThread connectThread;

    Handler handlerUIThread;

    EditText msg;
    TextView runAs;

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (acceptThread!=null){
            acceptThread.cancel();
        }
        if (connectThread!=null){
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

        runAs = (TextView) findViewById(R.id.runAs);

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

                if (acceptThread != null) {
                    acceptThread.sendMessage(msg);
                } else if (connectThread != null) {
                    connectThread.sendMessage(msg);
                } else {
                    Toast.makeText(getApplicationContext(), "Thread not running!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button btnServer = (Button) findViewById(R.id.btnServer);
        btnServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startServer();
            }
        });

        Button btnClient = (Button) findViewById(R.id.btnClient);
        btnClient.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startClient(connectedDev);
            }
        });
    }

    //should be synchronized??
    public synchronized void startServer(){
        Log.i(CONNECT_TAG, "btnServer clicked");

        if (connectThread!=null)
            connectThread.cancel();

        acceptThread = new AcceptThread();
        acceptThread.run();

        runAs.setText("Running as Server");
    }

    public synchronized void startClient(BluetoothDevice connectedDev){
        Log.i(CONNECT_TAG, "btnClient clicked");

        if (acceptThread!=null)
            acceptThread.cancel();

        connectThread = new ConnectThread(connectedDev);
        connectThread.run();

        runAs.setText("Running as Client");
    }


    //-----------------------------------------------------------------------------------------------------------------//

    //server
    public class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private final String NAME = "myPrototype";
        private static final String TAG = "BT_Accept_thread";

        private ConnectedThread myServerChannel = null;

        public AcceptThread() {
            Log.i(CONNECT_TAG, "Server created");
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("PWAccessP", MY_UUID);
                Log.i(CONNECT_TAG, "Server initializing communication with paired device");
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {

            Log.i(CONNECT_TAG, "Starting run thread");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    Log.i(CONNECT_TAG, "Socket created");
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {

                    // Situation normal. Start the connected thread.
                    myServerChannel = connectedCommunication(socket);
                    myServerChannel.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void sendMessage(String msg){
            myServerChannel.write(msg);
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                Log.i(CONNECT_TAG, "Server closing socket!");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------//

    //client thread
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private ConnectedThread myClientChannel = null;

        private static final String TAG = "BT_Accept_thread";

        public ConnectThread(BluetoothDevice device) {
            Log.i(CONNECT_TAG, "Client created");
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            boolean failed = false;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                Log.i(CONNECT_TAG, "Client createInsecureRfcommSocketToServiceRecord() with device");
            } catch (IOException e) {
                failed = true;
                Log.e(TAG, "Socket's createInsecureRfcommSocketToServiceRecord() method failed", e);
            }

            if (failed){
                try {
                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                    // MY_UUID is the app's UUID string, also used in the server code.
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                    Log.i(CONNECT_TAG, "Client createRfcommSocketToServiceRecord() with device");
                } catch (IOException e) {
                    failed = true;
                    Log.e(TAG, "Socket's createRfcommSocketToServiceRecord() method failed", e);
                }
            }

            Log.i(CONNECT_TAG, "Socket created");
            mmSocket = tmp;
        }

        public void run() {
            Log.i(CONNECT_TAG, "Client thread run() called");
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();
            //  Log.i(CONNECT_TAG, "IF discovery was running - closing");

            BluetoothAdapter btAdapter = null;
            try{
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                if (btAdapter.cancelDiscovery()) {
                    Log.d(CONNECT_TAG, "ConnectThread.run() cancelDiscovery OK");
                }
            } catch (Exception btExc) {
                Log.e(CONNECT_TAG, "ConnectThread.run() cancelDiscovery " + btExc.getMessage());
            }

//            Message connMsg = Message.obtain();
//            Bundle data = new Bundle();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                if (btAdapter.isEnabled()){
                    mmSocket.connect();
                    Log.i(CONNECT_TAG, "Client connected to socket");
//                    data.putString(DRV_WBA_MESSAGE, MSG_DRV_WBA_CONNECTED);
//                    connMsg.setData(data);
                }
                else {
                    Log.i(CONNECT_TAG, "BT not enabled, socket wont connect, retrying or method failed");
                    mBluetoothAdapter.enable();
                }
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.i(CONNECT_TAG, "Client socket closed");
                } catch (IOException closeException) {
                    Log.i(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //todo create better handler
            myClientChannel = connectedCommunication(mmSocket);
            myClientChannel.start();
            Log.i(TAG, "Starting new communication thread");

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }

        public void sendMessage(String msg){
            myClientChannel.write(msg);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------//

    // If one succeeds with the socket connection, now time to manage connection for sending and receiving files in a separate thread.
    public synchronized ConnectedThread connectedCommunication(BluetoothSocket socket) {
        Log.i(CONNECT_TAG, "Establishing connection in new thread");

        // Start the thread to manage the connection and perform transmissions
        ConnectedThread mConnectedThread = new ConnectedThread(socket);
        return  mConnectedThread;
    }


    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket = null;
        private InputStream mmInStream = null;
        private OutputStream mmOutStream = null;
        private byte[] mmBuffer; // mmBuffer store for the stream
        //private Handler mHandler; // handler that gets info from Bluetooth service

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            //Message connMsg = Message.obtain();

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.i(CONNECT_TAG, "socket accepted in Create thread");
            } catch (IOException e) {
                Log.i(CONNECT_TAG, "Could not create input/output streams ",e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;


            Log.i(CONNECT_TAG, "Channel created");
        }


        public void run() {
            Log.i(CONNECT_TAG, "Method run called");

            //todo try sending this after connection
            /*
            Message connMsg = Message.obtain();
            Bundle data = new Bundle();
            data.putString(MSG_DRV_MGH_MESSAGE, MSG_DRV_MGH_MESSAGE_CONNECTED);
            connMsg.setData(data);
            */

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()
                // receiving message
                try {
                    // Read from the InputStream
                    numBytes = mmInStream.read(mmBuffer);
                    Log.i(CONNECT_TAG, "received bytes: "+numBytes);
                    Log.i(CONNECT_TAG, "mmBuffer: "+mmBuffer);


                    //String converted = new String(mmBuffer) + "\0";
                    String converted = new String(mmBuffer);
                    Log.i(CONNECT_TAG, "msg converted: "+converted);
                    Message msg2 = handlerUIThread.obtainMessage(Constants.MESSAGE_READ, 2, 1, (Object)converted);
                    msg2.sendToTarget();

                } catch (IOException e) {
                    // connection was lost and start your connection again
                    Log.i(CONNECT_TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void write(String message) {

            byte[] buffer = message.getBytes();

            try {
                mmOutStream.write(buffer);
                //todo for example notify UI thread

                Log.i(CONNECT_TAG, "sending message: "+message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}