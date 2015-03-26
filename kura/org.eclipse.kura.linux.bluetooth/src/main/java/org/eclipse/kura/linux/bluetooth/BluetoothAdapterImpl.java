package org.eclipse.kura.linux.bluetooth;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothScanListener;
import org.eclipse.kura.linux.bluetooth.le.BluetoothLeScanner;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothAdapterImpl implements BluetoothAdapter {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothAdapterImpl.class);
	
	private String  m_name;
	private String 	m_address;
	private boolean m_leReady;
	private int     m_scanTime;
	
	public BluetoothAdapterImpl(String name, int scanTime) throws KuraException {
		m_name = name;
		m_scanTime = scanTime;
		buildAdapter(name);
	}
	
	// --------------------------------------------------------------------
	//
	//  Private methods
	//
	// --------------------------------------------------------------------
	private void buildAdapter(String name) throws KuraException {
		Map<String,String> props = new HashMap<String,String>();
		props = BluetoothUtil.getConfig(name);
		m_address = props.get("address");
		m_leReady= Boolean.parseBoolean(props.get("leReady"));
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
	public int getScanTime() {
		return m_scanTime;
	}
	
	@Override
	public void setScanTime(int scanTime) {
		m_scanTime = scanTime;
	}

	@Override
	public boolean isEnabled() {
		return BluetoothUtil.isEnabled(m_name);
	}

	@Override
	public void startLeScan(BluetoothScanListener listener) {
		BluetoothLeScanner bls = new BluetoothLeScanner();
		bls.startScan(m_name, m_scanTime, listener);

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

}
