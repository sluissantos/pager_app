package com.logpyx.auraconfig;

import android.bluetooth.BluetoothGattCharacteristic;

public interface SendInterface {

    public int sendData(byte[] data, BluetoothGattCharacteristic characteristic);
    
    public void setReceiveInterface(ReceiveInterface receiveInterface);

}
