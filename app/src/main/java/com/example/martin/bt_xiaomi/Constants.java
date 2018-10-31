package com.example.martin.bt_xiaomi;

import java.util.UUID;

/*
todo: add methods for better handling of messages
 */
public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    static final int MESSAGE_READ = 1;
    static final int MESSAGE_WRITE = 2;

    static final int THREAD_NAME_SERVER = 3;
    static final int THREAD_NAME_CLIENT = 4;

    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static final String CONNECT_TAG = "BT_Connected";
    static final String COMMUNICATION_TAG = "BT_Communication";

    //variables for WBA communications
    static final long WRITE_CHAR_TO_STREAM_DELAY_MS = 40;
    static final int WBA_MSG_VALUE_READ_TIMEOUT = 1;

    static final String WBA_COMMAND_SR_250Hz = "wbafs4\r";
    static final String WBA_COMMAND_START = "wbaom3\r";
    static final String WBA_COMMAND_START_WITH_POWERSAVE = "wbaom4\r";
    static final String WBA_COMMAND_START_V5_ACK = "wbav05\r";
    static final String WBA_COMMAND_START_V6_ACK = "wbav06\r";

}
