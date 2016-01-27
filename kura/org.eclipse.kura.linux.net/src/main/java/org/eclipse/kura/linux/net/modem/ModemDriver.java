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
	private static final String GPIO_EXPORT_PATH = "/sys/class/gpio/export";
	
	private static final String RELIAGATE_10_20_GPIO_PATH = "/sys/class/gpio/usb-rear-pwr/value";
	private static final String RELIAGATE_50_21_GPIO_11_0_CMD = "/usr/sbin/vector-j21-gpio 11 0";
	private static final String RELIAGATE_50_21_GPIO_11_1_CMD = "/usr/sbin/vector-j21-gpio 11 1"; 
	private static final String RELIAGATE_50_21_GPIO_6_CMD = "/usr/sbin/vector-j21-gpio 6";
	
	
	
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
				toggleGpio65();
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
				toggleGpio65();
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
	
	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}
	
	protected void toggleGpio65() throws IOException { 		
		File fgpio65Folder = new File (GPIO_65_PATH);
		if (!fgpio65Folder.exists()) {
			BufferedWriter bwGpioSelect = new BufferedWriter(new FileWriter(GPIO_EXPORT_PATH));
			bwGpioSelect.write("65");
			bwGpioSelect.flush();
			bwGpioSelect.close();
		}

		BufferedWriter bwGpio65Direction = new BufferedWriter(new FileWriter(GPIO_65_DIRECTION_PATH));
		bwGpio65Direction.write("out");
		bwGpio65Direction.flush();
		bwGpio65Direction.close();

		BufferedWriter fGpio65Value = new BufferedWriter(new FileWriter(GPIO_65_VALUE_PATH));
		fGpio65Value.write("0");
		fGpio65Value.flush();
		fGpio65Value.write("1");
		fGpio65Value.flush();
		sleep(5000);
		fGpio65Value.write("0");
		fGpio65Value.flush();
		fGpio65Value.close();
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
