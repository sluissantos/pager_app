package com.logpyx.auraconfig;

import java.util.UUID;

public class SampleGattAttributes {
    //parte que o fabiano comentou dos códigos lá tem na API
    public static final UUID SERVICE_UUID=UUID.fromString("ffc2dedc-bf02-442e-a69d-b5caff5e9b21");
    public static final UUID CHARACTERISTIC_UUID=UUID.fromString("fbdca8dd-1b1d-4a4c-bc07-040e80c1cb63");
    public static final UUID BLE_NOTIFICATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // Nova API - Contendo um serviço uart com 1 caracteristica Tx e RX.
    public static final UUID SERVICEUART_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTICRX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTICTX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // Algumas constantes (Por comodidade foram adicionadas aqui)
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
