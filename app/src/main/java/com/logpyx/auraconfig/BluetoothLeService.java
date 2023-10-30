package com.logpyx.auraconfig;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Semaphore;

public class BluetoothLeService extends Service implements SendInterface{
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic characteristicTx;
    private BluetoothGattCharacteristic characteristicImage;
    private BluetoothGattCharacteristic characteristicNewImage;
    private BluetoothGattCharacteristic characteristicNewImageTransferUnit;
    private BluetoothGattCharacteristic characteristicNewImageExpectedTransferUnit;
    private SampleGattAttributes sampleGattAttributes = new SampleGattAttributes();
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public boolean flag_finished = false;

    ReceiveInterface receiveInterface;

    int freeSpaceStart;
    int freeSpaceEnd;

    int freeSpaceAll;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange: bluetoothgattcallback");
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "onConnectionStateChange: requesting higher mtu " + gatt.requestMtu(517));

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.i(TAG, "onMtuChanged: new mtu " + mtu);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onMtuChanged: discovering services " + gatt.discoverServices());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "Service discovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered: ---------------------");
                for (BluetoothGattService gattService : gatt.getServices()) {
                    if (gattService.getUuid().toString().equals(SampleGattAttributes.OTA_SERVICE_UUID.toString())) {
                        Log.i(TAG, "onServicesDiscovered: service=" + gattService.getUuid());
                        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.IMAGE_CHARACTERISTIC_UUID.toString())) {
                                Log.i(TAG, "onServicesDiscovered: found Characteristic IMAGE_CHARACTERISTIC_UUID");
                                characteristicImage = characteristic;
                            }
                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_CHARACTERISTIC_UUID.toString())) {
                                Log.i(TAG, "onServicesDiscovered: found Characteristic NEW_IMAGE_CHARACTERISTIC_UUID");
                                characteristicNewImage = characteristic;
                            }
                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_TRANSFER_UNIT_CONTENT_CHARACTERISTIC_UUID.toString())) {
                                Log.i(TAG, "onServicesDiscovered: found Characteristic NEW_IMAGE_TRANSFER_UNIT_CONTENT_CHARACTERISTIC_UUID");
                                characteristicNewImageTransferUnit = characteristic;
                            }
                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID.toString())) {
                                Log.i(TAG, "onServicesDiscovered: found Characteristic NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID");
                                characteristicNewImageExpectedTransferUnit = characteristic;
                                setCharacteristicNotification(characteristic, true);
                            }
                        }
                    }
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                if (characteristic.getUuid().toString().equals(SampleGattAttributes.IMAGE_CHARACTERISTIC_UUID.toString())) {
                    byte[] response = characteristic.getValue();
                    // Use ByteBuffer para ler os inteiros de 4 bytes da matriz de bytes
                    ByteBuffer buffer = ByteBuffer.wrap(response);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    freeSpaceStart = buffer.getInt();
                    freeSpaceEnd = buffer.getInt();
                    freeSpaceAll = freeSpaceEnd - freeSpaceStart;
                    Log.i(TAG, "Start: 0x" + Integer.toHexString(freeSpaceStart));
                    Log.i(TAG, "End: 0x" + Integer.toHexString(freeSpaceEnd));
                    Log.i(TAG, "Difference in bytes: " + freeSpaceAll);
                    readFreeSpaceInfo(1);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.i(TAG, "characteristic = " + characteristic.getUuid().toString());
            byte[] value = characteristic.getValue();
            for (int k = 0; k < value.length; k++){
                if (value[k] < 0) {
                    value[k] = (byte) (value[k] + 256);
                }
                receiveInterface.extractMessage(value[k]);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (SampleGattAttributes.NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            final String value = new String(characteristic.getValue());
            Log.d(TAG, String.format("Received new value: " + new String(value)));
            intent.putExtra(EXTRA_DATA, String.valueOf(value));
        }
        sendBroadcast(intent);
    }

    @Override
    public int sendData(byte[] data) {
        return send(data);
    }

    @Override
    public void setReceiveInterface(ReceiveInterface receiveInterface) { this.receiveInterface = receiveInterface; }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d("desconectado ", "true");
        mBluetoothGatt.disconnect();
    }
    @SuppressLint("MissingPermission")
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to disconnect");
            return;
        }
        mBluetoothGatt.disconnect();
    }
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        flag_finished = true;
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.i(TAG, "characteristic = " + characteristic.getUuid().toString());
        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.i(TAG, "Descriptor: " + descriptor.getUuid().toString() + "-Notificação Ativada");
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /*
    Função: send(byte[] data)
    Descrição: Envia um array de byts para caracteristicaRX.
    @param: byte[] data -> Array de bytes

    @return:    int length -> Quantidade de dados enviados
    */
    public int send(byte[] data) {
        synchronized (this) {
            this.characteristicImage.setValue(data);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if(this.characteristicImage !=null){
                    Log.i(TAG, "send: Rx not null");
                    mBluetoothGatt.writeCharacteristic(this.characteristicImage);
                }else{
                    Log.i(TAG, "send: Rx null");
                }
            }
        }
        return data.length;
    }

    public void readFreeSpaceInfo(int state) {
        switch (state){
            case 0:
                readCharacteristic(characteristicImage);
                break;

            case 1:

        }
    }
}





