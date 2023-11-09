package com.logpyx.auraconfig;

import android.media.audiofx.AudioEffect;

import java.util.UUID;

public class SampleGattAttributes {
    public static final UUID OTA_SERVICE_UUID =UUID.fromString("8a97f7c0-8506-11e3-baa7-0800200c9a66");
    public static final UUID IMAGE_CHARACTERISTIC_UUID =UUID.fromString("122e8cc0-8508-11e3-baa7-0800200c9a66");
    public static final UUID NEW_IMAGE_CHARACTERISTIC_UUID = UUID.fromString("210f99f0-8508-11e3-baa7-0800200c9a66");
    public static final UUID NEW_IMAGE_TRANSFER_UNIT_CONTENT_CHARACTERISTIC_UUID = UUID.fromString("2691aa80-8508-11e3-baa7-0800200c9a66");
    public static final UUID NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID = UUID.fromString("2bdc5760-8508-11e3-baa7-0800200c9a66");
    public static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final int BYTE_STX = 0x02;
    public static final int BYTE_RTX = 0x03;
    public static final int FINDING_NEW_PACKAGE = 0x00;
    public static final int READING_NEW_PACKAGE = 0x01;
    public static final int READING_OPCODE_DATA = 0x02;
    public static final int READING_CHECKSUM = 0x03;
    public static final int VALIDATING_PACKAGE = 0x04;

    // Lista de Commandos implementados no firmware do aura
    public static final int RD_REQUEST_CONNECTION = 0xD0;
    public static final int RD_ALL_INFO_DEVICE = 0xD1;
    public static final int WR_TAG_ID_DISTANCE = 0xD2;
    public static final int WR_PERIOD_REQUEST = 0xD5;
    public static final int WR_PERIPHERAL_OFFSET = 0xD6;

    public static final int WR_OTA_START = 0xB1;
    public static final int WR_URL_HTTPS_SERVER_OTA = 0xD9;
    public static final int WR_DFU_START = 0xB0;
    public static final int WR_SET_OVERCRANE_PARAM = 0xF2;
    public static final int WR_GET_OVERCRANE_PARAM = 0xF3;
    public static final int WR_SET_OVERCRANE_MODE = 0xF1;
    public static final int WR_SET_FIX_TAG = 0xF0;
    public static final int VERSION_REQUEST = 0xDB;
    public static final int WR_DECA_ID_LIST = 0xD7;
    public static final int WR_ZONE_NEAR_DISTANCE = 0xD4;
    public static final int WR_ZONE_FAR_DISTANCE = 0xD3;
    public static final int WR_ID_CELULAR = 0xDE;
    public static final int RD_REQUEST_DISCONNECTION = 0xD8;


}
