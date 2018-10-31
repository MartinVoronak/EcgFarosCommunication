package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

import static com.example.martin.bt_xiaomi.Constants.TAG_CONNECT;
import static com.example.martin.bt_xiaomi.Constants.MY_UUID;

//client thread to bind the communication channel
public class ConnectThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private CommunicationThread myCommChanel = null;
    private Handler handlerUIThread; // handler that gets info from Bluetooth service

    private static final String TAG = "BT_Accept_thread";

    public ConnectThread(BluetoothDevice device, Handler handler) {
        Log.i(TAG_CONNECT, "Client created");

        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;

        this.handlerUIThread = handler;
        boolean failed = false;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            Log.i(TAG_CONNECT, "Client createInsecureRfcommSocketToServiceRecord() with device");
        } catch (IOException e) {
            failed = true;
            Log.e(TAG, "Socket's createInsecureRfcommSocketToServiceRecord() method failed", e);
        }

        if (failed){
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.i(TAG_CONNECT, "Client createRfcommSocketToServiceRecord() with device");
            } catch (IOException e) {
                failed = true;
                Log.e(TAG, "Socket's createRfcommSocketToServiceRecord() method failed", e);
            }
        }

        Log.i(TAG_CONNECT, "Socket created");
        mmSocket = tmp;
    }

    public void run() {
        Log.i(TAG_CONNECT, "Client thread run() called");

        BluetoothAdapter btAdapter = null;
        try{
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter.cancelDiscovery()) {
                Log.d(TAG_CONNECT, "ConnectThread.run() cancelDiscovery OK");
            }
        } catch (Exception btExc) {
            Log.e(TAG_CONNECT, "ConnectThread.run() cancelDiscovery " + btExc.getMessage());
        }

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            if (btAdapter.isEnabled()){
                mmSocket.connect();
                Log.i(TAG_CONNECT, "Client connected to socket");
            }
            else {
                btAdapter.enable();
                Log.i(TAG_CONNECT, "BT was not enabled, retrying");
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
                Log.i(TAG_CONNECT, "Client socket closed");
            } catch (IOException closeException) {
                Log.i(TAG, "Could not close the client socket", closeException);
            }
        }

        // create communication thread
        myCommChanel = new CommunicationThread(mmSocket, handlerUIThread);
    }


    //use only when closing the app, otherwise channel will be closed
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public CommunicationThread getMyCommChanel(){
        return myCommChanel;
    }
}
