package org.eclipse.kura.linux.bluetooth.le;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.bluetooth.BluetoothBeaconData;
import org.eclipse.kura.bluetooth.BluetoothBeaconScanListener;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.linux.bluetooth.BluetoothDeviceImpl;
import org.eclipse.kura.linux.bluetooth.util.BTSnoopListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeScanner implements BluetoothProcessListener, BTSnoopListener {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothLeScanner.class); 
	private static final String s_mac_regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

	public static final int SCAN_FAILED_INTERNAL_ERROR = 0x0003;
	private static final String SIGINT = "2";

	private Map<String, String> m_devices;
	private List<BluetoothDevice> m_scanResult;
	private BluetoothProcess m_proc = null;
	private BluetoothProcess m_dump_proc = null;
	private BluetoothLeScanListener m_listener = null;
	private BluetoothBeaconScanListener m_beacon_listener = null;
	private boolean m_scanRunning = false;

	public BluetoothLeScanner() {
		m_devices = new HashMap<String, String>();
	}
	
	public void startScan(String name, BluetoothLeScanListener listener) {
		m_listener = listener;

		s_logger.info("Starting bluetooth le scan...");
		
		// Start scan process
		m_proc = BluetoothUtil.hcitoolCmd(name, "lescan", this);
					
		set_scanRunning(true);
	}
	

	public void startBeaconScan(String name, BluetoothBeaconScanListener listener) {
		m_beacon_listener = listener;

		s_logger.info("Starting bluetooth le beacon scan...");

		// Start scan process
		m_proc = BluetoothUtil.hcitoolCmd(name, new String[]{ "lescan-passive", "--duplicates" }, this);
		
		// Start dump process
		m_dump_proc = BluetoothUtil.btdumpCmd(name, this);
					
		set_scanRunning(true);
	}

	public void killScan() {
		// SIGINT must be sent to the hcitool process. Otherwise the adapter must be toggled (down/up).
		if (m_proc != null) {
			s_logger.info("Killing hcitool...");
			BluetoothUtil.killCmd(BluetoothUtil.HCITOOL, SIGINT);
			m_proc = null;
		}
		else
			s_logger.info("Cannot Kill hcitool, m_proc = null ...");
		
		// Shut down btdump process
		if (m_dump_proc != null) {
			s_logger.info("Killing btdump...");
			m_dump_proc.destroy();
			m_dump_proc = null;
		}
		else
			s_logger.info("Cannot Kill btdump, m_dump_proc = null ...");
		
		set_scanRunning(false);
	}

	// --------------------------------------------------------------------
	//
	//  BluetoothProcessListener API
	//
	// --------------------------------------------------------------------
	@Override
	public void processInputStream(String string) {

		String[] lines = string.split("\n");
		for (String line : lines) {
			processLine(line);
		}

		m_scanResult = new ArrayList<BluetoothDevice>();
		for (Entry<String, String> device : m_devices.entrySet()) {
			m_scanResult.add(new BluetoothDeviceImpl(device.getKey(), device.getValue()));
			s_logger.info("m_scanResult.add {} - {}", device.getKey(), device.getValue());
		}

		// Alert listener that scan is complete

		if(m_listener != null)
			m_listener.onScanResults(m_scanResult);
	}

	@Override
	public void processInputStream(int ch) {
	}

	@Override
	public void processErrorStream(String string) {
	}

	// --------------------------------------------------------------------
	//
	//  Private methods
	//
	// --------------------------------------------------------------------
	private void processLine(String line) {
		String name;
		String address;
		s_logger.info(line);
		if (line.contains("Set scan parameters failed:")) {
			s_logger.error("Error : " + line);
		} else {
			// Results from hcitool lescan should be in form:
			// <mac_address> <device_name>
			String[] results = line.split("\\s", 2);
			if (results.length == 2) {
				address = results[0].trim();
				name = results[1].trim();

				if(address.matches(s_mac_regex)) {
					if (m_devices.containsKey(address)) {
						if (!name.equals("(unknown)") && !m_devices.get(address).equals(name)) {
							s_logger.debug("Updating device: " + address + " - " + name);
							m_devices.put(address, name);
						}
					}
					else {
						s_logger.debug("Device found: " + address + " - " + name);
						m_devices.put(address, name);
					}
				}
			}
		}
	}
	

	@Override
	public void processBTSnoopRecord(byte[] record) {

		try {
			
			// Extract beacon advertisements
			List<BluetoothBeaconData> beaconDatas = BluetoothUtil.parseLEAdvertisingReport(record);

			// Extract beacon data
			for(BluetoothBeaconData beaconData : beaconDatas) {
				
				// Notify the listener
				try {
					
					if(m_beacon_listener != null)
						m_beacon_listener.onBeaconDataReceived(beaconData);
					
				} catch(Exception e) {
					s_logger.error("Scan listener threw exception", e);
				}
			}
			
		} catch(Exception e) {
			s_logger.error("Error processing advertising report", e);
		}
		
	}

	public boolean is_scanRunning() {
		return m_scanRunning;
	}

	public void set_scanRunning(boolean m_scanRunning) {
		this.m_scanRunning = m_scanRunning;
	}


}
