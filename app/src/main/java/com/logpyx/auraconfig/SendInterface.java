package com.logpyx.auraconfig;

public interface SendInterface {

    public int sendData(byte[] data);
    
    public void setReceiveInterface(ReceiveInterface receiveInterface);

}
