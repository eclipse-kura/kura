/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemDriver {
	
	private static final Logger s_logger = LoggerFactory.getLogger(ModemDriver.class);
	
	private static final String TARGET_NAME = System.getProperty("target.device");
	private static final String GPIO_65_PATH = "/sys/class/gpio/gpio65";
	private static final String GPIO_65_DIRECTION_PATH = GPIO_65_PATH + "/direction";
	private static final String GPIO_65_VALUE_PATH = GPIO_65_PATH + "/value";
	private static final String GPIO_60_PATH = "/sys/class/gpio/gpio60";
	private static final String GPIO_60_DIRECTION_PATH = GPIO_60_PATH + "/direction";
	private static final String GPIO_60_VALUE_PATH = GPIO_60_PATH + "/value";
	private static final String GPIO_PATH = "/sys/class/gpio";
	private static final String GPIO_EXPORT_PATH = GPIO_PATH + "/export";
	
	private static final String RELIAGATE_10_20_GPIO_PATH = "/sys/class/gpio/usb-rear-pwr/value";
	private static final String RELIAGATE_50_21_GPIO_11_0_CMD = "/usr/sbin/vector-j21-gpio 11 0";
	private static final String RELIAGATE_50_21_GPIO_11_1_CMD = "/usr/sbin/vector-j21-gpio 11 1"; 
	private static final String RELIAGATE_50_21_GPIO_6_CMD = "/usr/sbin/vector-j21-gpio 6";
	
	private static final int RELIAGATE_10_05_GSM_RESET_GPIO_NUM = 252;
	private static final int RELIAGATE_10_05_GSM_POWERKEY_GPIO_NUM = 123;
	
	public boolean turnModemOff() throws Exception {
		if (TARGET_NAME == null) {
			return false;
		}
		boolean retVal = true;
		int remainingAttempts = 3;
		do {
			if (remainingAttempts <= 0) {
				retVal = false;
				break;
			}
			s_logger.info("turnModemOff() :: turning modem OFF ... attempts left: {}", remainingAttempts);
			if(TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
				toggleGpio("65", GPIO_65_PATH, GPIO_65_DIRECTION_PATH, GPIO_65_VALUE_PATH);
			} else if(TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
				toggleGpio("60", GPIO_60_PATH, GPIO_60_DIRECTION_PATH, GPIO_60_VALUE_PATH);
			} else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_20.getTargetName())) {
				disable1020Gpio();
			} else if (TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName())) {
				int status = exec5021Gpio110();
				s_logger.info("turnModemOff() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_0_CMD, status);
				if (status != 0) {
					continue;
				}
				sleep(1000);

				status = exec5021Gpio111();
				s_logger.info("turnModemOff() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_1_CMD, status);
				if (status != 0) {
					continue;
				}
				sleep(3000);

				status = exec5021Gpio110();
				s_logger.info("turnModemOff() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_0_CMD, status);
				retVal = (status == 0) ? true : false;
			} else {
				s_logger.warn("turnModemOff() :: modem turnOff operation is not supported for the {} platform", TARGET_NAME);
				retVal = true;
				break;
			}
			remainingAttempts--;
			sleep(5000);
		} while (isOn());

		s_logger.info("turnModemOff() :: Modem is OFF? - {}", retVal);
		return retVal;
	}

	public boolean turnModemOn() throws Exception {
		if (TARGET_NAME == null) {
			return false;
		}
		boolean retVal = true;
		int remainingAttempts = 5;
		do {
			if (remainingAttempts <= 0) {
				retVal = false;
				break;
			}
			s_logger.info("turnModemOn() :: turning modem ON ... attempts left: {}", remainingAttempts);
			if(TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
				toggleGpio("65", GPIO_65_PATH, GPIO_65_DIRECTION_PATH, GPIO_65_VALUE_PATH);
			} else if(TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
				toggleGpio("60", GPIO_60_PATH, GPIO_60_DIRECTION_PATH, GPIO_60_VALUE_PATH);
			} else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_20.getTargetName())) {
				enable1020Gpio();
			} else if (TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName())) {
				int status = exec5021Gpio111();
				s_logger.info("turnModemOn() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_1_CMD, status);
				if (status != 0) {
					continue;
				}
				sleep(1000);

				status = exec5021Gpio6();
				s_logger.info("turnModemOn() :: '{}' returned {}", RELIAGATE_50_21_GPIO_6_CMD, status);
				retVal = (status == 0) ? true : false;
			} else {
				s_logger.warn("turnModemOn() :: modem turnOn operation is not supported for the {} platform", TARGET_NAME);
				retVal = true;
				break;
			}
			remainingAttempts--;
			sleep(10000);
		} while (!isOn());

		s_logger.info("turnModemOn() :: Modem is ON? - {}", retVal);
		return retVal;
	}
	
	public boolean resetModem() {
		boolean retVal = true;
		if(KuraConstants.ReliaGATE_10_05.equals(TARGET_NAME)) {
			// just pulse the modem reset pin
			try {
				if (!isSysfsGpioExported(RELIAGATE_10_05_GSM_RESET_GPIO_NUM)) {
					initSysfsGpio(RELIAGATE_10_05_GSM_RESET_GPIO_NUM, false);
				}
				pulseSysfsGpio(RELIAGATE_10_05_GSM_RESET_GPIO_NUM, true, 1000);
				
				// wait until the modem is on again
				int cnt = 10;
				while (!isOn() && cnt > 0) {
					sleep(1000);
					cnt--;
				}
				if (!isOn()) {
					retVal = false;
				}
			} catch (Exception e) {
				retVal = false;
			}
		} else {
			s_logger.warn("resetModem() :: modem reset operation is not supported for the {} platform", TARGET_NAME);
		}
		return retVal;
	}
	
	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}
	
	protected boolean isSysfsGpioExported(int gpioNum) {
		boolean exported = false;
		final String gpioPath = GPIO_PATH + "/gpio" + String.valueOf(RELIAGATE_10_05_GSM_RESET_GPIO_NUM);
		File fgpioFolder = new File (gpioPath);
		if (!fgpioFolder.exists()) {
			exported = true;
		}
		return exported;
	}
	
	protected void initSysfsGpio(int gpioNum, boolean level) throws IOException {
		final String gpioPath = GPIO_PATH + "/gpio" + String.valueOf(RELIAGATE_10_05_GSM_RESET_GPIO_NUM);
		BufferedWriter bwGpioSelect = new BufferedWriter(new FileWriter(GPIO_EXPORT_PATH));
		try {
			bwGpioSelect.write(gpioNum);
			bwGpioSelect.flush();
		} finally {
			bwGpioSelect.close();
		}

		BufferedWriter bwGpioDirection = new BufferedWriter(new FileWriter(gpioPath + "/direction"));
		try {
			bwGpioDirection.write("out");
			bwGpioDirection.flush();
		} finally {
			bwGpioDirection.close();
		}

		BufferedWriter fGpioValue = new BufferedWriter(new FileWriter(gpioPath + "/value"));
		try {
			fGpioValue.write(level ? "1" : "0");
			fGpioValue.flush();
		} finally {
			fGpioValue.close();
		}
	}
	
	protected void pulseSysfsGpio(int gpioNum, boolean level, long duration) throws IOException {
		final String gpioPath = GPIO_PATH + "/gpio" + String.valueOf(RELIAGATE_10_05_GSM_RESET_GPIO_NUM);
		BufferedWriter fGpioValue = new BufferedWriter(new FileWriter(gpioPath + "/value"));
		try {
			fGpioValue.write(level ? "1" : "0");
			fGpioValue.flush();
			sleep(duration);
			fGpioValue.write(level ? "0" : "1");
			fGpioValue.flush();
		} finally {
			fGpioValue.close();
		}
	}
	
	protected void toggleGpio(String gpio, String gpioPath, String directionPath, String valuePath) throws IOException { 		
		File fgpioFolder = new File (gpioPath);
		if (!fgpioFolder.exists()) {
			BufferedWriter bwGpioSelect = new BufferedWriter(new FileWriter(GPIO_EXPORT_PATH));
			bwGpioSelect.write(gpio);
			bwGpioSelect.flush();
			bwGpioSelect.close();
		}

		BufferedWriter bwGpioDirection = new BufferedWriter(new FileWriter(directionPath));
		bwGpioDirection.write("out");
		bwGpioDirection.flush();
		bwGpioDirection.close();

		BufferedWriter fGpioValue = new BufferedWriter(new FileWriter(valuePath));
		fGpioValue.write("0");
		fGpioValue.flush();
		fGpioValue.write("1");
		fGpioValue.flush();
		sleep(5000);
		fGpioValue.write("0");
		fGpioValue.flush();
		fGpioValue.close();
	}
	
	private boolean isOn() throws Exception {

		boolean isModemOn = false;
		if (this instanceof UsbModemDriver) {
			isModemOn = SupportedUsbModems.isAttached(
					((UsbModemDriver)this).getVendor(),
					((UsbModemDriver)this).getProduct());
			s_logger.info("isOn() :: USB modem attached? {}", isModemOn);
		} else if (this instanceof SerialModemDriver) {
			isModemOn = ((SerialModemDriver)this).isReachable();
			s_logger.info("isOn() :: Serial modem reachable? {}", isModemOn);
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
					"Unsupported modem device");
		}
		return isModemOn;
	}
	
	private static void disable1020Gpio() throws IOException {
		FileWriter fw = null;
		try{
			fw = new FileWriter(RELIAGATE_10_20_GPIO_PATH);
			fw.write("0");
		} finally {
			if (fw != null){
				fw.close();
			}
		}
	}
	
	private static void enable1020Gpio() throws IOException {
		FileWriter fw = null;
		try{
			fw = new FileWriter(RELIAGATE_10_20_GPIO_PATH);
			fw.write("1");
		} finally {
			if (fw != null){
				fw.close();
			}
		}
	}
	
	private static int exec5021Gpio110() throws IOException, InterruptedException {
		int status;
		SafeProcess pr = null;
		try {
			pr = ProcessUtil.exec(RELIAGATE_50_21_GPIO_11_0_CMD);
			status = pr.waitFor();	
		} finally {
			if (pr != null) {
				ProcessUtil.destroy(pr);
			}
		}
		return status;
	}
	
	private static int exec5021Gpio111() throws IOException, InterruptedException {
		int status;
		SafeProcess pr = null;
		try {
			pr = ProcessUtil.exec(RELIAGATE_50_21_GPIO_11_1_CMD);
			status = pr.waitFor();	
		} finally {
			if (pr != null) {
				ProcessUtil.destroy(pr);
			}
		}
		return status;
	}
	
	private static int exec5021Gpio6() throws IOException, InterruptedException {
		int status;
		SafeProcess pr = null;
		try {
			pr = ProcessUtil.exec(RELIAGATE_50_21_GPIO_6_CMD);
			status = pr.waitFor();	
		} finally {
			if (pr != null) {
				ProcessUtil.destroy(pr);
			}
		}
		return status;
	}
}
