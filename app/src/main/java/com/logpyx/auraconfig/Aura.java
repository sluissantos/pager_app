package com.logpyx.auraconfig;

import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Aura extends Protocol implements ReceiveInterface{
    public List<Short> decaWaveIDList = new ArrayList<>();
    public List<Short> offsetList = new ArrayList<>();
    public List<Short> nearDistanceList = new ArrayList<>();
    public List<Short> farDistanceList = new ArrayList<>();

    private Handler handler = new Handler();

    public Aura(SendInterface sendInterface){
        super(sendInterface);
    }

    @Override
    void processCommand(int[] DataBuffer) {
        for (int i=0; i<DataBufferSize; i++){
            if(DataBuffer[i]<0){
                DataBuffer[i]+=256;
            }
            Log.i(TAG, "DataBuffer["+i+"]="+Integer.toString(DataBuffer[i]));
        }
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
                    //Log.i(TAG,"DecaID "+ DataBuffer[1] +" "+ DataBuffer[2]/*int16*/);
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
            case SampleGattAttributes.WR_GET_OVERCRANE_PARAM:
                byte[] packageBuffer = new byte[517];
                for(int i=1; i<DataBuffer.length; i++){
                    if(DataBuffer[i]<0){
                        DataBuffer[i]+=256;
                    }
                    packageBuffer[i-1] = (byte)DataBuffer[i];
                }
                break;

            case SampleGattAttributes.WR_ID_CELULAR:
                int device = (byte)(DataBuffer[1] >>8);
                DeviceControlActivity deviceControlActivity = (DeviceControlActivity) ActivityManager.getCurrentActivity();
                if (deviceControlActivity != null) {
                    deviceControlActivity.updateArgument(Integer.toString(device)); // Substitua "argumentValue" pelo novo valor
                }
                Log.i(TAG, "device = "+device);
                break;
        }
    }

   /*
    Função: sendDistanciaZona1(int distancia, long delay)
    Descrição: Envia a distancia definida para zona 1.
    @param: int distancia -> Distancia em mm
                long delay -> Delay em ms
                (O android perde alguns frames, então eu atraso as funções para ele porder processar
                acredito que acionar essas funções via intent seja interessante)
    @return:    int length -> Quantidade de dados enviados (Nao funcional)
    */
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

    /*
    Função: sendDistanciaZona2(int distancia, long delay)
    Descrição: Envia a distancia definida para zona 1.
    @param: int distancia -> Distancia em mm
                 long delay -> Delay em ms
                 (O android perde alguns frames, então eu atraso as funções para ele porder processar
                 acredito que acionar essas funções via intent seja interessante)
    @return:    int length -> Quantidade de dados enviados (Nao funcional)
    */
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

    /*
    Função: sendTaxaRequisicao(int taxa, long delay)
    Descrição: Envia a distancia definida para zona 1.
    @param: int taxa -> Os valores da taxa são tabelas conforme a seguir
                                0 - Representa 1300 ms
                                1 - Representa  650 ms
                                2 - Representa  380 ms
                        long delay -> Delay em ms para envio da menssagem.
                        (O android perde alguns frames, então eu atraso as funções para ele poder processar
                         acredito que acionar essas funções via intent seja interessante)
    @return:    int length -> Quantidade de dados enviados (Nao funcional)
    */
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

    /*
    Função: solicitaDeviceInfo()
    Descrição: Envia um comando de solicitação para retorna dados dos disposivos conectados.
    @param: long delay -> Atraso do envio
    @return:    int length -> Quantidade de dados enviados (Nao funcional)
    Funcionamento
    Envio: solicitaDeviceInfo()-> (AURA Processa Comando e retorna dados disponiveis)
    Recepção: onCharacteristicChanged()-> extratCommand() -> ProcessComando
    */
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

    /*
    Função: sendOffsetCommand(int decaWaveId, int offset, long delay)
    Descrição: Envia um comando para configurar o offset do periférico baseado no decaWaveID
    @param: int decaWaveId-> Identificador do periférico
    @return:    int offset -> Valor do offset em mm
    Funcionamento
    Envio: solicitaDeviceInfo()-> (AURA Processa Comando e retorna dados disponiveis)
    Recepção: onCharacteristicChanged()-> extratCommand() -> ProcessComando
    */
    public int sendOffsetCommand(int decaWaveId, int offset,long delay){
        byte data[] ={(byte)(decaWaveId>>8),
                (byte)decaWaveId,
                (byte)(offset>>8),
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

    /*
    Função: sendOffsetCommand(int decaWaveId, int offset, long delay)
    Descrição: Envia um comando para configurar o offset do periférico baseado no decaWaveID
    @param: int decaWaveId-> Identificador do periférico
    @return:    int offset -> Valor do offset em mm
    Funcionamento
    Envio: solicitaDeviceInfo()-> (AURA Processa Comando e retorna dados disponiveis)
    Recepção: onCharacteristicChanged()-> extratCommand() -> ProcessComando
    */
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

    public int sendOpCode(byte opcode, long delay){
        byte data[] ={(byte)opcode};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand(opcode,data,0);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public int sendOtaUrl(int[] ip, int port, long delay){
        byte data[] ={(byte)ip[0],
                (byte)ip[1],
                (byte)ip[2],
                (byte)ip[3],
                (byte)(port>>8),
                (byte)port};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_URL_HTTPS_SERVER_OTA,data,6);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public int sendGetOvercraneParam(long delay){
        byte data[] ={(byte)SampleGattAttributes.WR_GET_OVERCRANE_PARAM};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_GET_OVERCRANE_PARAM,data,0);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public int sendSetOvercraneParam(int xOffSet, int yOffSet, int minSecDist, int maxSecDist, int minAltura, int maxAltura, long delay){
        byte data[] ={(byte)(xOffSet>>8),
                (byte)xOffSet,
                (byte)(yOffSet>>8),
                (byte)yOffSet,
                (byte)(minSecDist>>8),
                (byte)minSecDist,
                (byte)(maxSecDist>>8),
                (byte)maxSecDist,
                (byte)(minAltura>>8),
                (byte)minAltura,
                (byte)(maxAltura>>8),
                (byte)maxAltura};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_SET_OVERCRANE_PARAM,data,12);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public int sendAuraMode(int mode, long delay){
        byte data[] ={(byte)mode};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_SET_OVERCRANE_MODE,data,1);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public int sendFixTag(byte[] tag, long delay) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte) SampleGattAttributes.WR_SET_FIX_TAG, tag, tag.length);
            }
        };
        handler.postDelayed(runnable, delay);
        return 0;
    }

    public int sendDecawaveIDList(byte[] tagList, long delay) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte) SampleGattAttributes.WR_DECA_ID_LIST, tagList, tagList.length);
            }
        };
        handler.postDelayed(runnable, delay);
        return 0;
    }

    public int sendNearFarDistance(byte opcode, int distance, long delay){
        byte data[] ={(byte)255,
                (byte)255,
                (byte)(distance>>8),
                (byte)distance};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand(opcode,data,4);
            }
        };
        handler.postDelayed(runnable,delay);
        return 0;
    }

    public int sendTagIdDistance(int device, byte[] tag, int distance, long delay){
        byte data[] ={(byte)255,
                (byte)255,
                (byte)tag[0],
                (byte)tag[1],
                (byte)(distance>>8),
                (byte)distance,
                (byte)device};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendCommand((byte)SampleGattAttributes.WR_TAG_ID_DISTANCE,data,7);
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

    @Override
    public void extractMessage(int newData) {
        extractCommand(newData);
    }
}
