package com.logpyx.auraconfig;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.logpyx.auraconfig.Adapter.RecyclerBleAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeviceScanActivity extends AppCompatActivity implements RecyclerViewInterface {

//    private BluetoothLeScanner btScanner;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;

    //private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private boolean advertising = false;
    private int PERMISSION = 255;
    private boolean scanning;

    //parte recycle
    private ArrayList<BluetoothDevice> mLeDevices;
    private RecyclerBleAdapter mLeDeviceRecycleAdapter;
    private RecyclerView rvBles;
    //parte de gatt service
    private BluetoothGattServer bluetoothGattServer;
    public static final String TAG = "device_scan_activity";

    private ScanResult result;
    public void setResult(ScanResult result){
        this.result=result;
    }
    //parte de filtro de por nome

    private TextView filtro;
    private String inputFiltro;

    @SuppressLint("MissingPermission")
    public void handleSearch(View v){
        filtro=findViewById(R.id.filtro_edit);
        inputFiltro=filtro.getText().toString();
        filtro.setText(inputFiltro);
    }
    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            //Log.i(TAG, "onScanResult: ");
            super.onScanResult(callbackType, result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.i(TAG, "BLUETOOTH_CONNECT = " + String.valueOf(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)));
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        result.getScanRecord().getServiceUuids();
                    }
                    if(inputFiltro != null && inputFiltro.length() > 0){
                        mLeDeviceRecycleAdapter.addDevice(result.getDevice(), inputFiltro);
                    }
                    else {
                        mLeDeviceRecycleAdapter.addDevice(result.getDevice());
                    }
                    setResult(result);
                    mLeDeviceRecycleAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(TAG, "onBatchScanResults: ");
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "onScanFailed: ");
            super.onScanFailed(errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setIcon(R.drawable.toolbarimage);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Este dispositivo não suporta Bluetooth LE", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "onCreate: nao suporta");
            //finish();
        }
        //parte de permissões
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "ACCESS_COARSE_LOCATION = " + String.valueOf(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)));
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "ACCESS_FINE_LOCATION = " + String.valueOf(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)));
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG, "onCreate: outro if");
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.i(TAG, "onCreate: outro if 2");
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        filtro=findViewById(R.id.filtro_edit);
        //Parte de implementação do recycle view
        rvBles = findViewById(R.id.recycle_view_main);
        mLeDevices = new ArrayList<>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!scanning) {
            menu.findItem(R.id.action_refresh).setVisible(true);
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        } else {
            menu.findItem(R.id.action_refresh).setVisible(true);
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        }
        return true;
    }
//    SwipeRefreshLayout mySwipeRefreshLayout;
//    private void myUpdateOperation(){
//
//    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                Log.i(TAG, "onOptionsItemSelected: ");
                mLeDeviceRecycleAdapter.clear();
                startStopOperations(true);
                Toast.makeText(this, "Scan Selecionado", Toast.LENGTH_SHORT).show();
                if(inputFiltro!=null)Toast.makeText(this, "Procurando "+inputFiltro+" dispositivos", Toast.LENGTH_SHORT).show();
                else Toast.makeText(this, "Procurando todos dispositivos", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_stop:
                startStopOperations(false);
                Toast.makeText(this, "Stop Selecionado", Toast.LENGTH_SHORT).show();
                break;
//            case R.id.action_refresh:
//                mySwipeRefreshLayout.setRefreshing(true);
//                myUpdateOperation();
//                Toast.makeText(this, "Refresh Selecionado", Toast.LENGTH_SHORT).show();
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startStopOperations(boolean enable) {
        Log.i(TAG, "startStopOperations: enable " + enable);
        scanLeDevice(enable);
        // Função do celular não é ble server, logo não é necessário um advertising
        //advertise(enable);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //scanning until stopped
            Log.i(TAG, "BLUETOOTH_SCAN = " + String.valueOf(this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                 // mBluetoothAdapter.getBluetoothLeScanner().startScan(buildScanFilters(), buildScanSettings(), leScanCallback);
                mBluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
                scanning = true;
            }
            //invalidateOptionsMenu();
            //scanning = true;
            } else {
                scanning = false;
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
            }
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION);
        }

        rvBles.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        mLeDeviceRecycleAdapter = new RecyclerBleAdapter(getBaseContext(), mLeDevices, this);
        rvBles.setAdapter(mLeDeviceRecycleAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLeDeviceRecycleAdapter.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getBaseContext());
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //parte de clicar na lista e ir pra tela de configuração
    @Override
    public void onItemClick(int position) {
        final BluetoothDevice device= mLeDevices.get(position);
        if(device==null) return;
        final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
           // intent.putExtra("device_name", mLeDevices.get(position).getName());
           // intent.putExtra("device_address", mLeDevices.get(position).getAddress());
            if(device.getName()!=null)intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
            else intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, "Unknown");
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS,device.getAddress());
            if(scanning){
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
                scanning=false;
            }
        }
        startActivity(intent);
        invalidateOptionsMenu();

    }
