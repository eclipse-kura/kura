package org.eclipse.kura.linux.net.modem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

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
			s_logger.info("turnOff() :: turning modem OFF ... attempts left: {}", remainingAttempts);
			if(TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
				toggleGpio65();
			} else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_20.getTargetName())) {
				FileWriter fw = new FileWriter("/sys/class/gpio/usb-rear-pwr/value");
				fw.write("0");
				fw.close();
			} else if (TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName())) {
				SafeProcess pr = ProcessUtil.exec("/usr/sbin/vector-j21-gpio 11 0");
				int status = pr.waitFor();
				s_logger.info("turnOff() :: '/usr/sbin/vector-j21-gpio 11 0' returned {}", status);
				if (status != 0) {
					continue;
				}
				sleep(1000);

				pr = ProcessUtil.exec("/usr/sbin/vector-j21-gpio 11 1");
				status = pr.waitFor();
				s_logger.info("turnOff() :: '/usr/sbin/vector-j21-gpio 11 1' returned {}", status);
				if (status != 0) {
					continue;
				}
				sleep(3000);

				pr = ProcessUtil.exec("/usr/sbin/vector-j21-gpio 11 0");
				status = pr.waitFor();
				s_logger.info("turnOff() :: '/usr/sbin/vector-j21-gpio 11 0' returned {}", status);
				retVal = (status == 0) ? true : false;
			} else {
				s_logger.warn("turnOff() :: modem turnOff operation is not supported for the {} platform", TARGET_NAME);
			}
			remainingAttempts--;
			sleep(5000);
		} while (isOn());

		s_logger.info("turnOff() :: Modem is OFF? - {}", retVal);
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
			s_logger.info("turnOn() :: turning modem ON ... attempts left: {}", remainingAttempts);
			if(TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
				toggleGpio65();
			} else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_20.getTargetName())) {
				FileWriter fw = new FileWriter("/sys/class/gpio/usb-rear-pwr/value");
				fw.write("1");
				fw.close();
			} else if (TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName())) {
				SafeProcess pr = ProcessUtil.exec("/usr/sbin/vector-j21-gpio 11 1");
				int status = pr.waitFor();
				s_logger.info("turnOn() :: '/usr/sbin/vector-j21-gpio 11 1' returned {}", status);
				if (status != 0) {
					continue;
				}
				sleep(1000);

				pr = ProcessUtil.exec("/usr/sbin/vector-j21-gpio 6");
				status = pr.waitFor();
				s_logger.info("turnOn() :: '/usr/sbin/vector-j21-gpio 6' returned {}", status);
				retVal = (status == 0) ? true : false;
			} else {
				s_logger.warn("turnOn() :: modem turnOn operation is not supported for the {} platform", TARGET_NAME);
			}
			remainingAttempts--;
			sleep(10000);
		} while (!isOn());

		s_logger.info("turnOn() :: Modem is ON? - {}", retVal);
		return retVal;
	}
	
	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}
	
	protected void toggleGpio65() throws Exception { 		
		File fgpio65Folder = new File ("/sys/class/gpio/gpio65");
		if (!fgpio65Folder.exists()) {
			BufferedWriter bwGpioSelect = new BufferedWriter(new FileWriter("/sys/class/gpio/export"));
			bwGpioSelect.write("65");
			bwGpioSelect.flush();
			bwGpioSelect.close();
		}

		BufferedWriter bwGpio65Direction = new BufferedWriter(new FileWriter("/sys/class/gpio/gpio65/direction"));
		bwGpio65Direction.write("out");
		bwGpio65Direction.flush();
		bwGpio65Direction.close();

		BufferedWriter fGpio65Value = new BufferedWriter(new FileWriter("/sys/class/gpio/gpio65/value"));
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
}
