/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.bluetooth.util.BluetoothSafeProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothUtil {
	
	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothUtil.class);
	private static final ExecutorService s_processExecutor = Executors.newSingleThreadExecutor();

	private static final String BD_ADDRESS    = "BD Address:";
	private static final String HCI_VERSION   = "HCI Version:";
	private static final String HCICONFIG     = "hciconfig";
	private static final String HCITOOL       = "hcitool";
	private static final String GATTTOOL      = "gatttool";
	
	/*
	 * Use hciconfig utility to return information about the bluetooth adapter
	 */
	public static Map<String,String> getConfig(String name) throws KuraException {
		Map<String,String> props = new HashMap<String,String>();
		BluetoothSafeProcess proc = null;
		BufferedReader br = null;
		StringBuilder sb = null;
		String[] command = { HCICONFIG, name, "version" };
		try {
			proc = BluetoothProcessUtil.exec(command);
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
			props.put("leReady", "false");
			for (String result : results) {
				if((result.indexOf(BD_ADDRESS)) >= 0) {
					// Address reported as:
					// BD Address: xx:xx:xx:xx:xx:xx  ACL MTU: xx:xx SCO MTU: xx:x
					String[] ss = result.split(" ");
					String address="";
					for(String sss:ss){
						if(sss.matches("^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$")){
							address = sss;
							break;
						}
					}
//					String address = result.substring(index + BD_ADDRESS.length());
//					String[] tmpAddress = address.split("\\s", 2);
//					address = tmpAddress[0].trim();
					props.put("address", address);
					s_logger.trace("Bluetooth adapter address set to: " + address);
				}
				if((result.indexOf(HCI_VERSION)) >= 0) {
					// HCI version : 4.0 (0x6) or HCI version : 4.1 (0x7)
					if((result.indexOf("0x6") >= 0) || (result.indexOf("0x7") >= 0)) {
						props.put("leReady", "true");
						s_logger.trace("Bluetooth adapter is LE ready");
					}
				}
			}
			
		} catch (Exception e) {
			s_logger.error("Failed to execute command: {}", command, e);
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		} finally {
			try {
				if (br != null)
					br.close();
				if (proc != null)
					proc.destroy();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
			
		}
		
		return props;
	}
	
	/*
	 * Use hciconfig utility to determine status of bluetooth adapter
	 */
	public static boolean isEnabled(String name) {
		
		String[] command = { HCICONFIG, name };
		BluetoothSafeProcess proc = null;
		BufferedReader br = null;
		
		try {
			proc = BluetoothProcessUtil.exec(command);
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
			s_logger.error("Error executing command: {}", command, e);
		} finally {
			try {
				if (br != null)
					br.close();
				if (proc != null)
					proc.destroy();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
		}
		
		return false;
	}
	
	/*
	 * Utility method that allows sending any hciconfig command. The buffered
	 * response is returned in case results are needed.
	 */
	public static BufferedReader hciconfigCmd(String name, String cmd) {
		String[] command = { HCICONFIG, name, cmd };
		BluetoothSafeProcess proc = null;
		BufferedReader br = null;
		try {
			proc = BluetoothProcessUtil.exec(command);
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		} catch (Exception e) {
			s_logger.error("Error executing command: {}", command, e);
		} finally {
			try {
				if (br != null)
					br.close();
				if (proc != null)
					proc.destroy();
			} catch (IOException e) {
				s_logger.error("Error closing read buffer", e);
			}
		}
		return br;
	}
	
	/*
	 * Utility method to send specific kill commands to processes.
	 */
	public static void killCmd(String cmd, String signal) {
		//String[] command = { "pkill", "-" + signal, cmd };
		String[] commandPidOf = { "pidof", cmd };
		BluetoothSafeProcess proc = null;
		BufferedReader br = null;
		try {
			proc = BluetoothProcessUtil.exec(commandPidOf);
			proc.waitFor();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String pid = br.readLine();
			
			// Check if the pid is not empty
			if (pid != null) {
				String[] commandKill = { "kill", "-" + signal, pid };
				proc = BluetoothProcessUtil.exec(commandKill);
			}
			
		} catch (IOException e) {
			s_logger.error("Error executing command: {}", commandPidOf, e);
		} catch (InterruptedException e) {
			s_logger.warn("Error executing command: {}", commandPidOf, e);
		} finally {
			if (proc != null)
				proc.destroy();
			try {
				if (proc != null)
					br.close();
			} catch (IOException e) {
				s_logger.warn("Error closing process for command: {}", commandPidOf, e);
			}
		}
	}
	
	/*
	 * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
	 * BluetoothProcessListener is used to receive output from the process. 
	 */
	public static BluetoothProcess hcitoolCmd (String name, String cmd, BluetoothProcessListener listener) {
		String[] command = { HCITOOL, "-i", name, cmd };
		BluetoothProcess proc = null;
		try {
			s_logger.debug("Command executed : {}", Arrays.toString(command));
			proc = exec(command, listener);
		} catch (Exception e) {
			s_logger.error("Error executing command: {}", command, e);
		}
		
		return proc;
	}
	
	/*
	 * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
	 * BluetoothProcessListener is used to receive output from the process. 
	 */
	public static BluetoothProcess hcitoolCmd (String name, String[] cmd, BluetoothProcessListener listener) {
		String[] command = new String[3 + cmd.length];
		command[0] = HCITOOL;
		command[1] = "-i";
		command[2] = name;
		for (int i=0; i < cmd.length; i++)
			command[i+3] = cmd[i];
		BluetoothProcess proc = null;
		try {
			s_logger.debug("Command executed: {}", Arrays.toString(command));
			proc = exec(command, listener);
		} catch (Exception e) {
			s_logger.error("Error executing command: {}", command, e);
		}
		
		return proc;
	}
	
	/*
	 * Method to start an interactive session with a remote Bluetooth LE device using the gatttool utility. The
	 * listener is used to receive output from the process. 
	 */
	public static BluetoothProcess startSession(String address, BluetoothProcessListener listener) {
		String[] command = { GATTTOOL, "-b", address, "-I" };
		BluetoothProcess proc = null;
		try {
			proc = exec(command, listener);
		} catch (Exception e) {
			s_logger.error("Error executing command: ", command, e);
		}
		return proc;
	}
	
	/*
	 * Method to create a separate thread for the BluetoothProcesses.
	 */
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
            s_logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
	}
}
