package com.logpyx.auraconfig;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DeviceControlActivity extends AppCompatActivity{
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    //parte dos extras que eu fiz no main pra tela de configuração
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    //parte de configuração de tela
    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothLeService bluetoothLeService;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private List<Short> decaWaveIDList = new ArrayList<>();
    public List<Short> offsetList = new ArrayList<>();
    public List<Short> nearDistanceList = new ArrayList<>();
    public List<Short> farDistanceList = new ArrayList<>();
    private short farDistance;
    private short nearDistance;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    // Code to manage Service lifecycle.
    public Aura aura;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        //parte de conectar com um dispositivo, usa o mDeviceAddress
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            //alterei para o que se ebcibtra  no site do android studio, antes n tinha esse if
            if (bluetoothLeService != null) {
                if (!bluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                bluetoothLeService.connect(mDeviceAddress);
                aura = new Aura((SendInterface) bluetoothLeService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            ServiceConnection.super.onNullBinding(name);
            System.out.println("nome do pacote" + name.getPackageName());
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            }
        }
    };

    private TextView zona1TextView;
    private TextView zona2TextView;
    private TextView offsetTextView;
    private TextView prTextView;
    private TextView zona1_txt;
    private TextView zona2_txt;
    private TextView offset_txt;

    private TextView firstIpUrlOTATextView;
    private TextView secondIpUrlOTATextView;
    private TextView thirdIpUrlOTATextView;
    private TextView fourthIpUrlOTATextView;

    private TextView portUrlOTATextView;

    private TextView xOffsetTextView;

    private TextView yOffsetTextView;

    private TextView minSecDistTextView;

    private TextView maxSecDistTextView;

    private TextView maxAlturaTextView;

    private TextView minAlturaTextView;

    private TextView auraModeTextView;

    private TextView fixTagTextView;

    private TextView firstDecawaveIDTextView;

    private TextView secondDecawaveIDTextView;

    private TextView thirdDecawaveIDTextView;

    private TextView nearDistanceTextView;

    private TextView farDistanceTextView;

    private TextView deviceTagIdDistanceTextView;
    private TextView tagTagIdDistanceTextView;
    private TextView distanceTagIdDistanceTextView;

    private TextView pr_txt;
    private String taxaReq;
    private String periferico;

    private Button reconectar;

    private Button otaStart;

    private Button otaURL;

    private Button dfuStart;

    private Button getOverCraneParam;

    private Button setOverCraneParam;

    private Button auraMode;

    private Button setTag;

    private Button versionRequest;

    private Button setDecawaveIDList;

    private Button setNearDistance;

    private Button setFarDistance;

    private Button setTagIdDistance;


    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(R.drawable.toolbarimage);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));

        //simular a tela de configuração que fiz
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Log.d("Address Device", mDeviceAddress);

        ((TextView) findViewById(R.id.device_name)).setText(mDeviceName);
        mConnectionState = findViewById(R.id.connection_state);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        reconectar = findViewById(R.id.reconnectButton);
        reconectar.setVisibility(View.GONE);

        otaStart = findViewById(R.id.otaStart);
        otaURL = findViewById(R.id.setOtaURL);
        dfuStart = findViewById(R.id.dfuStart);
        getOverCraneParam = findViewById(R.id.getOvercraneParam);
        setOverCraneParam = findViewById(R.id.setOvercraneParam);
        auraMode = findViewById(R.id.auraMode);
        setTag = findViewById(R.id.fixTag);
        versionRequest = findViewById(R.id.versionRequest);
        setDecawaveIDList = findViewById(R.id.setDecaWaveIDList);
        setFarDistance = findViewById(R.id.setFarDistance);
        setFarDistance = findViewById(R.id.setFarDistance);
        setTagIdDistance = findViewById(R.id.setTagIdDistance);

        //taxaReqTextView=findViewById(R.id.taxar_edit);
        zona1TextView = findViewById(R.id.zona1_edit);
        zona2TextView = findViewById(R.id.zona2_edit);
        offsetTextView = findViewById(R.id.offset_edit);
        firstIpUrlOTATextView = findViewById(R.id.firstIp_edit);
        secondIpUrlOTATextView = findViewById(R.id.secondIp_edit);
        thirdIpUrlOTATextView = findViewById(R.id.thirdIp_edit);
        fourthIpUrlOTATextView = findViewById(R.id.fourthIp_edit);
        portUrlOTATextView = findViewById(R.id.port_idit);
        xOffsetTextView = findViewById(R.id.xOffset_edit);
        yOffsetTextView = findViewById(R.id.yOffset_edit);
        minSecDistTextView = findViewById(R.id.minSecureDist_edit);
        maxSecDistTextView = findViewById(R.id.maxSecureDist_edit);
        minAlturaTextView = findViewById(R.id.minAltura_edit);
        maxAlturaTextView = findViewById(R.id.maxAltura_edit);
        auraModeTextView = findViewById(R.id.setMode_edit);
        fixTagTextView = findViewById(R.id.fixTag_edit);
        firstDecawaveIDTextView = findViewById(R.id.firstDecawaveID_edit);
        secondDecawaveIDTextView = findViewById(R.id.secondDecawaveID_edit);
        thirdDecawaveIDTextView = findViewById(R.id.thirdDecawaveID_edit);
        nearDistanceTextView = findViewById(R.id.nearDistance_edit);
        farDistanceTextView = findViewById(R.id.farDistance_edit);
        deviceTagIdDistanceTextView = findViewById(R.id.deviceTagIdDistance_edit);
        tagTagIdDistanceTextView = findViewById(R.id.tagTagIdDistance_edit);
        distanceTagIdDistanceTextView = findViewById(R.id.distanceTagIdDistance_edit);

        if (bluetoothLeService != null) {
            //  taxaReqTextView.setText(input.substring(0,4));
            //  zona1TextView.setText(input.substring(4,6));
            //  zona2TextView.setText(input.substring(6,8))
        }

        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> SpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.spinner_txt, android.R.layout.simple_spinner_item);
        SpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(SpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mConnected) {
                    String text = parent.getItemAtPosition(position).toString();
                    Toast.makeText(parent.getContext(), text, Toast.LENGTH_LONG).show();
                    taxaReq = text;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    public void updateArgument(final String deviceId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "ENTROU AQUI EM DOIDAO");
                deviceTagIdDistanceTextView.setText(deviceId);
            }
        });
    }

    public void handleReconnect(View view){
        if(!mConnected && bluetoothLeService != null){
            bluetoothLeService.connect(mDeviceAddress);
        }
    }

    public void handleOtaStart(View view){
        if (mConnected){
            try{
                aura.sendOpCode((byte)SampleGattAttributes.WR_OTA_START, 600);
                Toast.makeText(getApplicationContext(), "Enviado comando OTA START", Toast.LENGTH_SHORT).show();
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar o comando OTA Start", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handlesetOtaURL(View view){
        if (mConnected){
            int[] ip = new int[4];
            try {
                String ip1 = firstIpUrlOTATextView.getText().toString();
                String ip2 = secondIpUrlOTATextView.getText().toString();
                String ip3 = thirdIpUrlOTATextView.getText().toString();
                String ip4 = fourthIpUrlOTATextView.getText().toString();
                String port = portUrlOTATextView.getText().toString();
                if(ip1.isEmpty() || ip2.isEmpty() || ip3.isEmpty() || ip4.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Preencha todos os campos do url.", Toast.LENGTH_LONG).show();
                }
                if (port.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Preencha o campor Porta.", Toast.LENGTH_LONG).show();
                } else {
                    ip[0] = Integer.parseInt(ip1);
                    ip[1] = Integer.parseInt(ip2);
                    ip[2] = Integer.parseInt(ip3);
                    ip[3] = Integer.parseInt(ip4);
                    aura.sendOtaUrl(ip, Integer.parseInt(port), 600);
                }
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando SET OTA URL.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleDfuStart(View view){
        if (mConnected){
            try{
                aura.sendOpCode((byte)SampleGattAttributes.WR_DFU_START, 600);
                Toast.makeText(getApplicationContext(), "Enviado comando DFU START", Toast.LENGTH_SHORT).show();
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar o comando DFU Start", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleSetAuraMode(View view){
        if(mConnected){
            try {
                String modeText = auraModeTextView.getText().toString();
                if(modeText.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Defina um valor para o Modo Aura", Toast.LENGTH_LONG).show();
                }
                else {
                    int mode = Integer.parseInt(modeText);
                    aura.sendAuraMode(mode, 600);
                    if(mode == 0){
                        Toast.makeText(getApplicationContext(), "Forklift mode", Toast.LENGTH_LONG).show();
                    }
                    else if(mode == 1){
                        Toast.makeText(getApplicationContext(), "Over Crane mode", Toast.LENGTH_LONG).show();
                    }
                    else if(mode == 2){
                        Toast.makeText(getApplicationContext(), "Access Control mode", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando Aura Mode", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleFixTag(View view) {
        if (mConnected) {
            try {
                String tagText = fixTagTextView.getText().toString();
                if (tagText.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Defina uma TAG", Toast.LENGTH_LONG).show();
                } else {
                    String tag = tagText.substring(tagText.length() - 4);
                    byte[] tagBytes = hexStringToByteArray(tag);
                    aura.sendFixTag(tagBytes, 600);
                    Toast.makeText(getApplicationContext(), "Fixed TAG " + tag, Toast.LENGTH_LONG).show();
                }
                fixTagTextView.setText("");
            } catch (Exception t) {
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando Fix TAG", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }


    public void handleGetOvercraneParam(View view){
        if (mConnected){
            try{
                aura.sendGetOvercraneParam(600);
                Toast.makeText(getApplicationContext(), "Enviado comando GET OVER CRANE PARAM", Toast.LENGTH_SHORT).show();
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar o comando GET OVER CRANE PARAM", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleSetOverCraneParam(View view){
        if (mConnected){
            try{
                String xOffSet = xOffsetTextView.getText().toString();
                String yOffSet = yOffsetTextView.getText().toString();
                String minSecDist = minSecDistTextView.getText().toString();
                String maxSecDist = maxSecDistTextView.getText().toString();
                String minAltura = minAlturaTextView.getText().toString();
                String maxAltura = maxAlturaTextView.getText().toString();
                if(xOffSet.isEmpty() || yOffSet.isEmpty() || minSecDist.isEmpty() || maxSecDist.isEmpty() ||
                    minAltura.isEmpty() || maxAltura.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Preencha todos os parâmetros", Toast.LENGTH_SHORT).show();
                } else {
                    aura.sendSetOvercraneParam(Integer.parseInt(xOffSet), Integer.parseInt(yOffSet), Integer.parseInt(minSecDist), Integer.parseInt(maxSecDist),
                            Integer.parseInt(minAltura), Integer.parseInt(maxAltura), 600);
                    Toast.makeText(getApplicationContext(), "Enviado comando SET OVER CRANE PARAM", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar o comando SET OVER CRANE PARAM", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleVersionRequest(View view){
        if (mConnected){
            try{
                aura.sendOpCode((byte)SampleGattAttributes.VERSION_REQUEST, 600);
                Toast.makeText(getApplicationContext(), "Enviado comando VERSION REQUEST", Toast.LENGTH_SHORT).show();
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar o comando VERSION REQUEST", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleSetDecawaveIDList(View view) {
        if (mConnected) {
            try {
                String tag1 = firstDecawaveIDTextView.getText().toString();
                String tag2 = secondDecawaveIDTextView.getText().toString();
                String tag3 = thirdDecawaveIDTextView.getText().toString();
                if (tag1.isEmpty() || tag2.isEmpty() ||tag3.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Defina as 3 TAGs Decawave", Toast.LENGTH_LONG).show();
                } else {
                    String tagList = tag1.substring(tag1.length() - 4);
                    tagList = tagList.concat(tag2.substring(tag2.length() - 4)).concat(tag3.substring(tag3.length() - 4));
                    byte[] tagBytes = hexStringToByteArray(tagList);
                    aura.sendDecawaveIDList(tagBytes, 600);
                    Toast.makeText(getApplicationContext(), "Fixed Decawave TAG List " + tagList, Toast.LENGTH_LONG).show();
                }
                //fixTagTextView.setText("");
            } catch (Exception t) {
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando SET DECAWAVE TAG LIST", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleSetNearDistance(View view){
        if(mConnected){
            try {
                String nearDistance = nearDistanceTextView.getText().toString();
                if(nearDistance.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Defina um valor para ZONA VERMELHA", Toast.LENGTH_LONG).show();
                }
                else {
                    int distance = Integer.parseInt(nearDistance);
                    aura.sendNearFarDistance((byte)SampleGattAttributes.WR_ZONE_NEAR_DISTANCE, distance, 600);
                    Toast.makeText(getApplicationContext(), "Definido um valor para ZONA VERMELHA", Toast.LENGTH_LONG).show();
                }
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando SET NEAR DISTANCE", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleSetFarDistance(View view){
        if(mConnected){
            try {
                String farDistance = farDistanceTextView.getText().toString();
                if(farDistance.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Defina um valor para ZONA AMARELA", Toast.LENGTH_LONG).show();
                }
                else {
                    int distance = Integer.parseInt(farDistance);
                    aura.sendNearFarDistance((byte)SampleGattAttributes.WR_ZONE_FAR_DISTANCE, distance, 600);
                    Toast.makeText(getApplicationContext(), "Definido um valor para ZONA AMARELA", Toast.LENGTH_LONG).show();
                }
            } catch (Exception t){
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando SET FAR DISTANCE", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleSetTagIdDistance(View view) {
        if (mConnected) {
            try {
                String deviceText = deviceTagIdDistanceTextView.getText().toString();
                String tagText = tagTagIdDistanceTextView.getText().toString();
                String distanceText = distanceTagIdDistanceTextView.getText().toString();

                if (deviceText.isEmpty() || tagText.isEmpty() || distanceText.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Defina os três parâmetros.", Toast.LENGTH_LONG).show();
                } else {
                    int device = Integer.parseInt(deviceText);
                    String tag = tagText.substring(tagText.length() - 4);
                    byte[] tagBytes = hexStringToByteArray(tag);
                    Log.i(TAG, "byte[0]="+tagBytes[0]+" byte[1]="+tagBytes[1]);
                    int distance = Integer.parseInt(distanceText);
                    aura.sendTagIdDistance(device, tagBytes, distance, 600);
                    Toast.makeText(getApplicationContext(), "Fixed TAG " + tag, Toast.LENGTH_LONG).show();
                }
                fixTagTextView.setText("");
            } catch (Exception t) {
                Toast.makeText(getApplicationContext(), "Erro ao enviar comando Fix TAG", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleTx(View v) {
        if (mConnected) {
            try {
                int taxaRequisicao = Integer.parseInt(taxaReq);
                aura.solicitaDeviceInfo(100);
                if(taxaRequisicao==1300){
                    taxaRequisicao=0;
                }
                else if(taxaRequisicao==650){
                    taxaRequisicao=1;
                }
                else if(taxaRequisicao==380){
                    taxaRequisicao=2;
                }
                aura.sendTaxaRequisicao(taxaRequisicao, 600);
                Log.i(TAG, "taxaResiquisao="+taxaRequisicao);
                Toast.makeText(getApplicationContext(), "Taxa de requisição enviada", Toast.LENGTH_LONG).show();
            }catch (Exception t){
                Toast.makeText(getApplicationContext(), "Taxa de requisição vazia", Toast.LENGTH_LONG).show();
            }
            String dataConfig = null;
        } else
            Toast.makeText(getApplicationContext(), "Dispositivo Desconectado", Toast.LENGTH_SHORT).show();
    }

    public void handleD1(View v) {
        if (mConnected) {
            zona1TextView = findViewById(R.id.zona1_edit);
            int distanciaZona1;
            distanciaZona1 = Integer.parseInt(zona1TextView.getText().toString());

            if (distanciaZona1 < 100) {
                distanciaZona1 = 100;
                Toast.makeText(getApplicationContext(), "Zona 1 Inválida", Toast.LENGTH_LONG).show();
            }
            if (distanciaZona1 > 3000) {
                Toast.makeText(getApplicationContext(), "Zona 1 Inválida", Toast.LENGTH_LONG).show();
                distanciaZona1 = 3000;
            }
            // Distancia deve ser enviada em mm
            aura.sendDistanciaZona1(distanciaZona1 * 10, 400);
            Toast.makeText(getApplicationContext(), "Zona 1 Enviada", Toast.LENGTH_SHORT).show();
            String dataConfig = null;
        } else
            Toast.makeText(getApplicationContext(), "Dispositivo Desconectado", Toast.LENGTH_SHORT).show();
    }

    public void handleD2(View v) {
        if (mConnected) {
            //taxaReqTextView = findViewById(R.id.taxar_edit);
            zona2TextView = findViewById(R.id.zona2_edit);

            int distanciaZona2;
            //distanciaZona2 =Short.parseShort(zona2TextView.getText().toString(),10);
            distanciaZona2 = Integer.parseInt(zona2TextView.getText().toString());
            if (distanciaZona2 < 100) {
                Toast.makeText(getApplicationContext(), "Zona 2 Inválida", Toast.LENGTH_LONG).show();
                distanciaZona2 = 100;
            }
            if (distanciaZona2 > 3000) {
                Toast.makeText(getApplicationContext(), "Zona 2 Inválida", Toast.LENGTH_LONG).show();
                distanciaZona2 = 3000;
            }
            // Distancia deve ser enviada em mm
            aura.sendDistanciaZona2(distanciaZona2 * 10, 400);
            Toast.makeText(getApplicationContext(), "Zona 2 Enviada", Toast.LENGTH_SHORT).show();
            String dataConfig = null;

        } else
            Toast.makeText(getApplicationContext(), "Dispositivo Desconectado", Toast.LENGTH_SHORT).show();
    }

    public void handleOF(View v) {
        if (mConnected) {
            offsetTextView = findViewById(R.id.offset_edit);

            int offset;
            //distanciaZona2 =Short.parseShort(zona2TextView.getText().toString(),10);
            offset = Integer.parseInt( offsetTextView.getText().toString());
            if (offset < 100) {
                Toast.makeText(getApplicationContext(), "Offset Inválido", Toast.LENGTH_LONG).show();
                offset = 100;
            }
            if (offset > 3000) {
                Toast.makeText(getApplicationContext(), "Offset Inválido", Toast.LENGTH_LONG).show();
                offset = 3000;
            }
            // Distancia deve ser enviada em mm
            try {
                int per_int=Integer.parseInt(periferico);
                if (periferico != null) {
                    aura.sendOffsetCommand(Integer.parseInt(periferico), offset * 10, 100);
                    Toast.makeText(getApplicationContext(), "Periferico setado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Periferico vazio, operação inválida", Toast.LENGTH_SHORT).show();
                }
                String dataConfig = null;
            }catch (Exception o){
                Toast.makeText(getApplicationContext(), "Periferico vazio", Toast.LENGTH_SHORT).show();
            }

        } else
            Toast.makeText(getApplicationContext(), "Dispositivo Desconectado", Toast.LENGTH_SHORT).show();
    }

    public void handle(View v) {
      /*  taxaReqTextView=findViewById(R.id.taxar_edit);
        zona1TextView=findViewById(R.id.zona1_edit);
        zona2TextView=findViewById(R.id.zona2_edit);

        Log.d("handleEditText", "true");
        if(mBluetoothLeService!=null && mConnected==true){
            String input = mBluetoothLeService.getOriginalString();
            taxaReqTextView.setText(input.substring(0,4));
            zona1TextView.setText(input.substring(4,6));
            zona2TextView.setText(input.substring(6,8));
        }else{
            taxaReqTextView.setText("");
            zona1TextView.setText("");
            zona2TextView.setText("");
        }*/
    }

    //arrow que adiciona 1
    public void handleAdd1(View v) {
        zona1TextView = findViewById(R.id.zona1_edit);

        String inputZ1 = zona1TextView.getText().toString();
        int zona1value = Integer.parseInt(inputZ1);

        if (zona1value < 3000) zona1value += 50;
        else
            Toast.makeText(getApplicationContext(), "Operação Inválida", Toast.LENGTH_SHORT).show();
        zona1TextView.setText(String.valueOf(zona1value));
    }

    public void handleSub1(View v) {
        zona1TextView = findViewById(R.id.zona1_edit);

        String inputZ1 = zona1TextView.getText().toString();
        int zona1value = Integer.parseInt(inputZ1);

        if (zona1value > 100) zona1value -= 50;
        else
            Toast.makeText(getApplicationContext(), "Operação Inválida", Toast.LENGTH_SHORT).show();
        zona1TextView.setText(String.valueOf(zona1value));

    }

    public void handleAdd2(View v) {
        zona2TextView = findViewById(R.id.zona2_edit);

        String inputZ2 = zona2TextView.getText().toString();
        int zona2value = Integer.parseInt(inputZ2);

        if (zona2value < 3000) zona2value += 50;
        else
            Toast.makeText(getApplicationContext(), "Operação Inválida", Toast.LENGTH_SHORT).show();
        zona2TextView.setText(String.valueOf(zona2value));
    }

    public void handleSub2(View v) {
        zona2TextView = findViewById(R.id.zona2_edit);

        String inputZ2 = zona2TextView.getText().toString();
        int zona2value = Integer.parseInt(inputZ2);

        if (zona2value > 100) zona2value -= 50;
        else
            Toast.makeText(getApplicationContext(), "Operação Inválida", Toast.LENGTH_SHORT).show();
        zona2TextView.setText(String.valueOf(zona2value));
    }

    public void handleAdd3(View v) {
        offsetTextView = findViewById(R.id.offset_edit);

        String inputOF = offsetTextView.getText().toString();
        int offset_value = Integer.parseInt(inputOF);

        if (offset_value < 3000) offset_value += 50;
        else
            Toast.makeText(getApplicationContext(), "Operação Inválida", Toast.LENGTH_SHORT).show();
        offsetTextView.setText(String.valueOf(offset_value));
    }

    public void handleSub3(View v) {
        offsetTextView = findViewById(R.id.offset_edit);

        String inputTR = offsetTextView.getText().toString();
        int offset_value = Integer.parseInt(inputTR);

        if (offset_value > 100) offset_value -= 50;
        else
            Toast.makeText(getApplicationContext(), "Operação Inválida", Toast.LENGTH_SHORT).show();
        offsetTextView.setText(String.valueOf(offset_value));
    }

    private boolean isActived=true;

    public void spinnerTest(View v) {
        //Toast.makeText(getApplicationContext(), "handle spinner", Toast.LENGTH_SHORT).show();
        if(mConnected) {
            if (isActived) {
                aura.solicitaDeviceInfo(100);
                v.setBackground(getResources().getDrawable(R.drawable.image_button_selector));
                v.setBackground(getDrawable(R.color.colorAccent));
                isActived = false;
            } else {
                System.out.println(decaWaveIDList);
                decaWaveIDList = aura.getDecaWaveIDList();
                System.out.println("Spinner short list" + decaWaveIDList);
                List<String> listString = new ArrayList<>();
                listString.add("Selecione");
                for (Short s : decaWaveIDList) {
                    listString.add(s.toString());
                }
                offset_txt = findViewById(R.id.offset_txt);
                offsetList = aura.getOffsetList();
                System.out.println("Spinner string list " + listString);
                Spinner spinner = findViewById(R.id.spinner_pr);
                ArrayAdapter<String> SpinnerAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, listString);
                SpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(SpinnerAdapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        prTextView=findViewById(R.id.text_pr);
                        prTextView.setVisibility(View.VISIBLE);
                        String text = parent.getItemAtPosition(position).toString();
                        //Toast.makeText(getApplicationContext(), "Spinner " + text, Toast.LENGTH_SHORT).show();
                        periferico = text;
                        if(!text.contains("Selecione")){
                            prTextView.setText(text);
                            // pegar na posição -1 pois nessa lista na posição 0 tem uma string que não é um deca wave.
                            System.out.println("off set+ "+ shortToString(offsetList).get(position-1));
                            offset_txt.setText(shortToString(offsetList).get(position-1));
                        }
                        else prTextView.setText("");
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                v.setBackground(getDrawable(R.color.colorPrimary));
                isActived = true;
            }
        }else  Toast.makeText(getApplicationContext(), "Dispositivo Desconectado", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothLeService != null) {
            decaWaveIDList = aura.getDecaWaveIDList();
            String[] array = decaWaveIDList.toArray(new String[0]);
            Spinner spinner_pr = findViewById(R.id.spinner_pr);
            ArrayAdapter<CharSequence> SpinnerAdapter_pr = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, array);
            SpinnerAdapter_pr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_pr.setAdapter(SpinnerAdapter_pr);
            spinner_pr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(getApplicationContext(), "Spinner 2", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spinner_pr.setVisibility(View.VISIBLE);
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
       /* if (mBluetoothLeService != null) {
            //taxaReqTextView=findViewById(R.id.taxar_edit);
            zona1TextView= findViewById(R.id.zona1_edit);
            zona2TextView=findViewById(R.id.zona2_edit);

            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
            //colocar nos parenteses um input do que é pego pelo gatt
            String input = mBluetoothLeService.getOriginalString();
            taxaReqTextView.setText(input.substring(0,4));
            zona1TextView.setText(input.substring(4,6));
            zona2TextView.setText(input.substring(6,8));
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        bluetoothLeService = null;
    }
    private boolean mSoliciteInfoAtived=false;
    //parte do menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            if(mSoliciteInfoAtived) {
                menu.findItem(R.id.action_refresh).setVisible(true);
                menu.findItem(R.id.action_solicite).setVisible(false);
            }else{
                menu.findItem(R.id.action_refresh).setVisible(false);
                menu.findItem(R.id.action_solicite).setVisible(true);
            }
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.action_refresh).setVisible(false);
            menu.findItem(R.id.action_solicite).setVisible(false);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                bluetoothLeService.disconnect();
                break;
            case R.id.action_solicite:
                aura.solicitaDeviceInfo(100);
                mSoliciteInfoAtived=true;
                invalidateOptionsMenu();
                break;
            case R.id.action_refresh:
//                if (decaWaveIDList == null) decaWaveIDList = mBluetoothLeService.getDecaWaveIDList();
//                offsetList = mBluetoothLeService.getOffsetList();
                try {
                    farDistanceList = aura.getFarDistanceList();
                    nearDistanceList = aura.getNearDistanceList();

                    zona1_txt = findViewById(R.id.zona1_edit);
                    zona2_txt = findViewById(R.id.zona2_edit);
//                pr_txt = findViewById(R.id.pr_txt);
                    offset_txt = findViewById(R.id.offset_txt);
                    zona1_txt.setText(Integer.toString(nearDistanceList.get(0) / 10));
                    zona2_txt.setText(Integer.toString(farDistanceList.get(0) / 10));
                    invalidateOptionsMenu();
                    mSoliciteInfoAtived = false;
                }catch (Exception x){
                    x.toString();

                }
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    /*//parte do menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            if(mSoliciteInfoAtived) {
                menu.findItem(R.id.action_refresh).setVisible(true);
                menu.findItem(R.id.action_solicite).setVisible(false);
            }else{
                menu.findItem(R.id.action_refresh).setVisible(false);
                menu.findItem(R.id.action_solicite).setVisible(true);
            }
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.action_solicite).setVisible(false);
            menu.findItem(R.id.action_refresh).setVisible(false);
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }*/
    private List<String> shortToString(List<Short> origin){
        List<String> listString= new ArrayList<>();
        for (Short s : origin) {
            listString.add(origin.toString());
        }
        return listString;
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_solicite:
                mBluetoothLeService.solicitaDeviceInfo(100);
                mSoliciteInfoAtived=true;
                return true;
            case R.id.action_refresh:
                    *//*if (decaWaveIDList == null) decaWaveIDList = mBluetoothLeService.getDecaWaveIDList();
                    offsetList = mBluetoothLeService.getOffsetList();*//*
                    farDistanceList = mBluetoothLeService.getFarDistanceList();
                    nearDistanceList = mBluetoothLeService.getNearDistanceList();

                    zona1_txt = findViewById(R.id.zona1_txt);
                    zona2_txt = findViewById(R.id.zona2_txt);
                    *//*pr_txt = findViewById(R.id.pr_txt);
                    offset_txt = findViewById(R.id.offset_txt);*//*
                    zona1_txt.setText(shortToString(nearDistanceList).get(0));
                    zona2_txt.setText(shortToString(nearDistanceList).get(0));
                mSoliciteInfoAtived=false;
                return true;
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }*/


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
                if(mConnected){
                   reconectar.setVisibility((View.GONE));
                }
                else{
                    reconectar.setVisibility(View.VISIBLE);

                }
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
