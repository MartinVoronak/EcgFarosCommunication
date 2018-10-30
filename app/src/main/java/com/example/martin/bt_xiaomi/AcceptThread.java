package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

//server
public class AcceptThread extends Thread {

    private static final String CONNECT_TAG = "BT_Connected";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
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

                // Situation normal. Start the connected thread.
                myCommChanel = connectedCommunication(socket);
                myCommChanel.start();
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void sendMessage(String msg) {
        myCommChanel.write(msg);
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
            Log.i(CONNECT_TAG, "Server closing socket!");
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }

        if (myCommChanel != null)
            myCommChanel.close();
    }

    // If one succeeds with the socket connection, now time to manage connection for sending and receiving files in a separate thread.
    public synchronized CommunicationThread connectedCommunication(BluetoothSocket socket) {
        Log.i(CONNECT_TAG, "Establishing connection in new thread");

        // Start the thread to manage the connection and perform transmissions
        CommunicationThread mCommunicationThread = new CommunicationThread(socket, handlerUIThread);
        return mCommunicationThread;
    }
}
