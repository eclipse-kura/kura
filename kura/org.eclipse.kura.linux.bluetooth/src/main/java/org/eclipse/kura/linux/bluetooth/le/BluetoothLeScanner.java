package org.eclipse.kura.linux.bluetooth.le;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothScanListener;
import org.eclipse.kura.linux.bluetooth.BluetoothDeviceImpl;
import org.eclipse.kura.linux.bluetooth.BluetoothScanListenerAdapter;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeScanner implements BluetoothProcessListener {
	
	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothLeScanner.class); 
	private static final String s_mac_regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
	
	public static final int SCAN_FAILED_INTERNAL_ERROR = 0x0003;
	
	private Map<String, String> devices;
	private List<BluetoothDevice> scanResult;
	private BluetoothScanListenerAdapter m_listener;
	
	public BluetoothLeScanner(BluetoothScanListener listener) {
		devices = new HashMap<String, String>();
		m_listener = new BluetoothScanListenerAdapter(listener);
	}
	
	public void startScan(String name, int scanTime) {
		BluetoothProcess proc = null;
		
		try {
			s_logger.debug("Starting scan...");
			proc = BluetoothUtil.hcitoolCmd(name, "lescan", this);
			//Thread.sleep(scanTime * 1000);
			long start = System.currentTimeMillis();
			long end = start + scanTime * 1000;
			s_logger.debug("Starting timer...");
			while (System.currentTimeMillis() < end) {
				
			}
			s_logger.debug("Killing scan");
			BluetoothUtil.killCmd("hcitool", "SIGINT");
			proc.destroy();
			
			scanResult = new ArrayList<BluetoothDevice>();
			Iterator<Entry<String, String>> it = devices.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
				scanResult.add(new BluetoothDeviceImpl(pair.getKey(), pair.getValue()));
			}
			s_logger.debug("Alerting listener");
			m_listener.onScanResults(scanResult);
			s_logger.debug("Done");
		} catch (Exception e) {
			s_logger.error("Error running bluetooth LE scan.", e);
			m_listener.onScanFailed(SCAN_FAILED_INTERNAL_ERROR);
		}
	}

	@Override
	public void processInputStream(String line) {
		
		String name;
		String address;
		
		String[] lines = line.split("\\r?\\n");
		for (String l : lines) {
			String[] results = l.split("\\s", 2);
			if (results.length == 2) {
				address = results[0].trim();
				name = results[1].trim();
				
				if(address.matches(s_mac_regex)) {
					if (devices.containsKey(address)) {
						if (!name.equals("unknown") && !devices.get(address).equals(name)) {
							System.out.println("Updating device: " + address + " - " + name);
							devices.put(address, name);
						}
					}
					else {
						s_logger.debug("Device found: " + address + " - " + name);
						devices.put(address, name);
					}
				}
			}
		}
		
	}
	
}
