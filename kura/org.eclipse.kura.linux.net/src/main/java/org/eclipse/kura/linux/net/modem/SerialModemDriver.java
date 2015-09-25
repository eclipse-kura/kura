/**
d * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.linux.net.modem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.linux.util.ProcessStats;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialModemDriver extends ModemDriver {

	private static final Logger s_logger = LoggerFactory.getLogger(SerialModemDriver.class);

	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");

	private SerialModemComm m_serialModemComm;
	private String m_getModelAtCommand;
	private String m_modemName;
	private String m_modemModel;
	
	private ServiceTracker<ConnectionFactory, ConnectionFactory> m_serviceTracker;

	public SerialModemDriver (String modemName, SerialModemComm serialModemComm, String getModelAtCommand) {

		m_modemName = modemName;
		m_serialModemComm = serialModemComm;
		m_getModelAtCommand = getModelAtCommand;
		BundleContext bundleContext = FrameworkUtil.getBundle(SerialModemDriver.class).getBundleContext();

		m_serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(bundleContext, ConnectionFactory.class, null);
		m_serviceTracker.open(true);
	}

	public int install() throws Exception {
		int status = -1;
		boolean modemReachable = false;

		try {
			modemReachable = isAtReachable(3, 1000);
		} catch (KuraException kuraEx) {
			s_logger.warn("Exception reaching serial modem ... " + kuraEx);
			try {
				unlockSerialPort();
				sleep (2000);
				modemReachable = isAtReachable(3, 1000);
			} catch (Exception e) {
				s_logger.error("Error unlocking the " + this.m_serialModemComm.getDataPort() + " device " + e);
			}
		}

		if (!modemReachable) {
			s_logger.info("{} modem is not reachable, installing driver ...", m_modemName);
			int retries = 3;
			if (OS_VERSION != null && OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
					TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {			
				try {
					turnModemOn();
					retries = 15;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			modemReachable = isAtReachable(retries, 1000);
		}

		if (modemReachable) {
			status = 0;
			s_logger.info("{} modem is reachable !!!", m_modemName);
		} else {
			s_logger.warn("{} modem is not reachable, failed to install modem driver", m_modemName);
		}

		return status;
	}

	public int remove() throws Exception {

		int status = -1;
		boolean modemReachable = true;

		try {
			modemReachable = isAtReachable(3, 1000);
		} catch (KuraException e) {
			s_logger.warn("Exception reaching serial modem ... " + e);
		}
		if (modemReachable) {
			int retries = 3;
			if (OS_VERSION != null && OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
					TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
				turnModemOff();
				sleep (2000);
				retries = 15;
			}

			modemReachable = isAtReachable(retries, 1000);
		}

		if (!modemReachable) {
			status = 0;
			s_logger.info("{} modem is not reachable !!!", m_modemName);
		} else {
			s_logger.info("{} modem is still reachable, failed to remove modem driver", m_modemName);
		}
		return status;
	}
	
	public boolean isReachable()  throws KuraException {
		return isAtReachable(3, 1000);
	}

	public String getModemName () {
		return m_modemName;
	}

	public String getModemModel () {

		return m_modemModel;
	}

	public SerialModemComm getComm () {
		return m_serialModemComm;
	}

	private CommConnection openSerialPort (int tout) throws KuraException {
		
		ConnectionFactory connectionFactory;
		connectionFactory = m_serviceTracker.getService();
		CommConnection connection = null;
		if(connectionFactory != null) {
			String uri = new CommURI.Builder(m_serialModemComm.getAtPort())
			.withBaudRate(m_serialModemComm.getBaudRate())
			.withDataBits(m_serialModemComm.getDataBits())
			.withStopBits(m_serialModemComm.getStopBits())
			.withParity(m_serialModemComm.getParity())
			.withTimeout(tout)
			.build().toString();

			try {
				connection = (CommConnection) connectionFactory.createConnection(uri, 1, false);
			} catch (Exception e) {
				s_logger.warn("Exception creating connection: " + e);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
		
		return connection;
	}

	private void closeSerialPort (CommConnection connection) throws KuraException {
		if(connection != null){
			try {
				connection.close();
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}

	private boolean isAtReachable(int numAttempts, int retryInMsec) throws KuraException {
		boolean status = false;
		CommConnection connection = openSerialPort(2000);

		do {
			numAttempts--;
			try {
				status = (connection.sendCommand("at\r\n".getBytes(), 500).length > 0);
				if (status) {
					byte [] reply = connection.sendCommand(m_getModelAtCommand.getBytes(), 1000, 100);
					if (reply != null) {
						m_modemModel = getResponseString(reply);
						reply = null;
					}
				} else {
					if (numAttempts > 0) {
						sleep(retryInMsec);
					}
				}
			} catch (Exception e) {
				sleep(retryInMsec);
			}			
		} while((status == false) && (numAttempts > 0));

		closeSerialPort(connection);
		return status;	
	}

	// Parse the AT command response for the relevant info
	private String getResponseString(String resp) {
		if(resp == null) {
			return "";
		}

		// remove the command and space at the beginning, and the 'OK' and spaces at the end
		return resp.replaceFirst("^\\S*\\s*", "").replaceFirst("\\s*(OK)?\\s*$", "");
	}

	private String getResponseString(byte[] resp) {
		if (resp == null) {
			return "";
		}

		return getResponseString(new String(resp));
	}

	private void unlockSerialPort() throws Exception {
		String dataPort = m_serialModemComm.getDataPort();
		dataPort = dataPort.substring(dataPort.lastIndexOf("/")+1);
		File fLockFile = new File ("/var/lock/LCK.." + dataPort);
		if (fLockFile.exists()) {
			s_logger.warn("lock exists for the {} device", dataPort);
			BufferedReader br = new BufferedReader(new FileReader(fLockFile));
			int lockedPid = Integer.parseInt(br.readLine().trim());
			br.close();

			ProcessStats processStats = LinuxProcessUtil.startWithStats("pgrep pppd");
			br = new BufferedReader(new InputStreamReader(processStats.getInputStream()));
			String spid = null;
			int pidToKill = -1;
			while ((spid = br.readLine()) != null) {
				int pid = Integer.parseInt(spid);
				if (pid == lockedPid) {
					pidToKill = pid;
					break;
				}
			}
			br.close();
			if (pidToKill > 0) {
				s_logger.info("killing pppd that locks the {} device", dataPort);
				int stat = LinuxProcessUtil.start("kill " + pidToKill, true);
				if (stat == 0) {
					s_logger.info("deleting " + fLockFile.getName());
					fLockFile.delete();
				}
			}
		}
	}
}
