package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

//client thread to bind the communication channel
public class ConnectThread extends Thread {

    private static final String CONNECT_TAG = "BT_Connected";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private CommunicationThread myCommChanel = null;
    private Handler handlerUIThread; // handler that gets info from Bluetooth service

    private static final String TAG = "BT_Accept_thread";

    public ConnectThread(BluetoothDevice device, Handler handler) {
        this.handlerUIThread = handler;
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

        BluetoothAdapter btAdapter = null;
        try{
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter.cancelDiscovery()) {
                Log.d(CONNECT_TAG, "ConnectThread.run() cancelDiscovery OK");
            }
        } catch (Exception btExc) {
            Log.e(CONNECT_TAG, "ConnectThread.run() cancelDiscovery " + btExc.getMessage());
        }

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            if (btAdapter.isEnabled()){
                mmSocket.connect();
                Log.i(CONNECT_TAG, "Client connected to socket");
            }
            else {
                btAdapter.enable();
                Log.i(CONNECT_TAG, "BT was not enabled, retrying");
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
        myCommChanel = connectedCommunication(mmSocket);
        myCommChanel.start();
        Log.i(TAG, "Starting new communication thread");

    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }

        if (myCommChanel != null)
            myCommChanel.close();
    }

    public void sendMessage(String msg){
        myCommChanel.write(msg);
    }

    // If one succeeds with the socket connection, now time to manage connection for sending and receiving files in a separate thread.
    public synchronized CommunicationThread connectedCommunication(BluetoothSocket socket) {
        Log.i(CONNECT_TAG, "Establishing connection in new thread");

        // Start the thread to manage the connection and perform transmissions
        CommunicationThread mCommunicationThread = new CommunicationThread(socket, handlerUIThread);
        return mCommunicationThread;
    }
}
