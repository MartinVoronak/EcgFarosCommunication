package com.example.martin.bt_xiaomi;

import java.util.UUID;

public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_READ = 1;
    int MESSAGE_WRITE = 2;

    int THREAD_NAME_SERVER = 3;
    int THREAD_NAME_CLIENT = 4;

    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String TAG_CONNECT = "BT_Connected";
    String TAG_COMMUNICATION = "BT_Communication";

    //variables for WBA communications
    long WRITE_CHAR_TO_STREAM_DELAY_MS = 40;
    int WBA_MSG_VALUE_READ_TIMEOUT = 1;
    int WBA_V5_PACKET_HEADER_SIZE = 9;
    byte NONIN_COMMAND_START_DATAFORMAT_13 = (byte) 13;

    String AW_PROTOCOL_CODE_V_0_5 = "05";
    String AW_PROTOCOL_CODE_V_0_6 = "06";
    String AW_PROTOCOL_CODE_V_0_8 = "08";


    String WBA_COMMAND_FW_VERSION = "wbainf\r";
    String WBA_COMMAND_SR_250Hz = "wbafs4\r";
    String WBA_COMMAND_START = "wbaom3\r";

    String WBA_COMMAND_START_WITH_POWERSAVE = "wbaom4\r";
    String WBA_COMMAND_START_V5_ACK = "wbav05\r";
    String WBA_COMMAND_START_V6_ACK = "wbav06\r";

}
