package org.eclipse.kura.linux.bluetooth.le.beacon;

import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothBeaconListener implements BluetoothProcessListener {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothBeaconListener.class);
	
	private BluetoothBeaconCommandListener m_listener = null;
	
	public BluetoothBeaconListener(BluetoothBeaconCommandListener listener) {
		m_listener = listener;
	}
	
	// --------------------------------------------------------------------
	//
	//  BluetoothProcessListener API
	//
	// --------------------------------------------------------------------
	@Override
	public void processInputStream(String string) {

		// Check if the command succedeed and return the last line
		s_logger.debug("Command response : " + string);
		String[] lines = string.split("\n");
		if (lines[0].toLowerCase().contains("usage")) {
			s_logger.info("Command failed. Error in command syntax.");
			m_listener.onCommandFailed(null);		
		}
		else {
			String lastLine = lines[lines.length-1];
		
			// The last line of hcitool cmd return contains:
			// the numbers of packets sent (1 byte)
			// the opcode (2 bytes)
			// the exit code (1 byte)
			// the returned data if any
			String exitCode = lastLine.substring(11, 13);
		
			if (exitCode.equals("00")) {
				s_logger.info("Command " + lines[0].substring(15, 35) + " Succeeded.");
				m_listener.onCommandResults(lastLine);
			} 
			else {
				s_logger.info("Command " + lines[0].substring(15, 35) + " failed. Error " + exitCode);
				m_listener.onCommandFailed(exitCode);
			}
		}
		
	}

	@Override
	public void processInputStream(int ch) {
	}

	@Override
	public void processErrorStream(String string) {
	}

}
