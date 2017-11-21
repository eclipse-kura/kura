package org.eclipse.kura.web.server.ublox;
/* ** class name : BluetoothAtComand
 * this class contains a set AT command who will be to send to serial port. the purpose have to communicate with
 * ublox module.
 * @author NarcisseKAPDJOU
 * @version 1.0.0
 */

import java.util.UUID;

import org.apache.log4j.Logger;

public class BluetoothAtCommand {

    private final Logger logger = Logger.getLogger(BluetoothAtCommand.class);
    private UBloxSerial serial = new UBloxSerial();
    private int conn_handle = 0; // GAT handle of the connected device
    private String path = new String("/dev/ttymxc2");
    private int serialBaudRate = 115200;
    private int protocol_type = 0; // GATT client LE
    public String adapterName = null;
    public String macAddress = null;
    public short rssi = 0;
    public long Atscan_timeout = 0; // time out of the discovery

    /**
    *
    */
    public BluetoothAtCommand() {
        // TODO Auto-generated constructor stub
    }

    /**
     * connect the serial port
     *
     */
    public void connectSerialPort() {
        try {
            serial.connect(path, serialBaudRate);
            logger.info("connect Serial port");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("serial connection failled", e);
        }

    }

    /**
     * connect the serial port
     *
     */
    public void connectSerialPort(String path, int serialBaudRate) {
        try {
            serial.connect(path, serialBaudRate);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("serial connection failled", e);
        }

    }

    /**
     * disconnect the serial port
     *
     */
    public void disconnectSerialPort() {
        try {
            serial.disconnect();
        } catch (Exception e) {
            logger.error("failled while to disconnect serial port", e);
        }

    }

    /**
     * Performs an inquiry procedure to find any scanner devices in the vicinity
     *
     * @param interfaceName
     *            name of the interface adapter
     *
     * @return +UBTD: bd_addr,rssi,device_name,data_type,data
     */

    public void scanBle(String interfaceName) {
        try {
            logger.info("scanBle...");
            serial.getUBloxCommand().setScanning(true); // to send of the UbloxCommand to create a list map

            serial.sendText("AT+UBTD=4,1"); // send AT command to start discovery device
            long start = System.currentTimeMillis();

            while (serial.getUBloxCommand().getScanningEnd() != true
                    && Atscan_timeout < BluetoothLeAdapterImpl.getScanTimeout()) // when the discovery device is in
            // progress and we don't have
            // received
            // the "OK" after the timer so we
            // can know who there is a error.
            {
                Thread.sleep(100);
                Atscan_timeout = System.currentTimeMillis() - start;
            }

        } catch (Exception e) {
            logger.error("error while to discovery Ble", e);
        }

    }

    /**
     * Reads the GAP discoverability mode
     *
     * @param
     *
     * @return +UTDM:discoverability_mode
     *         1 : GAP non-discoverable mode
     *         2 : GAP limited discoverable mode
     *         3 : default GAP general discoverable mode
     */

    public void getdiscoverabilityMode() {
        try {
            serial.sendText("AT+UBTDM?");
        } catch (Exception e) {
            logger.error("error while to get discoverability mode", e);
        }

    }

    /**
     * Reads the GAP connectability mode
     *
     * @param
     *
     * @return +UBTCM:connectability_mode
     *         1 : GAP non-connectable mode
     *         2 : default GAP connectable mode
     */

