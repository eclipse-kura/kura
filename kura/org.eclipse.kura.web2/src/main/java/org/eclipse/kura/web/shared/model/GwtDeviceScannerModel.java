package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtDeviceScannerModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private String macAddr;
    private String deviceName;
    private short RSSI;
    private String timeStamp;

    public GwtDeviceScannerModel() {
        super();
    }

    public String getMacAddr() {
        return macAddr;
    }

    public GwtDeviceScannerModel(String macAddr, String deviceName, short rSSI, String timeStamp) {
        super();
        this.macAddr = macAddr;
        this.deviceName = deviceName;
        this.RSSI = rSSI;
        this.timeStamp = timeStamp;
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

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

}
