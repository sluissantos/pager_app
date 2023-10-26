package com.logpyx.auraconfig;

import android.util.Log;

import androidx.annotation.LongDef;

public abstract class Protocol implements ReceiveInterface{
    private BluetoothLeService bluetoothLeService;
    public final static String TAG = Aura.class.getSimpleName();
    public int[] OpcodeDataBuffer = new int[517]; // Buffer para receber dados de RX
    public int DataBufferSize; // Quantidade de dados recebidos
    public int packageLenght;
    public int count=-1;
    public int ExpectedDataQuantity;// Quantidade de dados esperados no pacote.
    public int ReadingDataStatus = SampleGattAttributes.FINDING_NEW_PACKAGE; // Status atual da leitura de dados.

    SendInterface sendInterface;

    public Protocol(SendInterface sendInterface){
        this.sendInterface = sendInterface;
        this.sendInterface.setReceiveInterface(this);
    }

    /*
    Função: sendCommando(byte commando, byte[] dataBuffer, int dataBufferSize)
    Descrição: Monta o frame de dados com o cabeçalho e envia para caracteristica
    @param: byte commando -> RD_ALL_INFO_DEVICE = 0xD1 (Solicita os ID dos periféricos)
                                  WR_ZONE_FAR_DISTANCE = 0xD3 (Configura Zona1 )
                                  WR_ZONE_NEAR_DISTANCE = 0XD4 (Configura Zona2)
                                  WR_PERIOD_REQUEST = 0xD5 (Configura taxa de requisição)
                byte[] dataBuffer -> Array contendo os dados a serem enviados
                int dataBufferSize-> Tamanho do array de dados
    @return:   int dataWasSended -> Quantidade de dados enviados
    */
    public int sendCommand(byte command, byte[] dataBuffer, int dataBufferSize) {
        int dataWasSended=0;
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
        //Log.i("TAG", "sendCommand() called with: command = [" + command + "], dataBuffer = [" + dataBuffer + "], dataBufferSize = [" + dataBufferSize + "]");

        if (this.sendInterface != null) {
            //Log.i("TAG", "sendCommand() called with: command = [" + command + "], dataBuffer = [" + dataBuffer + "], dataBufferSize = [" + dataBufferSize + "]");
            dataWasSended = sendInterface.sendData(packageBuffer);
        }
        return dataWasSended;
    }
    /*
    Função: extratcCommand(int newData)
    Descrição: Remove o cabeçalho do pacote recebido e compara checksum
    @param: newData
    @return:
    */
    void extractCommand(int newData) {
        // Transforma signed int em unsigned int

        switch (this.ReadingDataStatus) {    // Busca pacote this.ReadingDataStatus = 0x00;
            case SampleGattAttributes.FINDING_NEW_PACKAGE:
                if (newData == SampleGattAttributes.BYTE_STX) {
                    this.ReadingDataStatus = SampleGattAttributes.READING_NEW_PACKAGE;
                }
                break;

            case SampleGattAttributes.READING_NEW_PACKAGE:
                this.packageLenght = newData;
                this.ReadingDataStatus = SampleGattAttributes.READING_OPCODE_DATA;
                break;

            // Faz a leitura dos dados do pacote
            case SampleGattAttributes.READING_OPCODE_DATA:
                if (this.DataBufferSize < this.packageLenght) {
                    this.OpcodeDataBuffer[this.DataBufferSize] = newData;
                    this.DataBufferSize++;
                    if (this.DataBufferSize == this.packageLenght) {
                        this.ReadingDataStatus = SampleGattAttributes.READING_CHECKSUM;
                    }
                } else {
                    DiscardPackage();
                }
                break;
            case SampleGattAttributes.READING_CHECKSUM:
                if (CheckSumIsValid(newData, this.OpcodeDataBuffer)) {
                    this.ReadingDataStatus = SampleGattAttributes.VALIDATING_PACKAGE;
                } else {
                    DiscardPackage();
                }
                break;
            case SampleGattAttributes.VALIDATING_PACKAGE:
                if (newData == SampleGattAttributes.BYTE_RTX) {
                    this.processCommand(OpcodeDataBuffer);
                    DiscardPackage();
                } else {
                    DiscardPackage();
                }
                break;
        }
    }

    /*
    Função: DiscardPackage()
    Descrição: Discarta o pacote caso ocorra a detecção de alguma falha nos dados.
    @param:
    @return:
    */
    private void DiscardPackage() {
        this.ReadingDataStatus = SampleGattAttributes.FINDING_NEW_PACKAGE;
        this.OpcodeDataBuffer = new int[517];
        this.DataBufferSize = 0;
        this.ExpectedDataQuantity = 0;
        this.count=-1;
    }

    /*
    Função: CheckSumIsValid(int csReference, int DabufferSize,int[] DataBuffer)
    Descrição: Testa se o checksum do pacote a ser recebido é valido
    @param: int csReference -> Checksun de referência
                int DataBufferSize -> Tamanho do buffer de dados
                int[] DataBuffer -> Vetor de bytes contendo os dados

    @return:    boolean csIsValid -> true
                                  -> false
    */
    private boolean CheckSumIsValid(int csReference, int[] OpcodeDataBuffer) {
        boolean csIsValid = false;
        int checkSum = 0;
        if (csReference < 0) {
            csReference = csReference + 256;
        }
        for (int i = 0; i < OpcodeDataBuffer.length; i++) {
            if (OpcodeDataBuffer[i] < 0) {
                checkSum += OpcodeDataBuffer[i] + 256;
            } else {
                checkSum += OpcodeDataBuffer[i];
            }
        }
        // Corta tamanho calculado para BYTE
        checkSum &= 0xFF;
        if (checkSum == csReference)
            csIsValid = true;

        return csIsValid;
    }

    /*
    Função: processCommand(int[] DataBuffer, int DataBufferSize)
    Descrição: Após receber um frame valido essa função processa os dados conforme o comando
    @param: int[] DataBuffer -> Array de dados
               int DataBufferSize -> Tamanho
    @return:    dataDevice[0] = (uint8_t)(deviceGet(i)->decawave_ID>>8); // DecaWaveId MSB
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
    */

    abstract void processCommand(int[] DataBuffer);

    private void resetStateExtractCommand(){

    }
}
