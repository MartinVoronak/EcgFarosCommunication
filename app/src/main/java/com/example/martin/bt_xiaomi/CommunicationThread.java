package com.example.martin.bt_xiaomi;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.martin.bt_xiaomi.Constants.NONIN_COMMAND_START_DATAFORMAT_13;
import static com.example.martin.bt_xiaomi.Constants.TAG_COMMUNICATION;
import static com.example.martin.bt_xiaomi.Constants.WBA_COMMAND_SR_250Hz;
import static com.example.martin.bt_xiaomi.Constants.WBA_COMMAND_START;
import static com.example.martin.bt_xiaomi.Constants.WBA_COMMAND_START_V6_ACK;
import static com.example.martin.bt_xiaomi.Constants.WBA_MSG_VALUE_READ_TIMEOUT;
import static com.example.martin.bt_xiaomi.Constants.WRITE_CHAR_TO_STREAM_DELAY_MS;

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

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            mmSocket = socket;
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.i(TAG_COMMUNICATION, "Could not create input/output streams ",e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        Log.i(TAG_COMMUNICATION, "Channel created");
    }


    public void run() {
        Log.i(TAG_COMMUNICATION, "Method run called");

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            //todo maybe remove this reading
            // and adapt to the mangoko message read

            mmBuffer = new byte[1024];
            int numBytes;

            // receiving message
            try {
                // Read from the InputStream
                numBytes = mmInStream.read(mmBuffer);
                Log.i(TAG_COMMUNICATION, "received bytes: "+numBytes);

                //String converted = new String(mmBuffer) + "\0";
                String converted = new String(mmBuffer);
                Log.i(TAG_COMMUNICATION, "msg converted: "+converted);
                Message msg2 = handlerUIThread.obtainMessage(Constants.MESSAGE_READ, 2, 1, (Object)converted);
                msg2.sendToTarget();

            } catch (IOException e) {
                // connection was lost and start your connection again
                Log.i(TAG_COMMUNICATION, "Input stream was disconnected", e);
                break;
            }
        }
    }

    public void write(String message) {

        byte[] buffer = message.getBytes();
        try {
            mmOutStream.write(buffer);
            //todo for example notify UI thread

            Log.i(TAG_COMMUNICATION, "sending message: "+message);
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

    public void startMeasurement() throws IOException {
        writeSamplingRateCommand();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }

        String startCommand = WBA_COMMAND_START;
        String expextedResponse = WBA_COMMAND_START_V6_ACK;
        writeStartCommand(startCommand);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e2) {
        }

        Log.d(TAG_COMMUNICATION, "Reading answer...");
        readStartMeasureResponse(expextedResponse);
    }

    private void writeSamplingRateCommand() {
        //todo for now we use all default values of variables

        String strCommand = WBA_COMMAND_SR_250Hz;
        byte[] command = strCommand.getBytes();
        Log.i(TAG_COMMUNICATION, "Command length: " + command.length);

        for (int i = 0; i < command.length; i += WBA_MSG_VALUE_READ_TIMEOUT) {
            Log.i(TAG_COMMUNICATION, "Command chars: " + command[i]);

            try {
                mmOutStream.write(command[i]);
            } catch (IOException e) {
                Log.i(TAG_COMMUNICATION, "osCommand not successful: ", e);
                e.printStackTrace();
            }
            try {
                //todo WHY DO WE NEED TO SLEEP THREAD???
                Thread.sleep(WRITE_CHAR_TO_STREAM_DELAY_MS);
            } catch (InterruptedException e) {
                Log.i(TAG_COMMUNICATION, "Thread sleep osCommand not successful: ", e);
                e.printStackTrace();
            }
        }
    }

    private void writeStartCommand(String a_startCommand) throws IOException {
        byte[] command = a_startCommand.getBytes();
        Log.i(TAG_COMMUNICATION, "Command length: " + command.length);
        Log.i(TAG_COMMUNICATION, "Sending Start-command...");

        for (int i = 0; i < command.length; i += WBA_MSG_VALUE_READ_TIMEOUT) {
            Log.i(TAG_COMMUNICATION, "Command chars: " + command[i]);
            this.mmOutStream.write(command[i]);
            try {
                Thread.sleep(WRITE_CHAR_TO_STREAM_DELAY_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void readStartMeasureResponse(String expextedResponse) {
        byte[] answerByteData = new byte[32];
        int bytesRead = 0;
        try {
            bytesRead = this.mmInStream.read(answerByteData);
            Log.i(TAG_COMMUNICATION, "Bytes read: " + bytesRead);
        } catch (IOException e) {
            Log.i(TAG_COMMUNICATION, "Cannot read Start measurment data from ECG device");
            e.printStackTrace();
        }
        String answerFromWBA = "";

        for (int j = 0; j < bytesRead; j += WBA_MSG_VALUE_READ_TIMEOUT) {
            if (j < answerByteData.length) {
                if (answerByteData[j] != NONIN_COMMAND_START_DATAFORMAT_13) {
                    answerFromWBA = new StringBuilder(String.valueOf(answerFromWBA)).append((char) answerByteData[j]).toString();
                } else {
                    answerFromWBA = new StringBuilder(String.valueOf(answerFromWBA)).append("\\r").toString();
                }
            }
        }
        Log.d(TAG_COMMUNICATION, "Answer: " + answerFromWBA);
        //todo change, for now just show toast we have some message from device
        Message msg2 = handlerUIThread.obtainMessage(Constants.MESSAGE_READ, 2, 1, (Object) answerFromWBA);
        msg2.sendToTarget();

        if (answerFromWBA.contains(expextedResponse) || answerFromWBA.contains("w")) {
            Log.i(TAG_COMMUNICATION, "Answer OK, we can start listening for data");
        }
    }
}
