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
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BluetoothLeService extends Service implements SendInterface{
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
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

    ReceiveInterface receiveInterface;

    private static int freeSpaceStart;
    private static int freeSpaceEnd;
    private static long minAddr = 0;
    private static long maxAddr = 0;
    private static int notificationInterval = 8;
    public static boolean uploadInProgress = false;
    public static int lastSequence = 0;
    public static IntelHex firmware;
    Queue<byte[]> imageDataQueue = new LinkedList<>();
    public DeviceControlActivity deviceControlActivity;
    public void setDeviceControlActivity(DeviceControlActivity activity) {
        this.deviceControlActivity = activity;
    }

    public void updateProgressBar(int progress, int error) {
        if (deviceControlActivity != null) {
            deviceControlActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceControlActivity.updateProgressBar(progress, error);
                }
            });
        }
    }

    public enum OtaError {
        OTA_SUCCESS(0),
        OTA_FLASH_VERIFY_ERROR(0x3c),
        OTA_FLASH_WRITE_ERROR(0xff),
        OTA_SEQUENCE_ERROR(0xf0),
        OTA_CHECKSUM_ERROR(0x0f);

        private final int value;

        OtaError(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OtaError valueOf(int value) {
            for (OtaError error : values()) {
                if (error.value == value) {
                    return error;
                }
            }
            throw new IllegalArgumentException("Invalid OtaError value: " + value);
        }
    }

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
                Log.i(TAG, "onConnectionStateChange: requesting higher mtu " + gatt.requestMtu(23));

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
            //Log.d(TAG, "onCharacteristicRead: characteristic = " + characteristic.getUuid().toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                if (characteristic.getUuid().toString().equals(SampleGattAttributes.IMAGE_CHARACTERISTIC_UUID.toString())) {
                    byte[] response = characteristic.getValue();
                    ByteBuffer buffer = ByteBuffer.wrap(response);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    freeSpaceStart = buffer.getInt();
                    freeSpaceEnd = buffer.getInt();
                    Log.d(TAG, "Initialized OTA application, application space " + Integer.toHexString(freeSpaceStart) + "-"+
                    Integer.toHexString(freeSpaceEnd) + " (" + (freeSpaceEnd - freeSpaceStart) + " bytes)");
                    Log.d(TAG,"minAddr: " + minAddr + ", maxAddr: " + maxAddr);
                    startOtaBle(1);
                }
                if(characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_CHARACTERISTIC_UUID.toString())){
                    //Log.i(TAG, "onCharacteristicRead: data received from NEW_IMAGE_CHARACTERISTIC_UUID");
                    byte[] response = characteristic.getValue();

                    ByteBuffer buffer = ByteBuffer.wrap(response);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);

                    int res_ni = buffer.get() & 0xFF;
                    int res_size = buffer.getInt();
                    int res_base = buffer.getInt();

                    if(res_ni != notificationInterval || res_size != (maxAddr-minAddr) || res_base != minAddr){
                        Log.d(TAG, "writing new image characteristic verify failed!");
                    }
                    else {
                        startOtaBle(3);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if(characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID.toString()) &&
                    uploadInProgress){
                //Log.d(TAG, "onCharacteristicChanged: NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID");
                byte[] data = characteristic.getValue();
                notification(data);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String characteristicUUID = characteristic.getUuid().toString();
                //Log.d(TAG, "onCharacteristicWrite successful for characteristic: " + characteristicUUID);

                byte[] writtenValue = characteristic.getValue();
                if (writtenValue != null) {
                    //Log.d(TAG, "Bytes written: " + bytesToHex(writtenValue));
                } else {
                    Log.d(TAG, "No bytes written.");
                }

                if (characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_CHARACTERISTIC_UUID.toString())) {
                    startOtaBle(2);
                }

                if (characteristic.getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_TRANSFER_UNIT_CONTENT_CHARACTERISTIC_UUID.toString())) {
                    imageDataQueue.poll(); // Remove o primeiro elemento da fila, pois foi enviado com sucesso
                    sendImageData(characteristicNewImageTransferUnit);
                }
            } else {
                String characteristicUUID = characteristic.getUuid().toString();
                Log.e(TAG, "onCharacteristicWrite failed for characteristic: " + characteristicUUID);
            }
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x ", b));
            }
            return sb.toString();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.i(TAG, "Descriptor write successful");
                //Log.i(TAG, "Characteristic UUID: " + descriptor.getCharacteristic().getUuid().toString());
                //1Log.i(TAG, "Descriptor UUID: " + descriptor.getUuid().toString());
                if (descriptor.getCharacteristic().getUuid().toString().equals(SampleGattAttributes.NEW_IMAGE_EXPECTED_TRANSFER_UNIT_CHARACTERISTIC_UUID.toString()) &&
                        descriptor.getUuid().toString().equals(SampleGattAttributes.NOTIFICATION_DESCRIPTOR_UUID.toString())) {
                    startOtaBle(4);
                }
            } else {
                Log.e(TAG, "Descriptor write failed with status: " + status);
            }
        }
    };

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }

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
    public int sendData(byte[] data, BluetoothGattCharacteristic characteristic) {
        return send(data, characteristic);
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
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        //Log.i(TAG, "characteristic = " + characteristic.getUuid().toString());
        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.i(TAG, "Descriptor: " + descriptor.getUuid().toString() + " - Notificação Ativada");
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public byte[] writeImageBlock(int sequence, boolean requestAck) {
        byte[] image = this.firmware.toBinArray((int)(this.minAddr + sequence * 16), (int)(this.minAddr + sequence * 16 + 15));
        int needsAck = requestAck ? 1 : 0;
        int checksum = needsAck ^ (sequence % 256) ^ (sequence >> 8);
        for (byte b : image) {
            checksum ^= b;
        }
        byte[] imageData = new byte[20];
        imageData[0] = (byte) checksum;
        System.arraycopy(image, 0, imageData, 1, 16);
        imageData[17] = (byte) needsAck;
        imageData[18] = (byte) (sequence & 0xFF);
        imageData[19] = (byte) ((sequence >> 8) & 0xFF);

        return imageData;
    }

    private void notification(byte[] data) {
        int nextSequence = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
        int error = ((data[3] & 0xFF) << 8) | (data[2] & 0xFF);
        OtaError status = OtaError.valueOf(error);
        Log.d(TAG, String.format("sending block %d of %d", nextSequence, this.lastSequence));
        int percentage = (int) (((double) nextSequence / this.lastSequence) * 100);
        updateProgressBar(percentage, error);
        if (error != 0) {
            Log.d(TAG, String.format("Received notification for expected tu, seq: %d, status: %s%n", nextSequence, status));
            Log.d(TAG, "Received error, retrying...");
            uploadInProgress = false;
            return;
        }
        if (nextSequence <= this.lastSequence) {
            for (int i = nextSequence; i < nextSequence + this.notificationInterval; i++) {
                boolean requestAck = i == nextSequence + this.notificationInterval - 1;
                if (i <= this.lastSequence) {
                    if (i == this.lastSequence) {
                        requestAck = true;
                    }
                    byte[] imageData = writeImageBlock(i, requestAck);
                    imageDataQueue.add(imageData);
                }
            }

            sendImageData(characteristicNewImageTransferUnit);

        } else {
            Log.d(TAG, "\nUpload finished");
            uploadInProgress = false;
        }
    }

    /*
    Função: send(byte[] data)
    Descrição: Envia um array de byts para caracteristicaRX.
    @param: byte[] data -> Array de bytes

    @return:    int length -> Quantidade de dados enviados
    */
    public int send(byte[] data, BluetoothGattCharacteristic characteristic) {
        synchronized (this) {
            if (characteristic != null) {
                characteristic.setValue(data);
                try {
                    mBluetoothGatt.writeCharacteristic(characteristic);
                    //Log.d(TAG, "Sent data to characteristic: " + characteristic.getUuid().toString());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to write characteristic: " + e.getMessage());
                }
            } else {
                Log.i(TAG, "Characteristic is null");
            }
        }
        return data.length;
    }

    private void sendImageData(BluetoothGattCharacteristic characteristic) {
        synchronized (this) {
            if (!imageDataQueue.isEmpty()) {
                if (characteristic != null) {
                    byte[] data = imageDataQueue.peek(); // Obtém o próximo conjunto de dados na fila
                    characteristic.setValue(data);
                    try {
                        mBluetoothGatt.writeCharacteristic(characteristic);
                        //Log.d(TAG, "Sent data to characteristic: " + characteristic.getUuid().toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to write characteristic: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void startOtaBle(int state) {
        if(this.minAddr != 0 && this.maxAddr != 0) {
            switch (state) {
                case 0:
                    readCharacteristic(characteristicImage);
                    break;

                case 1:
                    if(minAddr<freeSpaceStart || minAddr>freeSpaceEnd || maxAddr > freeSpaceEnd){
                        String message = String.format("program image out of allowed range (image: 0x%x-0x%x, device: 0x%x-0x%x)",
                                minAddr, minAddr + maxAddr, freeSpaceStart, freeSpaceEnd);
                        Log.d(TAG, "readFreeSpaceInfo: " + message);
                    }
                    ByteBuffer buffer = ByteBuffer.allocate(9);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    buffer.put((byte) notificationInterval);
                    buffer.putInt((int) (maxAddr - minAddr));
                    buffer.putInt((int) minAddr);
                    byte[] data = buffer.array();

                    StringBuilder sb = new StringBuilder();
                    for (byte b : data) {
                        sb.append(String.format("0x%02X ", b));
                    }
                    //Log.d(TAG, "Data in bytes: " + sb.toString());

                    send(data, characteristicNewImage);
                    break;
                case 2:
                    readCharacteristic(characteristicNewImage);
                    break;
                case 3:
                    setCharacteristicNotification(characteristicNewImageExpectedTransferUnit, true);
                    break;
                case 4:
                    this.lastSequence = (((int)maxAddr - (int)minAddr + 15) >> 4) - 1;
                    Log.d(TAG, "Starting upload, last sequence: " + this.lastSequence);
                    uploadInProgress = true;
                    break;
                default:
                    break;
            }
        }
    }

    public void setFile(long minAddr, long maxAddr, IntelHex file){
        this.minAddr = minAddr;
        this.maxAddr = maxAddr;
        this.firmware = file;
    }
}





