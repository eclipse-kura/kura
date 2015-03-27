package org.eclipse.kura.linux.bluetooth.le;

import java.io.BufferedWriter;
import java.io.IOException;

import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothGattImpl implements BluetoothGatt, BluetoothProcessListener {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothGattImpl.class);
	
	private static final String s_regexNotConnected = "\\[\\s{3}\\].*>\\s*$";
	private static final String s_regexConnected    = "\\[CON\\].*>\\s*$";
	
	private BluetoothProcess m_proc;
	private BufferedWriter   m_bufferedWriter;
	private boolean          m_connected = false;
	
	@Override
	public boolean connect(String address) {
		m_proc = BluetoothUtil.startSession(address, this);
		if (m_proc != null) {
			m_bufferedWriter = m_proc.getWriter();
			try {
				String command = "connect\n";
				sendCmd(command);
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				s_logger.error("Error in thread.", e);
			}
		}
		
		return m_connected;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processInputStream(String line) {
		if (line.trim().matches(s_regexNotConnected)) {
			s_logger.debug("Device not connected");
			m_connected = false;
		}
		else if (line.trim().matches(s_regexConnected)) {
			s_logger.debug("Device connected");
			m_connected = true;
		}
		else if (line.isEmpty()) {
			sendCmd("\n");
		}
		
	}
	
	private void sendCmd(String command) {
		try {
			m_bufferedWriter.write(command);
			m_bufferedWriter.flush();
		} catch (IOException e) {
			s_logger.error("Error writing command: " + command, e);
		}
	}


}