    public void getconnectabilityMode() {
        try {
            serial.sendText("AT+UBTCM?");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Make an ACL connection to a remote device defined with defined protocol type. unsolicited events
     * +UUBACLC or +UBTACLD will be sent out to confirm the connection establishment
     *
     * @param bd_addr
     *            Bluetooth Device address of the device to connect
     * @return OK
     */

    public void connectToBleDevice(String bd_addr) {
        try {
            serial.sendText("AT+UBTACLC=" + bd_addr + "," + protocol_type);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Close an existing ACL connection
     *
     * @param
     * @return OK
     */
    public void disconnectToBleDevice() {

        try {
            serial.sendText("AT+UBTACLD=" + conn_handle);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Reads the local address of the interface id
     *
     */
    public String getAdapterMacAddress() {
        try {
            serial.sendText("AT+UMLA=1");
            Thread.sleep(500);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return macAddress;
    }

    /**
     * Reads the local Bluetooth device name
     *
     * @param
     * @return device_name
     */
    public String getAdapterName() {
        try {
            serial.sendText("AT+UBTLN?");
            Thread.sleep(500);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return adapterName;
    }

    /**
     * GATT : Discovers all primary services on the remote device
     *
     * @param conn_handle
     *
     * @return conn_handle (type integer),start (type integer), end (type integer), uuid (type hex string)
     */

    public UUID[] getListServices() {

        UUID[] list_uuids = null;
        try {
            serial.getUBloxCommand().SetAskListService(true);
            serial.sendText("AT+UBTGDP=" + conn_handle);
        } catch (Exception e) {
            // TODO Auto-generated catch block

        }
        return list_uuids;
    }

    /**
     * Return a GATT service based on a UUID.
     *
     * @param uuid
     *            UUID of service
     * @return BluetoothGattService
     */
    public void getListServicesByUuid(UUID uuid) {
        try {
            serial.getUBloxCommand().SetAskListServiceByUuid(true);
            serial.sendText("AT+UBTGDPU=" + conn_handle + "," + uuid);

        } catch (Exception e) {
            // TODO Auto-generated catch block

        }

    }

    /**
     * Return the current received signal strength RSSI for a specified Bluetooth connection
     *
     * @param bd_addr
     *            address of the remote device
     * @return rssi
     */
    public short getRssiConnection(String bd_addr) {
        try {
            serial.sendText("AT+UBTRSS=" + bd_addr);

        } catch (Exception e) {
            // TODO Auto-generated catch block
        }
        return rssi;
    }

    /**
     * configures and sets up a service
     *
     * @param suuid
     *            uuid of service
     *
     * @return ser_handle (handle of the created service
     */
    public void setUpService(int suuid) {
        try {
            serial.sendText("AT+UBTGSER=" + suuid);

        } catch (Exception e) {
            // TODO Auto-generated catch block

        }
    }

    /**
     * Get a list of GATT services offered by the device
     *
     * @param start
     *            start handle of service
     * @param end
     *            end handle of service
     *
     * @return a list of GATT services
     */
    public void getServicesByStartEnd(int start, int end) {
        try {
            serial.sendText("AT+UBTGFI=" + conn_handle + "," + start + "," + end);

        } catch (Exception e) {
            // TODO Auto-generated catch block

        }

    }

    /**
     * search for services on a remote device
     *
     * @param bd_addr
     *
     * @param type
     *            enumerator: 0 serial port profile
     *            1 dial-up Networking profile
     *            2 uuid(iPhone)
     *            3 uuid(android)
     *            4 device id
     *
     * @param uuid
     * @return
     */
    public void ServiceSearch(String bd_addr, int type, int uuid) {
        try {
            serial.sendText("AT+UBTSS=" + bd_addr + "," + type + "," + uuid);

        } catch (Exception e) {
            // TODO Auto-generated catch block

        }

    }

    /**
     * Get a list of GATT characteristics based on start and end handles. Handle boundaries
     *
     * @param start
     *            start handle of service
     * @param end
     *            end handle of services
     *
     * @return conn_handle (type integer),attr_handle (type integer),propreties (byte array), value handle (type
     *         integer), uuid (type hex string)
     */
    public void getCaracteristiquesServices(int start, int end) {
        try {
            serial.sendText("AT+UBTGDCS=" + conn_handle + "," + start + "," + end);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Get a list of GATT characteristics of values descriptors
     *
     * @param value_handle
     *            handle of the characteristic value
     * @param service_end_handle
     *            end handle of the service to which the characteristic belongs
     *
     *
     * @return conn_handle (type integer),char_handle (type integer),descriptor_handle (type integer), value handle
     *         (type int), uuid (type hex string)
     */
    public void getCharacteristicsDescriptor(int value_handle, int service_end_handle) {
        try {
            serial.sendText("AT+UBTGDCD=" + conn_handle + "," + value_handle + "," + service_end_handle);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads the characteristic all bytes included
     *
     * @param value_handle
     *            handle of the characteristic value
     *
     * @return conn_handle (type integer),char_handle (type integer), hexa_data (type string)
     */
    public void readCharacteristicsValue(int value_handle) {

        try {
            serial.sendText("AT+UBTGR=" + conn_handle + "," + value_handle);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Read value from characteristic by UUID
     *
     * @param value_handle
     *            handle of the characteristic value
     *
     * @return conn_handle (type integer),char_handle (type integer), hexa_data (type string)
     */
    public void readCharacteristicsValueByUuid(int start, int suuid) {

        try {
            serial.sendText("AT+UBTGRU=" + conn_handle + "," + start + "," + suuid);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Write the client characteristic configuration
     *
     * @param desc_handle
     *            handle of the descriptor
     * @param config
     *            client configuration
     *
     * @return
     */
    public void writeCharacteristicsValue(String desc_handle, String config) {

        try {
            serial.sendText("AT+UBTGWC=" + conn_handle + "," + desc_handle + "," + config);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Write characteristic with no response
     *
     * @param value_handle
     *            handle of the characteristic value
     * @param hex_data
     *            the data as hex string
     *
     * @return
     */
    public void writeCharacteristicValueNoRes(String value_handle, String hex_data) {
        try {
            serial.sendText("AT+UBTGWN=" + conn_handle + "," + value_handle + "," + hex_data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * write long characteristic. this used to write a characteristic longer than 20 bytes or whenever a reliable write
     *
     * @param reliable
     *            send the data as reliable or not. if you use reliable, the returned data will be verified
     *            0 not reliable
     *            1 reliable
     * @param flag
     *            optional flag that is used when sending several packets or when the data is cancelled. if you send
     *            several
     *            packets, all but the last packet should set the flag to more data. the last data packet should set the
     *            flag to final
     *            0 =>final data(default), 1 =>more data, 2=> cancel data writing
     * @param offset
     * @return
     */
    public void writeLongCharacteristicValue(int value_handle, String hex_data, String reliable, int flag,
            String offset) {

        try {
            serial.sendText("AT+UBTGWL=" + conn_handle + "," + value_handle + "," + hex_data + "," + reliable + ","
                    + flag + "," + offset);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * this event is received when the remote side sends a notification.
     *
     * @param value_handle
     *            handle of the characteristic value
     * @param hex_data
     *            the data of the hex string
     * @return
     */
    public void notification(int value_handle, String hex_data) {

        try {
            serial.sendText("AT+UUBTGN=" + conn_handle + "," + value_handle + "," + hex_data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * this event is received when the remote side sends an indication.
     *
     * @param value_handle
     *            handle of the characteristic value
     * @param hex_data
     *            the data of the hex string
     * @return
     */
    public void indication(int value_handle, String hex_data) {

        try {
            serial.sendText("AT+UUBTGI=" + conn_handle + "," + value_handle + "," + hex_data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
