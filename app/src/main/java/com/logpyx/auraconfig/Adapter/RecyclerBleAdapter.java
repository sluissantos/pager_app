package com.logpyx.auraconfig.Adapter;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import androidx.recyclerview.widget.RecyclerView;

import com.logpyx.auraconfig.RecyclerViewInterface;
import com.logpyx.auraconfig.R;

import java.text.Normalizer;
import java.util.ArrayList;

public class RecyclerBleAdapter extends RecyclerView.Adapter<RecyclerBleAdapter.MyViewHolder> {
    //parte de click
    private final RecyclerViewInterface recyclerViewInterface;
    //instanciando como variavel local
    private final ArrayList<BluetoothDevice> mLeDevices;
    //implementando partes do código da iza Recycler FUll Checklist Adapter
    private final Context context;

    //estava recendo só o arraylist, mas no dia 20/12 implementei usando um código da iza pegando o context
    public RecyclerBleAdapter(Context context, ArrayList<BluetoothDevice> mLeDevices, RecyclerViewInterface recyclerViewInterface) {
        this.mLeDevices = mLeDevices;
        this.context = context;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public RecyclerBleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // v é igual ao card view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new RecyclerBleAdapter.MyViewHolder(v, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.deviceName.setText(mLeDevices.get(position).getName());
        BluetoothDevice device = mLeDevices.get(position);
        //coloquei uma variavel context mas deveria ser  if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                holder.deviceName.setText(deviceName);
            } else holder.deviceName.setText(R.string.name_unk);
            holder.deviceAddress.setText(device.getAddress());
        }

        holder.bind(mLeDevices.get(position));
    }

    @Override
    public int getItemCount() {
        return mLeDevices.size();
    }

    public void clear() {
        int size = getItemCount();
        mLeDevices.clear();
        notifyItemRangeRemoved(0, size);
    }

    private String getOnlyNumericDigitis(String value) {
        if (value == null) return null;
        String s = Normalizer.normalize(value, Normalizer.Form.NFD);
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    public void addDevice(BluetoothDevice device, String filtroName) {
        if (!mLeDevices.contains(device)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if(filtroName != null && filtroName.length() > 0 &&  device.getName()!= null && device.getName().length() > 0) {
                    //parte de filtrar nome digitado
                    if (device.getName().toLowerCase().contains(filtroName.toLowerCase())) {
                        mLeDevices.add(device);
                        notifyItemInserted(mLeDevices.size());
                    }
                    //parte de filtrar pelo MAC/ ADDRESS
                    else if(getOnlyNumericDigitis(device.getAddress()).toLowerCase().contains(getOnlyNumericDigitis(filtroName.toLowerCase()))){
                        mLeDevices.add(device);
                        notifyItemInserted(mLeDevices.size());
                    }
                }
                if(filtroName != null && filtroName.length() > 0 &&  device.getName()== null){
                    if(getOnlyNumericDigitis(device.getAddress()).toLowerCase().contains(getOnlyNumericDigitis(filtroName.toLowerCase()))){
                        mLeDevices.add(device);
                        notifyItemInserted(mLeDevices.size());
                    }
                }
            }
        }
    }
    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
            notifyItemInserted(mLeDevices.size());
        }
    }
    public static class MyViewHolder extends  RecyclerView.ViewHolder{
        TextView deviceName;
        TextView deviceAddress;
        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
            //parte do click no holder
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recyclerViewInterface !=null){
                        int pos  = getAdapterPosition();
                        if(pos!= RecyclerView.NO_POSITION){
                            recyclerViewInterface.onItemClick(pos);
                        }
                    }
                }
            });
        }

        //faz atribuição
        void bind(BluetoothDevice bluetoothDevice) {
            deviceName.setText(bluetoothDevice.getName());
            deviceAddress.setText(bluetoothDevice.getAddress());
        }
    }
}
