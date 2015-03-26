package org.eclipse.kura.linux.bluetooth;

import java.util.List;

import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothScanListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothScanListenerAdapter implements BluetoothScanListener {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothScanListenerAdapter.class);
	private BluetoothScanListener m_listener;
	
	public BluetoothScanListenerAdapter(BluetoothScanListener listener) {
		m_listener = listener;
	}
	@Override
	public void onScanFailed(int errorCode) {
		try {
			m_listener.onScanFailed(errorCode);
		} catch (Exception e) {
			s_logger.error("Error notifying listener " + m_listener + " for onScanFailed.", e);
		}
	}

	@Override
	public void onScanResults(List<BluetoothDevice> devices) {
		try {
			m_listener.onScanResults(devices);
		} catch (Exception e) {
			s_logger.error("Error notifying listener " + m_listener + " for onScanResults.", e);
		}
	}

}
