package com.logpyx.auraconfig;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Pager extends Protocol implements ReceiveInterface{
    public List<Short> decaWaveIDList = new ArrayList<>();
    public List<Short> offsetList = new ArrayList<>();
    public List<Short> nearDistanceList = new ArrayList<>();
    public List<Short> farDistanceList = new ArrayList<>();

    private Handler handler = new Handler();

    public Pager(SendInterface sendInterface){
        super(sendInterface);
    }

    @Override
    void processCommand(int[] DataBuffer) {
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
                //sendCommand((byte)SampleGattAttributes.WR_SET_OVERCRANE_PARAM,data,12);
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