//    final byte[] setServiceData = new byte[]{
//            0x11,0x50, 0x64
//    };
/*
    //parte de filtro dispositivo
    private List<ScanFilter> buildScanFilters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        // parte que estava dando problema com a caracteristica que o fabiano estava passando
        builder.setServiceUuid(new ParcelUuid(UUID.fromString("a6530bf2-d97e-478f-8814-78549e53f0be")));
        ScanFilter build = builder.build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(build);
        return filters;

    }
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        return builder.build();
    }
*/

    //parte de gatt service?
    private void advertise(final boolean enable) {

        if (enable && mBluetoothAdapter.isMultipleAdvertisementSupported() && !advertising) {
            Log.i(TAG, "advertise: advertising");

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothGattServer = bluetoothManager.openGattServer(this, bluetoothGattServerCallback);
            }
            if (bluetoothGattServer.getService(SampleGattAttributes.SERVICE_UUID) == null) {
                BluetoothGattService service = new BluetoothGattService(SampleGattAttributes.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                BluetoothGattCharacteristic bluetoothGattCharacteristic = new BluetoothGattCharacteristic(SampleGattAttributes.CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
                );
                bluetoothGattCharacteristic.setValue(mBluetoothAdapter.getName());
                bluetoothGattCharacteristic.addDescriptor(new BluetoothGattDescriptor(SampleGattAttributes.BLE_NOTIFICATION, BluetoothGattCharacteristic.PERMISSION_WRITE));
                service.addCharacteristic(bluetoothGattCharacteristic);
                bluetoothGattServer.addService(service);
            }
            mBluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(buildSettings(),
                    buildData(), advertiseCallback);
        } else if (!enable && mBluetoothAdapter.isMultipleAdvertisementSupported() && advertising) {
            List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            for (BluetoothDevice connectedDevice : connectedDevices) {
                bluetoothGattServer.cancelConnection(connectedDevice);
            }
            List<BluetoothDevice> connectedDevices2 = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
            for (BluetoothDevice connectedDevice : connectedDevices2) {
                bluetoothGattServer.cancelConnection(connectedDevice);
            }
            mBluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(advertiseCallback);
        }
    }

    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.i(TAG, "onConnectionStateChange: gattserver");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("GATT_SERVER", "onConnectionStateChange: CONNECTED status " + newState);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    bluetoothGattServer.connect(device, false);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("GATT_SERVER", "onConnectionStateChange: DISCONNECTED status " + newState);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }
    };
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            advertising = true;
            Log.i(TAG, "onStartSuccess: Advertising " + advertising);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            advertising = false;
            Log.i(TAG, "onStartFailure: Advertising");
        }
    };

    private AdvertiseData buildData() {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.addServiceData(new ParcelUuid(UUID.fromString("d71dbc8d-f13c-014d-2a57-95f5b0eb4b30")), "2413843681".getBytes());
        return builder.build();
    }

    private AdvertiseSettings buildSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(true);
        return builder.build();
    }
}
