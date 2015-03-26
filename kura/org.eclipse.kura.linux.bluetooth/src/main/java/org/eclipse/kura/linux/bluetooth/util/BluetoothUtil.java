package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothUtil {
	
	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothUtil.class);
	private static final ExecutorService s_processExecutor = Executors.newSingleThreadExecutor();

	private static final String BD_ADDRESS    = "BD Address:";
	private static final String HCI_VERSION_4 = "HCI Version: 4.0";
	private static final String HCICONFIG     = "hciconfig";
	private static final String HCITOOL       = "hcitool";
	
	public static Map<String,String> getConfig(String name) throws KuraException {
		Map<String,String> props = new HashMap<String,String>();
		SafeProcess proc = null;
		BufferedReader br = null;
		StringBuilder sb = null;
		String[] command = {HCICONFIG, name, "version"};
		try {
			proc = ProcessUtil.exec(command);
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
					// Address reported as:
					// BD Address: xx:xx:xx:xx:xx:xx  ACL MTU: xx:xx SCO MTU: xx:x
					String address = result.substring(index + BD_ADDRESS.length());
					String[] tmpAddress = address.split("\\s", 2);
					address = tmpAddress[0].trim();
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
				proc.destroy();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
			
		}
		
		return props;
	}
	
	public static boolean isEnabled(String name) {
		
		String[] command = {HCICONFIG, name};
		SafeProcess proc = null;
		BufferedReader br = null;
		
		try {
			proc = ProcessUtil.exec(command);
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
				proc.destroy();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
		}
		
		return false;
	}
	
	public static BufferedReader hciconfigCmd(String name, String cmd) {
		String[] command = {HCICONFIG, name, cmd};
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			proc = ProcessUtil.exec(command);
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		} catch (Exception e) {
			s_logger.error("Error executing command: " + command, e);
		} finally {
			try {
				br.close();
				proc.destroy();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
		}
		return br;
	}
	
	public static void killCmd(String cmd, String signal) {
		String[] command = {"pkill", "-" + signal, cmd};
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec(command);
		} catch (IOException e) {
			s_logger.error("Error executing command: " + command, e);
		} finally {
			proc.destroy();
		}
	}
	
	public static BluetoothProcess hcitoolCmd (String name, String cmd, BluetoothProcessListener listener) {
		String[] command = {HCITOOL, "-i", name, cmd};
		BluetoothProcess proc = null;
		try {
			proc = exec(command, listener);
		} catch (Exception e) {
			s_logger.error("Error executing command: " + command, e);
		}
		
		return proc;
	}
	
	private static BluetoothProcess exec(final String[] cmdArray, final BluetoothProcessListener listener) throws IOException {

		// Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = s_processExecutor.submit( new Callable<BluetoothProcess>() {
            @Override
            public BluetoothProcess call() throws Exception {
                Thread.currentThread().setName("BluetoothProcessExecutor");
                BluetoothProcess bluetoothProcess = new BluetoothProcess();
                bluetoothProcess.exec(cmdArray, listener);
                return bluetoothProcess;
            }           
        });
        
        try {
            return futureSafeProcess.get();
        } 
        catch (Exception e) {
            s_logger.error("Error waiting from SafeProcess ooutput", e);
            throw new IOException(e);
        }
	}
}
