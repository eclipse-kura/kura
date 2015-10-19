package org.eclipse.kura.linux.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.linux.bluetooth.le.BluetoothLeScanner;
import org.eclipse.kura.linux.bluetooth.le.beacon.BluetoothAdvertisingData;
import org.eclipse.kura.linux.bluetooth.le.beacon.BluetoothBeaconListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothAdapterImpl implements BluetoothAdapter {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothAdapterImpl.class);
	
	private static List<BluetoothDevice> s_connectedDevices;
	
	private String  m_name;
	private String 	m_address;
	private boolean m_leReady;
	private BluetoothLeScanner m_bls = null;
	private BluetoothBeaconCommandListener m_bbcl;
	
	// See Bluetooth 4.0 Core specifications (https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737)
	private final String OGF_CONTROLLER_CMD           = "0x08";
	private final String OCF_ADVERTISING_PARAM_CMD    = "0x0006";
	private final String OCF_ADVERTISING_DATA_CMD     = "0x0008";
	private final String OCF_ADVERTISING_ENABLE_CMD   = "0x000a";
	
	public BluetoothAdapterImpl(String name) throws KuraException {
		m_name = name;
		m_bbcl = null;
		buildAdapter(name);
	}
	
	public BluetoothAdapterImpl(String name, BluetoothBeaconCommandListener bbcl) throws KuraException {
		m_name = name;
		m_bbcl = bbcl;
		buildAdapter(name);
	}
	
	public void setBluetoothBeaconCommandListener(BluetoothBeaconCommandListener bbcl) {
		this.m_bbcl = bbcl;
	}
	
	// --------------------------------------------------------------------
	//
	//  Private methods
	//
	// --------------------------------------------------------------------
	private void buildAdapter(String name) throws KuraException {
		s_logger.debug("Creating new Bluetooth adapter: " + name);
		Map<String,String> props = new HashMap<String,String>();
		props = BluetoothUtil.getConfig(name);
		m_address = props.get("address");
		m_leReady= Boolean.parseBoolean(props.get("leReady"));
	}
	
	private String[] toStringArray(String string) {
		
		// Regex to split a string every 2 characters
		return string.split("(?<=\\G..)");
		
	}
	

	// --------------------------------------------------------------------
	//
	//  Static methods
	//
	// --------------------------------------------------------------------
	public static void addConnectedDevice(BluetoothDevice bd) {
		if (s_connectedDevices == null) {
			s_connectedDevices = new ArrayList<BluetoothDevice>();
		}
		s_connectedDevices.add(bd);
	}
	
	public static void removeConnectedDevice(BluetoothDevice bd) {
		if (s_connectedDevices == null) {
			return;
		}
		s_connectedDevices.remove(bd);
	}
	
	// --------------------------------------------------------------------
	//
	//  BluetoothAdapter API
	//
	// --------------------------------------------------------------------
	
	@Override
	public String getAddress() {
		return m_address;
	}
	
	@Override
	public boolean isEnabled() {
		return BluetoothUtil.isEnabled(m_name);
	}

	@Override
	public void startLeScan(BluetoothLeScanListener listener) {
		m_bls = new BluetoothLeScanner();
		m_bls.startScan(m_name, listener);
	}

	public void killLeScan() {
		if(m_bls!=null){
			m_bls.killScan();
			m_bls = null;
		}
	}

	public boolean isScanning() {
		if(m_bls!=null)
			return m_bls.is_scanRunning();
		else return false;
	}

	@Override
	public boolean isLeReady() {
		return m_leReady;
	}

	@Override
	public void enable() {
		BluetoothUtil.hciconfigCmd(m_name, "up");
	}

	@Override
	public void disable() {
		BluetoothUtil.hciconfigCmd(m_name, "down");
	}
	
	@Override
	public BluetoothDevice getRemoteDevice(String address) {
		return new BluetoothDeviceImpl(address, "");
	}

	@Override
	public void startBeaconAdvertising() {
		
		BluetoothBeaconListener bbl = new BluetoothBeaconListener(m_bbcl);
		
		s_logger.debug("Start Advertising : hcitool -i " + m_name + " cmd " + OGF_CONTROLLER_CMD + " " + OCF_ADVERTISING_ENABLE_CMD + " 01");
		s_logger.info("Start Advertising on interface " + m_name);
		String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "01" };
		BluetoothUtil.hcitoolCmd(m_name, cmd, bbl);
		
	}

	@Override
	public void stopBeaconAdvertising() {
		
		BluetoothBeaconListener bbl = new BluetoothBeaconListener(m_bbcl);
		
		s_logger.debug("Stop Advertising : hcitool -i " + m_name + " cmd " + OGF_CONTROLLER_CMD + " " + OCF_ADVERTISING_ENABLE_CMD + " 00");
		s_logger.info("Stop Advertising on interface " + m_name);
		String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "00" };
		BluetoothUtil.hcitoolCmd(m_name, cmd, bbl);
	}

	@Override
	public void setBeaconAdvertisingInterval(Integer min, Integer max) {
		
		BluetoothBeaconListener bbl = new BluetoothBeaconListener(m_bbcl);
		
		// See http://stackoverflow.com/questions/21124993/is-there-a-way-to-increase-ble-advertisement-frequency-in-bluez
		String[] minHex = toStringArray(BluetoothAdvertisingData.to2BytesHex(min));
		String[] maxHex = toStringArray(BluetoothAdvertisingData.to2BytesHex(max));
		
		s_logger.debug("Set Advertising Parameters : hcitool -i " + m_name + " cmd " + OGF_CONTROLLER_CMD + " " + OCF_ADVERTISING_PARAM_CMD + " " + minHex[1]+ " " + minHex[0] + " " + maxHex[1] + " " + maxHex[0] + " 03 00 00 00 00 00 00 00 00 07 00");
		s_logger.info("Set Advertising Parameters on interface " + m_name);
		String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_PARAM_CMD, minHex[1], minHex[0], maxHex[1], maxHex[0], "03", "00", "00", "00", "00", "00", "00", "00", "00", "07", "00"};
		BluetoothUtil.hcitoolCmd(m_name, cmd, bbl);
		
	}

	@Override
	public void setBeaconAdvertisingData(String uuid, Integer major, Integer minor, String companyCode, Integer txPower, boolean LELimited, boolean LEGeneral,
			boolean BR_EDRSupported, boolean LE_BRController, boolean LE_BRHost) {
		
		BluetoothBeaconListener bbl = new BluetoothBeaconListener(m_bbcl);
		
		String[] dataHex = toStringArray(BluetoothAdvertisingData.getData(uuid, major, minor, companyCode, txPower, LELimited, LEGeneral, BR_EDRSupported, LE_BRController, LE_BRHost));
		String[] cmd = new String[3 + dataHex.length];
		cmd[0] = "cmd";
		cmd[1] = OGF_CONTROLLER_CMD;
		cmd[2] = OCF_ADVERTISING_DATA_CMD;
		for (int i=0; i < dataHex.length; i++)
			cmd[i+3] = dataHex[i];
		
		s_logger.debug("Set Advertising Data : hcitool -i " + m_name + "cmd " + OGF_CONTROLLER_CMD + " " + OCF_ADVERTISING_DATA_CMD + " " + Arrays.toString(dataHex));
		s_logger.info("Set Advertising Data on interface " + m_name);
		BluetoothUtil.hcitoolCmd(m_name, cmd, bbl);
		
	}
	
	@Override
	public void ExecuteCmd(String ogf, String ocf, String parameter) {
		
		BluetoothBeaconListener bbl = new BluetoothBeaconListener(m_bbcl);
		
		String[] paramArray = toStringArray(parameter);
		s_logger.info("Execute custom command : hcitool -i " + m_name + "cmd " + ogf + " " + ocf + " " + Arrays.toString(paramArray));
		String[] cmd = new String[3 + paramArray.length];
		cmd[0] = "cmd";
		cmd[1] = ogf;
		cmd[2] = ocf;
		for (int i=0; i < cmd.length; i++)
			cmd[i+3] = paramArray[i];
		
		BluetoothUtil.hcitoolCmd(m_name, cmd, bbl);
	}
	
}
