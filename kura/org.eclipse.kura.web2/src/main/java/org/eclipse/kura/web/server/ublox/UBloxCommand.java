package org.eclipse.kura.web.server.ublox;
/** class name : UBloxCommand
 * implement the AT command parser to analyze the events serial port
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import asg.cliche.Command;

public class UBloxCommand {
	
	 private Boolean scanning = false; // state of discovery device
	 private Boolean get_services = false;
	 private Boolean get_servicesByUuid = false;
	 private static BluetoothLeScanListenerImpl listScan = null ;
	 private static BluetoothLeGattServiceImpl listServices = null;
	 private static BluetoothLeGattServiceImpl listServivesByUuid = null;
	 private static BluetoothLeGattCharacteristicImpl characteristic = null; // characteristic of services
	 private static BluetoothLeGattDescriptorImpl char_descriptor = null;  // characteristic of descriptor
	 
	 private Boolean scan_end = false; // 
	 private Boolean listService_end = false;

	public UBloxCommand() {
		
	}
	
	
	/* receive the scan state form the BluetoothAtCommand class when we start discovery
	 * @param scan
	 *        boolean : true is the discovery is starting
	 *                  false something else
	 * @return
	 */
	public void setScanning(Boolean scan) {
		if (scan && !scanning) { // if discovery is activated and the scan isn't in progress
			listScan = new BluetoothLeScanListenerImpl();
			scanning = scan;
		} else if (!scan && scanning) {
			scanning = scan;
		}
	}
	
	
	/* to know success end of the discovery devices
	 * @param 
	 * @return
	 */
	public boolean getScanningEnd() {
		return this.scan_end;
	}
	
	/* to know success end of the discovery devices
	 * @param 
	 * @return
	 */
	public boolean getListOfServiesEnd() {
		return this.listService_end;
	}
	
	
	
	/* get list of the discovery device found
	 * @param 
	 * @return
	 */
	public static BluetoothLeScanListenerImpl getListScan() {
		return listScan;
	}
	
	
	public static BluetoothLeGattServiceImpl getListServices() {
		return listServices;
	}
	
	public static BluetoothLeGattServiceImpl getListServicesByUuid() {
		return listServivesByUuid;
	}
	
	
	public static BluetoothLeGattCharacteristicImpl getCharacteristic() {
		return characteristic;
	}
	
	
	public static BluetoothLeGattDescriptorImpl getDescriptor() {
		return char_descriptor;
	}
	
	
	
	public void SetAskListService(Boolean service_list) {
		listServices = new BluetoothLeGattServiceImpl();
		get_services = service_list;
	}
	
	public void SetAskListServiceByUuid(Boolean service_listByuuid) {
		listServivesByUuid = new BluetoothLeGattServiceImpl(BluetoothLeDeviceImpl.getUUID());
		get_servicesByUuid = service_listByuuid;
	}
	
	
	
	/**
     * response of discovery device. when data (start with UBTD) of discovery device is received from serial port
     *
     * @param bd_addr
     *              mac address
     * @param rssi
     *              power receive
     * @param  device_name
     *              name of the device
     * @param  type
     *              data type
     * @data
     *              data       
     * @return 
     *         
     */
	@Command
	public String UBTD(String bd_addr, String rssi, String device_name, String type, String data) {
		BluetoothLeDeviceImpl device = new BluetoothLeDeviceImpl(bd_addr, device_name.replaceAll("\"", ""),
				Short.valueOf(rssi));
		listScan.addDevice(device);
		return "scanning";
	}
	
	@Command
	public String UBTD(String bd_addr, String rssi, String device_name, String type) {
		BluetoothLeDeviceImpl device = new BluetoothLeDeviceImpl(bd_addr, device_name.replaceAll("\"", ""),
				Short.valueOf(rssi));
		listScan.addDevice(device);
		return "scanning";
	}
	
	/**
     * +UUBACLC will be sent out to confirm the connection establishment
     *
     * @param conn_handle
     *              connexion handle
     * @param test1
     *              
     * @param  macAddress
     *              mac address of the device    
     * @return  
     *         
     */
	@Command
	public String UUBTACLC(String conn_handle, String test1, String macAddress) {
		return "connecting";
	}
	/*
	@Command
	public short UBTRSS(short b_rssi) {
		BluetoothLeDeviceImpl.rssi = b_rssi;
		
		return b_rssi;
	}
	
	*/
	
	
	/**
     * +UBTACLD will be sent out to confirm the connection establishment
     *
     * @param conn_handle
     *              connexion handle  
     * @return O or -1 if device already connect
     *         
     */
	@Command
	public String UUBTACLD(String conn_handle) {
		return "disconnect";
	}
	
	
	/**
     * GATT : +UBTGDP  response of Discovers all primary services on the remote device
     *
     * @param conn_handle
     *              connexion handle
     * @param start
     *              start handle of the service
     * @param end
     *              end handle of the service
     * @param uuid
     *              uuid of the service. this can either be 16bit  or 128bit
     * @return OK or EEROR
     *         
     */
	@Command
	public String UBTGDP(String conn_handle, String start, String end, String uuid) {
		BluetoothLeServiceImpl service = new BluetoothLeServiceImpl(Integer.valueOf(conn_handle),
				Integer.valueOf(start), Integer.valueOf(end), UUID.fromString(uuid));
		listServices.addService(service);
		return "service discovers";
	}

	/**
     * GATT : +UBTGDU  response of Discovers all primary services by UUID on the remote
     *
     * @param conn_handle
     *              connexion handle
     * @param start
     *              start handle of the service
     * @param end
     *              end handle of the service
   
     * @return OK or ERROR
     *         
     */
	@Command
	public String UBTGDPU(String conn_handle, String start, String end) {
		BluetoothLeServiceImpl service = new BluetoothLeServiceImpl(Integer.valueOf(conn_handle),
				Integer.valueOf(start), Integer.valueOf(end));
		listServivesByUuid.addServiceByUuid(service);
		return "service discovers by uuid";
	}
	
	@Command
	public String UBTGFI(String test1, String test2, String test3, String test4, String test5) {
		return ("UBTGFI ");
	}

	
	 /**
     * GATT : response of discover all characteristics of services
     * 
     * @param conn_handle
     *             connexion handle
     * @param attr_handle
     *             attribute handle of the characteristic
     * @param propreties
     *             bit mask describing the properties of the characteristic
     * @param value_handle
     *             attribute handle of the characteristic value
     * @param uuid
     *             UUID of the characteristic 
     * @return OK or Error
     */
	@Command
	public String UBTGDCS(int conn_handle, int attr_handle, byte[] propreties, int value_handle, String uuid) {
		characteristic = new BluetoothLeGattCharacteristicImpl(conn_handle, attr_handle, propreties, value_handle,
				uuid);
		return "UBTGDCS";
	}
	
	
	 /**
     * GATT : response of discover all characteristic descriptors
     * 
     * @param conn_handle
     *             connexion handle
     * @param char_handle
     *             handle handle of the characteristic
     * @param desc_handle
     *             handle of the descriptor
     * @param uuid
     *             UUID of the characteristic 
     * @return OK Error
     */
	@Command
	public String UBTGDCD(int conn_handle, int char_handle, int desc_handle, String uuid) {
		char_descriptor = new BluetoothLeGattDescriptorImpl(conn_handle, char_handle, desc_handle, uuid);
		return "UBTGDCD";
	}
	
	
	
	/**
     * GATT : response of read characteristic
     * 
     * @param conn_handle
     *             connexion handle
     * @param value_handle
     *             handle handle of the characteristic
     * @param hex_data
     *             the read data as hex string
     * @return OK or Error
     */
	@Command
	public String UBTGR(int conn_handle, int value_handle, String hex_data) {
		return "UBTGR";
	}
	
	
	
	/**
     * GATT : response of read characteristic by UUID
     * 
     * @param conn_handle
     *             connexion handle
     * @param value_handle
     *             handle handle of the characteristic
     * @param hex_data
     *             the read data as hex string
     * @return OK or Error
     */
	@Command
	public String UBTGRU(int conn_handle, int value_handle, String hex_data) {
		return ("UBTGRU");
	}

	/*
	@Command
	public String UBTGWN() {
		return("UBTGWN");
	}
	
	
	@Command
	public String UBTGWL() {
		return("UBTGWL");
	}
	*/
	
	/**
     * GATT : response of notification. this event is received when the remote side sends a notification
     * 
     * @param conn_handle
     *             connexion handle
     * @param value_handle
     *             handle handle of the characteristic
     * @param hex_data
     *             the read data as hex string
     * @return
     */
	@Command
	public String UUBTGN(int conn_handle, int value_handle, String hex_data) {
		return ("UUBTGN");
	}
	
	/**
     * GATT : response of notification. this event is received when the remote side sends an indication
     * 
     * @param conn_handle
     *             connexion handle
     * @param value_handle
     *             handle handle of the characteristic
     * @param hex_data
     *             the read data as hex string
     * @return
     */
	@Command
	public String UUBTGI(int conn_handle, int value_handle, String hex_data) {
		return ("UUBTGI");
	}
	
	/**
     * read response of adapter name
     * 
     * @param name
     *             name of adapter returned
     * @return
     */
	@Command
	public String UBTLN(String name) {
		BluetoothLeAdapterImpl adap = new BluetoothLeAdapterImpl();
		adap.setName(name);
		return name;
	}
	
	/**
     * read response of mac address form adapter
     * 
     * @param macAddr
     *             macAddr of adapter returned
     * @return
     */
	@Command
	public String UMLA(String macAddr) {
		BluetoothLeAdapterImpl adap = new BluetoothLeAdapterImpl();
		adap.setAddress(macAddr);
		return macAddr;
	}
	
	
	@Command
	public String OK() {
		scan_end = true;
		if (scanning) {
			setScanning(false);
			listScan.onScanResults();
		}
		return ("OK");
	}
	
	@Command
	public String ERROR() {
		return ("ERROR");
	}
	
	@Command
	public String STARTUP() {
		listService_end = true;
		SetAskListService(false);
		listServices.listServicesOnReslut();
		;
		return ("STARTUP");
	}

}
