package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.martin.bt_xiaomi.Constants.COMMUNICATION_TAG;

/*
* Thread after successful channel creation for communication
* handler notifies UI of incoming messages
* */
public class CommunicationThread extends Thread {

    private BluetoothSocket mmSocket = null;
    private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;
    private byte[] mmBuffer; // mmBuffer store for the stream

    private Handler handlerUIThread; // handler that gets info from Bluetooth service

    public CommunicationThread(BluetoothSocket socket, Handler handler) {
        this.handlerUIThread = handler;

        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.i(COMMUNICATION_TAG, "Could not create input/output streams ",e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        Log.i(COMMUNICATION_TAG, "Channel created");
    }


    public void run() {
        Log.i(COMMUNICATION_TAG, "Method run called");

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            mmBuffer = new byte[1024];
            int numBytes;

            // receiving message
            try {
                // Read from the InputStream
                numBytes = mmInStream.read(mmBuffer);
                Log.i(COMMUNICATION_TAG, "received bytes: "+numBytes);
                Log.i(COMMUNICATION_TAG, "mmBuffer: "+mmBuffer);


                //String converted = new String(mmBuffer) + "\0";
                String converted = new String(mmBuffer);
                Log.i(COMMUNICATION_TAG, "msg converted: "+converted);
                Message msg2 = handlerUIThread.obtainMessage(Constants.MESSAGE_READ, 2, 1, (Object)converted);
                msg2.sendToTarget();

            } catch (IOException e) {
                // connection was lost and start your connection again
                Log.i(COMMUNICATION_TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    public void write(String message) {

        byte[] buffer = message.getBytes();
        try {
            mmOutStream.write(buffer);
            //todo for example notify UI thread

            Log.i(COMMUNICATION_TAG, "sending message: "+message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            mmInStream.close();
            mmOutStream.close();
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
