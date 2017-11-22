package org.eclipse.kura.web.server;

import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import org.eclipse.kura.web.server.ublox.BluetoothAtCommand;
import org.eclipse.kura.web.server.ublox.BluetoothLeAdapterImpl;
import org.eclipse.kura.web.server.ublox.BluetoothLeDeviceImpl;
import org.eclipse.kura.web.shared.model.GwtDeviceScannerModel;

import asg.cliche.Command;
import asg.cliche.Param;

public class Ublox {

    // private static final Logger LOG = Logger.getLogger(Main.class);
    private static BluetoothAtCommand at = new BluetoothAtCommand();
    // private static UBloxSerial serial = new UBloxSerial();
    private static BluetoothLeAdapterImpl adap = new BluetoothLeAdapterImpl();
    private static BluetoothLeDeviceImpl device = new BluetoothLeDeviceImpl();
    public HashSet<GwtDeviceScannerModel> listDevice = new HashSet<GwtDeviceScannerModel>();

    public Ublox(int maxScan) {
        activate();
        scan(12000);
    }

    public String activate() {
        at.connectSerialPort();
        System.out.println("helloWolrd2!");
        return "connecting";
    }

    public String activate(@Param(description = "port name", name = "ttymxc2") String portName,
            @Param(description = "baude Rate", name = "115200") String baudRate) {
        try {
            at.connectSerialPort(portName, Integer.valueOf(baudRate));
            // serial.connect(portName, Integer.valueOf(baudRate));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "connecting";
    }

    @Command(description = "disconnect the serie port")
    public String desactivate() {
        at.disconnectSerialPort();
        return "Disconnecting";
    }

    // @Command(description = "Scan for bluetooth devices")
    public String scan(long time) {

        for (BluetoothLeDeviceImpl device : adap.findDevice(time).values()) {
            this.listDevice.add(new GwtDeviceScannerModel(device.getAddress(), device.getName(), device.getRSSI(),
                    String.valueOf(new Date())));
        }
        return "Scanning";
    }

    @Command(description = "send AT command to bluetooth devices")
    public String send(@Param(description = "commad name", name = "command") String ATCommand,
            @Param(description = "second parameter", name = "parameter") String param2) {
        try {
            // serial.sendText("AT"+ATCommand+"="+param2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "sending";
    }

    @Command(description = "connect to bluetooth devices")
    public String connect(@Param(description = "mac address", name = " bd addr") String bd_addr) {
        at.connectToBleDevice(bd_addr);
        return "connect";
    }

    @Command(description = "disconnect to bluetooth devices")
    public String disconnect() {
        at.disconnectToBleDevice();
        return "disconnect";
    }

    @Command(description = "")
    public String getAddressAdapter() {
        // at.getAdapterMacAddress();
        return adap.getAddress();
    }

    @Command(description = "")
    public String getNameAdapter() {
        return adap.getName();
    }

    @Command(description = "")
    public String getInterfaceAdapter() {
        return adap.getInterfaceName();
    }

    @Command(description = "")
    public short getRssi(String bd_addr) {
        // at.getListUuid();
        at.getRssiConnection(bd_addr);
        return device.getRSSI();
    }

    /**
     * Return conn_handle (type integer),start (type integer), end (type integer), uuid (type hex string)
     *
     * @param conn_handle
     *
     * @return
     * @throws Exception
     */
    @Command(description = "this responce is sent for every service found")
    public String getServices() throws Exception {
        device.findServices();
        return "arrylist";
    }

    @Command(description = "Check if the device is connected.")
    public String checkConnection() {
        return null;
    }

    /**
     * Return a GATT service based on a UUID.
     *
     * @param uuid
     *            UUID of service
     * @return BluetoothGattService
     * @throws Exception
     */
    @Command(description = "Return a GATT service based on a UUID.")
    public String getServicesByUuid(
            @Param(description = "UUID of service. this can either be 16bit or 128bit", name = "UUID") UUID uuid)
            throws Exception {
        device.findServiceByUuid(uuid);
        return "services by UUID :" + uuid;
    }

    /*
     * @Command(description="Get a list of GATT services offered by the device.")
     * public String getServices(
     * 
     * @Param(description="start handle of service", name = "start")
     * int start,
     * 
     * @Param(description="end", name = "end")
     * int end) {
     * at.getServicesByStartEnd(start, end);
     * return null;
     * }
     * 
     */
    /**
     * Return conn_handle (type integer),attr_handle (type integer),propreties (byte array)
     * value handle (type integer), uuid (type hex string)
     * 
     * @param start
     * @param end
     *
     * @return
     */
    @Command(description = "Get a list of GATT characteristics based on start and end handles. Handle boundaries")
    public String getCharacteristicsOfService(
            // @Param(description="GAT handle of the connected device", name="conn handle")
            // String conn_handle,
            @Param(description = "start handle of service", name = "start") int start,
            @Param(description = "end handle of service", name = "end") int end) {
        device.getCharacteristicOfServices(start, end);
        return null;
    }

    @Command(description = "Get a list of GATT characteristics based on start and end handles. Handle boundaries")
    public String getCharacteristicsDescriptor(
            // @Param(description="GAT handle of the connected device", name="conn handle")
            // String conn_handle,
            @Param(description = "attribute handle of the caracteristic value", name = "handle value") int value_handle,
            @Param(description = "end handle of the service to which the caracteristic belongs", name = "end handle") int service_end_handle) {
        device.getCharacteristicDescriptor(value_handle, service_end_handle);
        return null;
    }

    @Command(description = "Read characteristic value from handle.")
    public String readCharacteristicOfValue(
            @Param(description = "attribute handle of the caracteristic value", name = "start") int start,
            @Param(description = "GAT handle of the connected device", name = "suuid") int suuid) {
        at.readCharacteristicsValueByUuid(start, suuid);
        return "value has been written";
    }

    @Command(description = "Read characteristic value from handle.")
    public String readCharacteristics(
            @Param(description = "attribute handle of the caracteristic value", name = "handle value") int value_handle) {
        at.readCharacteristicsValue(value_handle);
        return "value has been written";
    }

    @Command(description = "Read value from characteristic by UUID.")
    public String readCharacteristicValueByUuid(
            // @Param(description="GAT handle of the connected device", name="conn handle")
            // String conn_handle,
            @Param(description = "start", name = "start") int start,
            @Param(description = "UUID", name = "UUID") int suuid) {
        at.readCharacteristicsValueByUuid(start, suuid);
        return null;
    }

    @Command(description = "Write client characteristic configuration .")
    public String writeCharacteristicsValue(
            @Param(description = "handle of the descriptor", name = "desc handle") String desc_handle,
            @Param(description = "client configuration", name = "config") String config) {
        at.writeCharacteristicsValue(desc_handle, config);

        return "Value has been written";
    }

    @Command(description = "Write characteristic with no reponse .")
    public String writeCharacteristicsValueNoRes(
            @Param(description = "attribute handle of the caracteristic value", name = "handle value") String value_handle,
            @Param(description = "the data as hex string", name = "hex_data") String hex_data) {
        at.writeCharacteristicValueNoRes(value_handle, hex_data);
        return "Value has been written";
    }

    /**
     * Return
     *
     * @param reliable
     *            send the data as reliable or not. if you use reliable, the returned data will be verified
     *            0 not reliable
     *            1 reliable
     * @param flag
     * @offset
     * @return
     */
    public String writeLongCharacteristicValue(
            @Param(description = "attribute handle of the caracteristic value", name = "handle value") int value_handle,
            @Param(description = "the data as hex string", name = "hex_data") String hex_data,
            @Param(description = "data as reliable or not", name = "reliable") String reliable,
            @Param(description = "flag used when sending several packets", name = "flag") int flag,
            @Param(description = "optional offset of the data to write (by default 0)", name = "offset") String offset) {
        at.writeLongCharacteristicValue(value_handle, hex_data, reliable, flag, offset);
        return "Value has been written";
    }

    public String notification(
            @Param(description = "attribute handle of the caracteristic value", name = "handle value") int value_handle,
            @Param(description = "the data as hex string", name = "hex_data") String hex_data) {
        at.notification(value_handle, hex_data);
        return null;
    }

    public String indication(
            @Param(description = "attribute handle of the caracteristic value", name = "handle value") int value_handle,
            @Param(description = "the data as hex string", name = "hex_data") String hex_data) {
        at.indication(value_handle, hex_data);
        return null;
    }

}
