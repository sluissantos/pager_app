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

import java.util.List;

public class BluetoothLeService extends Service implements SendInterface{
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    // Objetos para guardar as caracteristicas
    private BluetoothGattCharacteristic characteristicTx;
    private BluetoothGattCharacteristic characteristicRx;
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

    Handler handler = new Handler();

    ReceiveInterface receiveInterface;


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
                    // Se o serviço for encontrado
                    if (gattService.getUuid().toString().equals(SampleGattAttributes.SERVICEUART_UUID.toString())) {
                        Log.i(TAG, "onServicesDiscovered: service=" + gattService.getUuid());
                        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                            // Log.i(TAG, "onServicesDiscovered: characteristic=" + characteristic.getUuid());
                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.CHARACTERISTICRX_UUID.toString())) {
                                Log.i(TAG, "onServicesDiscovered: found Characteristic (RX)");
                                characteristicRx = characteristic;
                                readCharacteristic(characteristic);
                            }
                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.CHARACTERISTICTX_UUID.toString())) {
                                Log.i(TAG, "onServicesDiscovered: found Characteristic (TX)");
                                characteristicTx = characteristic;
                                // Ativa notificação para recepeção de dados
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
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
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
        if (SampleGattAttributes.CHARACTERISTICTX_UUID.equals(characteristic.getUuid())) {
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
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // Caracteristica TX possui apenas 1 descriptor 0x2902
        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.i(TAG, "Descriptor: " + descriptor.getUuid().toString() + "-Notificação Ativada");
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        // Codigo original
         /*  if (SampleGattAttributes.CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    SampleGattAttributes.BLE_NOTIFICATION);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }*/
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
            this.characteristicRx.setValue(data);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if(this.characteristicRx !=null){
                    Log.i(TAG, "send: Rx not null");
                    mBluetoothGatt.writeCharacteristic(this.characteristicRx);
                }else{
                    Log.i(TAG, "send: Rx null");
                }
            }
        }
        return data.length;
    }

    /*
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementações das funções relacionados ao frame de dados - APIRev01 - Versão em C disponibilizadas
    // nos arquivos de amostra
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public int[] DataBuffer = new int[517]; // Buffer para receber dados de RX
    public int DataBufferSize; // Quantidade de dados recebidos
    private int[] ExpectedDataVetor={0,0};
    private int count=-1;
    private int ExpectedDataQuantity;// Quantidade de dados esperados no pacote.
    private int ReadingDataStatus = SampleGattAttributes.FINDING_NEW_PACKAGE; // Status atual da leitura de dados.

    public List<Short> decaWaveIDList = new ArrayList<>();
    public List<Short> offsetList = new ArrayList<>();
    public List<Short> nearDistanceList = new ArrayList<>();
    public List<Short> farDistanceList = new ArrayList<>();
    ///////////////////////////////////////////////////////////////////////////////////
// Função: extratcCommand(int newData)
// Descrição: Remove o cabeçalho do pacote recebido e compara checksum
// Argumentos:
// Retorno:
//////////////////////////////////////////////////////////////////////////////////
    public void extratcCommand(int newData) {
        // Transforma signed int em unsigned int
        if (newData < 0) {
            newData = newData + 256;
            Log.d("ExtratNegative",""+newData);
        }

        switch (this.ReadingDataStatus) {    // Busca pacote this.ReadingDataStatus = 0x00;
            case SampleGattAttributes.FINDING_NEW_PACKAGE:
                if (newData == SampleGattAttributes.BYTE_STX) {
                    this.ReadingDataStatus = SampleGattAttributes.READING_NEW_PACKAGE;
                }
                break;

            case SampleGattAttributes.READING_NEW_PACKAGE:
                count++;
                this.ExpectedDataVetor[count] = newData;
                if(count == 1){
                    ExpectedDataQuantity = (short)((ExpectedDataVetor[0] & 0xFF)<<8 | (ExpectedDataVetor[1] & 0xFF));
                    this.ReadingDataStatus = SampleGattAttributes.READING_DATA;
                }
                break;

            // Faz a leitura dos dados do pacote
            case SampleGattAttributes.READING_DATA:
                if (this.DataBufferSize < this.ExpectedDataQuantity) {
                    this.DataBuffer[this.DataBufferSize] = newData;
                    this.DataBufferSize++;
                    if (this.DataBufferSize == this.ExpectedDataQuantity) {
                        this.ReadingDataStatus = SampleGattAttributes.READING_CHECKSUM;
                    }
                } else {
                    DiscardPackage();
                }
                break;
            case SampleGattAttributes.READING_CHECKSUM:
                if (CheckSumIsValid(newData, this.DataBufferSize, this.DataBuffer)) {
                    this.ReadingDataStatus = SampleGattAttributes.VALIDATING_PACKAGE;
                } else {
                    DiscardPackage();
                }
                break;
            case SampleGattAttributes.VALIDATING_PACKAGE:
                if (newData == SampleGattAttributes.BYTE_RTX) {
                    this.processCommand(DataBuffer,DataBufferSize);
                    DiscardPackage();
                } else {
                    DiscardPackage();
                }
                break;
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////
// Função: processCommand(int[] DataBuffer, int DataBufferSize)
// Descrição: Após receber um frame valido essa função processa os dados conforme o comando
// Argumentos: int[] DataBuffer -> Array de dados
//             int DataBufferSize -> Tamanho
// Retorno:
/*  dataDevice[0] = (uint8_t)(deviceGet(i)->decawave_ID>>8); // DecaWaveId MSB
    dataDevice[1] = (uint8_t)(deviceGet(i)->decawave_ID); // DecaWaveId LSB
    dataDevice[2] = (uint8_t)(deviceGet(i)->deviceOffset>>8); // Offset MSB
    dataDevice[3] = (uint8_t)(deviceGet(i)->deviceOffset);// Offset LSB
    dataDevice[4] = (uint8_t)(deviceGet(i)->security_far>>8); // Far MSB
    dataDevice[5] = (uint8_t)(deviceGet(i)->security_far);// Far LSB
    dataDevice[6] = (uint8_t)(deviceGet(i)->security_near>>8);// Near MSB
    dataDevice[7] = (uint8_t)(deviceGet(i)->security_near);// Near LSB
    dataDevice[8] = deviceGet(i)->macAdress[0];  // MAC
    dataDevice[9] = deviceGet(i)->macAdress[1];  // MAC
    dataDevice[10] = deviceGet(i)->macAdress[2]; // MAC
    dataDevice[11] = deviceGet(i)->macAdress[3]; // MAC
    dataDevice[12] = deviceGet(i)->macAdress[4]; // MAC
    dataDevice[13] = deviceGet(i)->macAdress[5]; // MAC
//////////////////////////////////////////////////////////////////////////////////
    public void processCommand(int[] DataBuffer,int DataBufferSize){
        //Log.i("Teste:",DataBuffer);
        switch (DataBuffer[0]){
            case SampleGattAttributes.RD_ALL_INFO_DEVICE:
                //if(int16 != -1){}
                if(decaWaveIDList.size() < 5){
                    short int16 = (short)(((DataBuffer[1] & 0xFF) << 8) | (DataBuffer[2] & 0xFF));
                    long int64;
                    if (int16 < 0 ) {
                        int64 = (long)(int16 + 65536);
                        Log.d("ExtratNegative",""+int64);
                    }
                    Log.i(TAG,"DecaWaveIDList"+ decaWaveIDList);
                    decaWaveIDList.add(int16);
                    Log.i(TAG,"processCommand -> Recebendo device info");
                    //Log.i(TAG,"DecaID "+ DataBuffer[1] +" "+ DataBuffer[2]/*int16);
                }
                if(offsetList.size() < 5){
                    short int16 = (short)(((DataBuffer[3] & 0xFF) << 8) | (DataBuffer[4] & 0xFF));
                    offsetList.add(int16);
                    Log.i(TAG,"processCommand -> Recebendo device info");
                    Log.i(TAG,"offset "+ int16);
                }
                if(farDistanceList.size() < 5){
                    short int16 = (short)(((DataBuffer[5] & 0xFF) << 8) | (DataBuffer[6] & 0xFF));
                    farDistanceList.add(int16);
                    Log.i(TAG,"processCommand -> Recebendo device info");
                    Log.i(TAG,"far distance "+ int16);
                }
                if(nearDistanceList.size() < 5){
                    short int16 = (short)(((DataBuffer[7] & 0xFF) << 8) | (DataBuffer[8] & 0xFF));
                    nearDistanceList.add(int16);
                    Log.i(TAG,"processCommand -> Recebendo device info");
                    Log.i(TAG,"near distance "+ int16);
                }
                break;
            case SampleGattAttributes.RD_REQUEST_CONNECTION:
                //Apenas responde para o AURA que esse device
                sendDeviceID((byte)DataBuffer[1],100);
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
// Função: DiscardPackage(byte commando, byte[] dataBuffer, int dataBufferSize)
// Descrição: Discarta o pacote caso ocorra a detecção de alguma falha nos dados.
// Argumentos:
// Retorno:
//////////////////////////////////////////////////////////////////////////////////
    public void DiscardPackage() {
        this.ReadingDataStatus = SampleGattAttributes.FINDING_NEW_PACKAGE;
        this.DataBuffer = new int[517];
        this.DataBufferSize = 0;
        this.ExpectedDataQuantity = 0;
        this.count=-1;
    }

    ///////////////////////////////////////////////////////////////////////////////////
// Função: CheckSumIsValid(int csReference, int DabufferSize,int[] DataBuffer)
// Descrição: Testa se o checksum do pacote a ser recebido é valido
// Argumentos: int csReference -> Checksun de referência
//             int DataBufferSize -> Tamanho do buffer de dados
//             int[] DataBuffer -> Vetor de bytes contendo os dados
//
// Retorno:    boolean csIsValid -> true
//                               -> false
//////////////////////////////////////////////////////////////////////////////////
    public boolean CheckSumIsValid(int csReference, int DataBufferSize, int[] DataBuffer) {
        boolean csIsValid = false;
        int checkSum = 0;
        if (csReference < 0) {
            csReference = csReference + 256;
        }
        for (int i = 0; i < DataBufferSize; i++) {
            if (DataBuffer[i] < 0) {
                checkSum += DataBuffer[i] + 256;
            } else {
                checkSum += DataBuffer[i];
            }
        }
        // Corta tamanho calculado para BYTE
        checkSum &= 0xFF;
        if (checkSum == csReference)
            csIsValid = true;

        return csIsValid;
    }

    ///////////////////////////////////////////////////////////////////////////////////
// Função: sendCommando(byte commando, byte[] dataBuffer, int dataBufferSize)
// Descrição: Monta o frame de dados com o cabeçalho e envia para caracteristica
// Argumentos: byte commando -> RD_ALL_INFO_DEVICE = 0xD1 (Solicita os ID dos periféricos)
//                              WR_ZONE_FAR_DISTANCE = 0xD3 (Configura Zona1 )
//                              WR_ZONE_NEAR_DISTANCE = 0XD4 (Configura Zona2)
//                              WR_PERIOD_REQUEST = 0xD5 (Configura taxa de requisição)
//            byte[] dataBuffer -> Array contendo os dados a serem enviados
//            int dataBufferSize-> Tamanho do array de dados
// Retorno:   int dataWasSended -> Quantidade de dados enviados
//////////////////////////////////////////////////////////////////////////////////
    public int sendCommand(byte command, byte[] dataBuffer, int dataBufferSize) {
        // Flag de dado enviado.
        int dataWasSended;
        int checkSum = command;
        int i;

        int packageBufferSize = dataBufferSize + 5;
        byte[] packageBuffer = new byte[packageBufferSize];

        packageBuffer[0] = (byte) 0x02; // Insere byte inicial no pacote
        packageBuffer[1] = (byte) (dataBufferSize + 1); // Insere tamanho do comando no buffer
        packageBuffer[2] = command;
        for (i = 0; i < dataBufferSize; i++) {
            packageBuffer[i + 3] = dataBuffer[i]; // Insere dados do comando no buffer
            checkSum += dataBuffer[i]; // Calcula checksum
        }
        checkSum &= 0xFF; // Corta tamanho calculado para BYTE
        packageBuffer[i + 3] = (byte) checkSum; // Insere checksum no pacote
        packageBuffer[i + 4] = (byte) 0x03; // Insere byte final no pacote

        dataWasSended = send(packageBuffer);

        return dataWasSended;
    }

    /*


 ///////////////////////////////////////////////////////////////////////////////////
// Função: sendDistanciaZona1(int distancia, long delay)
// Descrição: Envia a distancia definida para zona 1.
// Argumentos: int distancia -> Distancia em mm
//             long delay -> Delay em ms
//             (O android perde alguns frames, então eu atraso as funções para ele porder processar
////             acredito que acionar essas funções via intent seja interessante)
// Retorno:    int length -> Quantidade de dados enviados (Nao funcional)
//////////////////////////////////////////////////////////////////////////////////
    public int sendDistanciaZona1(int distancia,long delay){
        int length=0;
        byte[] data ={(byte)0xFF,
                (byte)0xFF,
                (byte)(distancia >>8),
                (byte)(distancia)};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_ZONE_FAR_DISTANCE,data,4);
            }
        };
        handler.postDelayed(runnable,delay);
        Log.i(TAG,"senDistanceZona1");
        return length;
    }

    ///////////////////////////////////////////////////////////////////////////////////
// Função: sendDistanciaZona2(int distancia, long delay)
// Descrição: Envia a distancia definida para zona 1.
// Argumentos: int distancia -> Distancia em mm
//             long delay -> Delay em ms
//             (O android perde alguns frames, então eu atraso as funções para ele porder processar
////             acredito que acionar essas funções via intent seja interessante)
// Retorno:    int length -> Quantidade de dados enviados (Nao funcional)
//////////////////////////////////////////////////////////////////////////////////
    public int sendDistanciaZona2(int distancia,long delay){
        int length=0;
        byte[] data ={(byte)0xFF,
                (byte)0xFF,
                (byte)(distancia >>8),
                (byte)(distancia)};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
               sendCommand((byte)SampleGattAttributes.WR_ZONE_NEAR_DISTANCE,data,4);
            }
        };
        handler.postDelayed(runnable,delay);

        Log.i(TAG,"senDistanceZona2");
        return 0;
    }

    ///////////////////////////////////////////////////////////////////////////////////
// Função: sendTaxaRequisicao(int taxa, long delay)
// Descrição: Envia a distancia definida para zona 1.
// Argumentos: int taxa -> Os valores da taxa são tabelas conforme a seguir
//                          0 - Representa 1300 ms
//                          1 - Representa  650 ms
//                          2 - Representa  380 ms
//             long delay -> Delay em ms para envio da menssagem.
//             (O android perde alguns frames, então eu atraso as funções para ele porder processar
//             acredito que acionar essas funções via intent seja interessante)
// Retorno:    int length -> Quantidade de dados enviados (Nao funcional)
//////////////////////////////////////////////////////////////////////////////////
    public int sendTaxaRequisicao(int taxa,long delay){
        int length=0;
        if(taxa >2)
            taxa = 2;

        if(taxa < 0)
            taxa = 0;

        byte[] data ={(byte)taxa};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_PERIOD_REQUEST,data,1);
            }
        };
        handler.postDelayed(runnable,delay);
        Log.i(TAG,"TaxaRequisiçao");
        return length;
    }


    ///////////////////////////////////////////////////////////////////////////////////
// Função: solicitaDeviceInfo()
// Descrição: Envia um comando de solicitação para retorna dados dos disposivos conectados.
// Argumentos: long delay -> Atraso do envio
// Retorno:    int length -> Quantidade de dados enviados (Nao funcional)
    // Funcionamento
// Envio: solicitaDeviceInfo()-> (AURA Processa Comando e retorna dados disponiveis)
// Recepção: onCharacteristicChanged()-> extratCommand() -> ProcessComando
//////////////////////////////////////////////////////////////////////////////////
    public int solicitaDeviceInfo(long delay){
        // limpa a lista para receber novos IDs
        decaWaveIDList.clear();
        byte data[] ={(byte)SampleGattAttributes.RD_ALL_INFO_DEVICE};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.RD_ALL_INFO_DEVICE,data,0);
            }
        };
        handler.postDelayed(runnable,delay);
        Log.i(TAG,"Solicita Device Info");
        return 0;
    }

    ///////////////////////////////////////////////////////////////////////////////////
// Função: sendOffsetCommand(int decaWaveId, int offset, long delay)
// Descrição: Envia um comando para configurar o offset do periférico baseado no decaWaveID
// Argumentos: int decaWaveId-> Identificador do periférico
// Retorno:    int offset -> Valor do offset em mm
    // Funcionamento
// Envio: solicitaDeviceInfo()-> (AURA Processa Comando e retorna dados disponiveis)
// Recepção: onCharacteristicChanged()-> extratCommand() -> ProcessComando
//////////////////////////////////////////////////////////////////////////////////
public int sendOffsetCommand(int decaWaveId, int offset,long delay){
        byte data[] ={(byte)(decaWaveId>>8),
                (byte)decaWaveId,
                (byte)(offset >>8),
                (byte) offset};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_PERIPHERAL_OFFSET,data,4);
            }
        };
        handler.postDelayed(runnable,delay);
        Log.i(TAG,"Device Info");
        return 0;
    }

    // Função: sendOffsetCommand(int decaWaveId, int offset, long delay)
// Descrição: Envia um comando para configurar o offset do periférico baseado no decaWaveID
// Argumentos: int decaWaveId-> Identificador do periférico
// Retorno:    int offset -> Valor do offset em mm
    // Funcionamento
// Envio: solicitaDeviceInfo()-> (AURA Processa Comando e retorna dados disponiveis)
// Recepção: onCharacteristicChanged()-> extratCommand() -> ProcessComando
//////////////////////////////////////////////////////////////////////////////////
    public int sendDeviceID(byte id,long delay){
        byte data[] ={(byte)(id)};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.RD_REQUEST_CONNECTION,data,1);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public List<Short> getDecaWaveIDList(){
        return this.decaWaveIDList;
    }
    public List<Short> getOffsetList(){
        return this.offsetList;
    }
    public List<Short> getFarDistanceList(){
        return this.farDistanceList;
    }
    public List<Short> getNearDistanceList(){
        return this.nearDistanceList;
    }
*/
}





