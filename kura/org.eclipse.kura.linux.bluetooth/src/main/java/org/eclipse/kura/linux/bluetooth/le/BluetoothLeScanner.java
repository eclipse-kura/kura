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
	
	public BluetoothLeScanner() {
		devices = new HashMap<String, String>();
	}
	
	public void startScan(String name, int scanTime, BluetoothScanListener listener) {
		BluetoothProcess proc = null;
		
		try {
			s_logger.debug("Starting bluetooth le scan...");
			proc = BluetoothUtil.hcitoolCmd(name, "lescan", this);
			
			// Sleep for specified time while scan is running
			Thread.sleep(scanTime * 1000);
			// SIGINT must be sent to the hcitool process. Otherwise the adapter must be toggled (down/up).
			BluetoothUtil.killCmd("hcitool", "SIGINT");
			proc.destroy();
			
			scanResult = new ArrayList<BluetoothDevice>();
			Iterator<Entry<String, String>> it = devices.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
				scanResult.add(new BluetoothDeviceImpl(pair.getKey(), pair.getValue()));
			}
			
			// Alert listener that scan is complete
			listener.onScanResults(scanResult);

		} catch (Exception e) {
			s_logger.error("Error running bluetooth LE scan.", e);
			listener.onScanFailed(SCAN_FAILED_INTERNAL_ERROR);
		}
	}

	@Override
	public void processInputStream(String line) {
		
		String name;
		String address;
		
		// Results from hcitool lescan should be in form:
		// <mac_address> <device_name>
		String[] results = line.split("\\s", 2);
		if (results.length == 2) {
			address = results[0].trim();
			name = results[1].trim();
			
			if(address.matches(s_mac_regex)) {
				if (devices.containsKey(address)) {
					if (!name.equals("(unknown)") && !devices.get(address).equals(name)) {
						s_logger.debug("Updating device: " + address + " - " + name);
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
