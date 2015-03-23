package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothUtil {
	
	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothUtil.class);
	
	private static final String BD_ADDRESS    = "BD Address:";
	private static final String HCI_VERSION_4 = "HCI Version: 4.0";
	private static final String HCICONFIG     = "hciconfig";
	private static final String HCITOOL       = "hcitool";
	
	public static Map<String,String> getConfig(String name) throws KuraException {
		Map<String,String> props = new HashMap<String,String>();
		
		Process proc = null;
		BufferedReader br = null;
		StringBuilder sb = null;
		String command = HCICONFIG + " " + name + " version";
		try {
			proc = ProcessUtil.exec(command);
			proc.waitFor();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains("command not found")) {
					throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
				}
				if (line.contains("No such device")) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
				}
				sb.append(line + "\n");
			}
			
			//TODO: Pull more parameters from hciconfig? 
			String[] results = sb.toString().split("\n");
			int index = -1;
			props.put("leReady", "false");
			for (String result : results) {
				if((index = result.indexOf(BD_ADDRESS)) >= 0) {
					String address = result.substring(index + BD_ADDRESS.length()).trim();
					props.put("address", address);
					s_logger.trace("Bluetooth adapter address set to: " + address);
				}
				if((index = result.indexOf(HCI_VERSION_4)) >= 0) {
					props.put("leReady", "true");
					s_logger.trace("Bluetooth adapter is LE ready");
				}
			}
			
			
		} catch (Exception e) {
			s_logger.error("Failed to execute command: " + command, e);
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
			ProcessUtil.destroy(proc);
		}
		
		return props;
	}
	
	public static boolean isEnabled(String name) {
		
		String command = HCICONFIG + " " + name;
		Process proc = null;
		BufferedReader br = null;
		
		try {
			proc = ProcessUtil.exec(command);
			proc.waitFor();
			
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains("UP")) {
					return true;
				}
				if (line.contains("DOWN")) {
					return false;
				}
			}
		} catch (Exception e) {
			s_logger.error("Error executing command: " + command, e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
			ProcessUtil.destroy(proc);
		}
		
		return false;
	}
	
	public static void hciconfigCmd(String name, String cmd) {
		String command = HCICONFIG + " " + name + " " + cmd;
		Process proc = null;
		try {
			proc = ProcessUtil.exec(command);
			proc.waitFor();
		} catch (Exception e) {
			s_logger.error("Error executing command: " + command, e);
		} finally {
			ProcessUtil.destroy(proc);
		}
		
	}
}
