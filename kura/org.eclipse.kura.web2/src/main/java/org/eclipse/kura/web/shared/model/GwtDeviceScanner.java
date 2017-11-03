package org.eclipse.kura.web.shared.model;

public class GwtDeviceScanner {

    private String macAddr;
    private String deviceName;
    private String timeStamp;
    private String DataType;

    public GwtDeviceScanner() {
        super();
    }

    public GwtDeviceScanner(String macAddr, String deviceName, String timeStamp, String dataType) {
        super();
        this.macAddr = macAddr;
        this.deviceName = deviceName;
        this.timeStamp = timeStamp;
        DataType = dataType;
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

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getDataType() {
        return DataType;
    }

    public void setDataType(String dataType) {
        DataType = dataType;
    }

}
