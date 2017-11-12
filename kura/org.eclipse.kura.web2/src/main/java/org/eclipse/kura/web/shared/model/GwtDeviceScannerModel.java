package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtDeviceScannerModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private String macAddr;
    private String deviceName;
    private short RSSI;
    private short timeStamp;

    public GwtDeviceScannerModel() {
        super();
    }

    public GwtDeviceScannerModel(String macAddr, String deviceName, short rSSI, short timeStamp) {
        super();
        this.macAddr = macAddr;
        this.deviceName = deviceName;
        this.RSSI = rSSI;
        this.timeStamp = timeStamp;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public short getRSSI() {
        return RSSI;
    }

    public void setRSSI(short rSSI) {
        RSSI = rSSI;
    }

    public short getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(short timeStamp) {
        this.timeStamp = timeStamp;
    }

}
