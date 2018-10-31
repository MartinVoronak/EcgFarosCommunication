package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import static com.example.martin.bt_xiaomi.Constants.CONNECT_TAG;
import static com.example.martin.bt_xiaomi.Constants.MY_UUID;

//server
public class AcceptThread extends Thread {

    private final BluetoothServerSocket mmServerSocket;
    private final String NAME = "myPrototype";
    private static final String TAG = "BT_Accept_thread";
    private Handler handlerUIThread;

    private CommunicationThread myCommChanel = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    public AcceptThread(BluetoothAdapter adapter, Handler handler) {
        this.handlerUIThread = handler;
        this.mBluetoothAdapter = adapter;

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

                // create communication thread
                myCommChanel = new CommunicationThread(socket, handlerUIThread);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
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

    public CommunicationThread getMyCommChanel(){
        return myCommChanel;
    }
}
