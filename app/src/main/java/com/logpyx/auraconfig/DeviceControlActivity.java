package com.logpyx.auraconfig;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    // Code to manage Service lifecycle.
    public Pager pager;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        //parte de conectar com um dispositivo, usa o mDeviceAddress
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothLeService != null) {
                if (!bluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                bluetoothLeService.connect(mDeviceAddress);
                pager = new Pager((SendInterface) bluetoothLeService);
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

    private TextView fileDirTextView;

    private Button reconectar;

    private Button selectFileButton;

    private Button startButton;

    private boolean mSoliciteInfoAtived=false;

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

        startButton = findViewById(R.id.startButton);

        fileDirTextView = findViewById(R.id.fileDirText);
        reconectar = findViewById(R.id.reconnectButton);
        selectFileButton = findViewById(R.id.selectFileButton);
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOpenFileExplorer();
            }
        });

        reconectar.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> SpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.spinner_txt, android.R.layout.simple_spinner_item);
        SpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    public void handleReconnect(View view){
        if(!mConnected && bluetoothLeService != null){
            bluetoothLeService.connect(mDeviceAddress);
        }
    }

    private static final int READ_REQUEST_CODE = 42; // código de solicitação personalizado

    public void handleOpenFileExplorer() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Define o tipo de arquivo que você deseja buscar
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                String fileName = getFileName(uri);
                String directory = getDirectoryPath(uri);
                Log.d("File Info", "Directory: " + directory + ", Name: " + fileName);
                fileDirTextView.setText(fileName);
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder stringBuilder = new StringBuilder();
                        int lineCount = 0;
                        String line;
                        while ((line = reader.readLine()) != null && lineCount < 5) {
                            stringBuilder.append(line);
                            stringBuilder.append("\n");
                            lineCount++;
                        }
                        String content = stringBuilder.toString();
                        Log.d("File Content", content);
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getDirectoryPath(Uri uri) {
        String path = uri.getPath();
        int index = path.lastIndexOf("/");
        return path.substring(0, index);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothLeService != null) {
            decaWaveIDList = pager.getDecaWaveIDList();
            String[] array = decaWaveIDList.toArray(new String[0]);
            ArrayAdapter<CharSequence> SpinnerAdapter_pr = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, array);
            SpinnerAdapter_pr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
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

    public void handleStartOtaPager(View view){
        if (mConnected){
            bluetoothLeService.readFreeSpaceInfo(0);
        }
    }

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
                mSoliciteInfoAtived=true;
                invalidateOptionsMenu();
                break;
            case R.id.action_refresh:
//                if (decaWaveIDList == null) decaWaveIDList = mBluetoothLeService.getDecaWaveIDList();
//                offsetList = mBluetoothLeService.getOffsetList();
                try {
                    farDistanceList = pager.getFarDistanceList();
                    nearDistanceList = pager.getNearDistanceList();
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
