/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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

    private static final Logger logger = LoggerFactory.getLogger(ModemDriver.class);

    private static final String TARGET_NAME = System.getProperty("target.device");
    private static final String GPIO_PATH = "/sys/class/gpio";
    private static final String GPIO_EXPORT_PATH = GPIO_PATH + "/export";
    private static final String BASE_GPIO_PATH = "/sys/class/gpio/gpio";
    private static final String GPIO_DIRECTION_SUFFIX_PATH = "/direction";
    private static final String GPIO_VALUE_SUFFIX_PATH = "/value";

    private static final String GPIO_INDEX_60 = "60";
    private static final String GPIO_INDEX_65 = "65";

    private static final String RELIAGATE_10_20_GPIO_PATH = "/sys/class/gpio/usb-rear-pwr/value";
    private static final String RELIAGATE_50_21_GPIO_11_0_CMD = "/usr/sbin/vector-j21-gpio 11 0";
    private static final String RELIAGATE_50_21_GPIO_11_1_CMD = "/usr/sbin/vector-j21-gpio 11 1";
    private static final String RELIAGATE_50_21_GPIO_6_CMD = "/usr/sbin/vector-j21-gpio 6";

    private static final String RELIAGATE_10_05_GSM_RESET_GPIO_NUM = "252";
    private static final String RELIAGATE_10_05_GSM_USB_PATH = "/sys/bus/usb/devices/usb2/authorized";

    private static final String BOLTGATE_20_25_PCIEX_SLOT3_POWER_GPIO_VALUE = "/dev/pciex_slot3_power/value";
    private static final String BOLTGATE_20_25_PCIEX_W_DISABLE3_GPIO_VALUE = "/dev/pciex_w_disable3/value";

    private static final String GPIO_DIRECTION = "out";

    private static final String FAILED_INITIALIZE_GPIO_MSG = "Failed to initialize GPIO {}";

    private static int baseGpio;

    static {
        if (TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
            String gpioIndex = GPIO_INDEX_65;
            String gpioPath = BASE_GPIO_PATH + gpioIndex;
            String gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
            try {
                exportGpio(gpioIndex, gpioPath); // Prepare gpios
                setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
            } catch (IOException e) {
                logger.warn(FAILED_INITIALIZE_GPIO_MSG, gpioIndex, e);
            }
        } else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
            String gpioIndex = GPIO_INDEX_60;
            String gpioPath = BASE_GPIO_PATH + gpioIndex;
            String gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
            try {
                exportGpio(gpioIndex, gpioPath); // Prepare gpios
                if (!GPIO_DIRECTION.equals(getGpioDirection(gpioDirectionPath))) {
                    setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
                    String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    // we need to invert GPIO pin value to turn the modem on
                    // since setting pin direction
                    // turns RG 10-11-36 off.
                    invertGpioValue(gpioValuePath);
                }
            } catch (IOException e) {
                logger.warn(FAILED_INITIALIZE_GPIO_MSG, gpioIndex, e);
            }
        } else if (TARGET_NAME.equals(KuraConstants.Reliagate_20_25.getTargetName())) {
            baseGpio = -1;
            try {
                baseGpio = getBaseGpio();
            } catch (KuraException e) {
                logger.warn("BaseGpio fetch failed!", e);
            }

            if (baseGpio != -1) {
                // export gpio for internal modem. (Modem Power)
                exportGpio(30);

                // export gpio for internal modem. (pci-ex slot 3)
                exportGpio(23);

                // export gpio for external modem. (J9 usb port)
                exportGpio(35);
            }
        }
    }

    private static void exportGpio(int offset) {
        String gpioIndex = Integer.toString(baseGpio + offset);
        String gpioPath = BASE_GPIO_PATH + gpioIndex;
        String gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
        try {
            exportGpio(gpioIndex, gpioPath); // Prepare gpios
            setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
        } catch (IOException e) {
            logger.warn(FAILED_INITIALIZE_GPIO_MSG, gpioIndex, e);
        }
    }

    public boolean turnModemOff() throws Exception {
        if (TARGET_NAME == null) {
            return false;
        }
        boolean retVal = true;
        int remainingAttempts = 3;
        final long turnOffDelay = getTurnOffDelay();
        while (isOn()) {
            if (remainingAttempts <= 0) {
                retVal = false;
                break;
            }
            logger.info("turnModemOff() :: turning modem OFF ... attempts left: {}", remainingAttempts);
            if (TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
                String gpioIndex = GPIO_INDEX_65;
                String gpioPath = BASE_GPIO_PATH + gpioIndex;
                String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                toggleGpio(gpioValuePath);
            } else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
                String gpioIndex = GPIO_INDEX_60;
                String gpioPath = BASE_GPIO_PATH + gpioIndex;
                String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                invertGpioValue(gpioValuePath);
            } else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_20.getTargetName())) {
                disable1020Gpio();
            } else if (TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName())) {
                int status = exec5021Gpio110();
                logger.info("turnModemOff() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_0_CMD, status);
                if (status != 0) {
                    continue;
                }
                sleep(1000);

                status = exec5021Gpio111();
                logger.info("turnModemOff() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_1_CMD, status);
                if (status != 0) {
                    continue;
                }
                sleep(3000);

                status = exec5021Gpio110();
                logger.info("turnModemOff() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_0_CMD, status);
                retVal = status == 0 ? true : false;
            } else if (TARGET_NAME.equals(KuraConstants.Reliagate_20_25.getTargetName())) {
                // TODO: make resets more smart, based on the effective modem
                // that has to be stopped/started

                if (baseGpio != -1) {
                    // invert gpio value for internal modem. (Modem Power)
                    String gpioIndex = Integer.toString(baseGpio + 30);
                    String gpioPath = BASE_GPIO_PATH + gpioIndex;
                    String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    invertGpioValue(gpioValuePath);

                    // invert gpio value for internal modem. (pci-ex slot 3)
                    gpioIndex = Integer.toString(baseGpio + 23);
                    gpioPath = BASE_GPIO_PATH + gpioIndex;
                    gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    invertGpioValue(gpioValuePath);

                    // invert gpio value for external modem. (J9 usb port)
                    gpioIndex = Integer.toString(baseGpio + 35);
                    gpioPath = BASE_GPIO_PATH + gpioIndex;
                    gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    invertGpioValue(gpioValuePath);
                }
            } else if (TARGET_NAME.equalsIgnoreCase(KuraConstants.BoltGATE_20_25.getTargetName())) {

                turnOffGpio(BOLTGATE_20_25_PCIEX_SLOT3_POWER_GPIO_VALUE);

            } else {
                logger.warn("turnModemOff() :: modem turnOff operation is not supported for the {} platform",
                        TARGET_NAME);
                retVal = true;
                break;
            }
            remainingAttempts--;
            logger.info("turnModemOff() :: sleeping for {} ms after modem shutdown", turnOffDelay);
            sleep(turnOffDelay);
        }

        logger.info("turnModemOff() :: Modem is OFF? - {}", retVal);
        return retVal;
    }

    public boolean turnModemOn() throws Exception {
        if (TARGET_NAME == null) {
            return false;
        }
        boolean retVal = true;
        int remainingAttempts = 5;
        while (!isOn()) {
            if (remainingAttempts <= 0) {
                retVal = false;
                break;
            }
            logger.info("turnModemOn() :: turning modem ON ... attempts left: {}", remainingAttempts);
            if (TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
                String gpioIndex = GPIO_INDEX_65;
                String gpioPath = BASE_GPIO_PATH + gpioIndex;
                String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                toggleGpio(gpioValuePath);
            } else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
                String gpioIndex = GPIO_INDEX_60;
                String gpioPath = BASE_GPIO_PATH + gpioIndex;
                String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                invertGpioValue(gpioValuePath);
                sleep(5000);
            } else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_20.getTargetName())) {
                enable1020Gpio();
            } else if (TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName())) {
                int status = exec5021Gpio111();
                logger.info("turnModemOn() :: '{}' returned {}", RELIAGATE_50_21_GPIO_11_1_CMD, status);
                if (status != 0) {
                    continue;
                }
                sleep(1000);

                status = exec5021Gpio6();
                logger.info("turnModemOn() :: '{}' returned {}", RELIAGATE_50_21_GPIO_6_CMD, status);
                retVal = status == 0 ? true : false;
            } else if (TARGET_NAME.equals(KuraConstants.Reliagate_20_25.getTargetName())) {
                if (baseGpio != -1) {
                    // invert gpio value for internal modem. (Modem Power)
                    String gpioIndex = Integer.toString(baseGpio + 30);
                    String gpioPath = BASE_GPIO_PATH + gpioIndex;
                    String gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    invertGpioValue(gpioValuePath);

                    // invert gpio value for internal modem. (pci-ex slot 3)
                    gpioIndex = Integer.toString(baseGpio + 23);
                    gpioPath = BASE_GPIO_PATH + gpioIndex;
                    gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    invertGpioValue(gpioValuePath);

                    // invert gpio value for external modem. (J9 usb port)
                    gpioIndex = Integer.toString(baseGpio + 35);
                    gpioPath = BASE_GPIO_PATH + gpioIndex;
                    gpioValuePath = gpioPath + GPIO_VALUE_SUFFIX_PATH;
                    invertGpioValue(gpioValuePath);
                }
            } else if (TARGET_NAME.equalsIgnoreCase(KuraConstants.BoltGATE_20_25.getTargetName())) {

                turnOnGpio(BOLTGATE_20_25_PCIEX_SLOT3_POWER_GPIO_VALUE);

                Thread.sleep(1000);

                toggleGpio(BOLTGATE_20_25_PCIEX_W_DISABLE3_GPIO_VALUE);

            } else {
                logger.warn("turnModemOn() :: modem turnOn operation is not supported for the {} platform",
                        TARGET_NAME);
                retVal = true;
                break;
            }
            remainingAttempts--;
            sleep(10000);
        }

        logger.info("turnModemOn() :: Modem is ON? - {}", retVal);
        return retVal;
    }

    public boolean resetModem() {
        boolean retVal = false;
        if (KuraConstants.ReliaGATE_10_05.getTargetName().equals(TARGET_NAME)) {
            // just pulse the modem reset pin
            try {
                if (!isSysfsGpioExported(RELIAGATE_10_05_GSM_RESET_GPIO_NUM)) {
                    initSysfsGpio(RELIAGATE_10_05_GSM_RESET_GPIO_NUM, false);
                }
                echoSysfsResource(RELIAGATE_10_05_GSM_USB_PATH, false);
                Thread.sleep(1000);
                pulseSysfsGpio(true, 1000);
                Thread.sleep(1000);
                echoSysfsResource(RELIAGATE_10_05_GSM_USB_PATH, true);

                // wait until the modem is on again
                int cnt = 10;
                while (!isOn() && cnt > 0) {
                    sleep(1000);
                    cnt--;
                }
                if (isOn()) {
                    retVal = true;
                }
            } catch (IOException e) {
                logger.error("Failed to write to gpio", e);
            } catch (KuraException e) {
                logger.error("Failed to detect modem", e);
            } catch (InterruptedException e) {
                logger.error("Interrupted Exception during sleep", e);
            }
        } else {
            logger.warn("resetModem() :: modem reset operation is not supported for the {} platform", TARGET_NAME);
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

    protected boolean isSysfsGpioExported(String gpioNum) {
        boolean exported = false;
        final String gpioPath = BASE_GPIO_PATH + RELIAGATE_10_05_GSM_RESET_GPIO_NUM;
        File fgpioFolder = new File(gpioPath);
        if (fgpioFolder.exists()) {
            exported = true;
        }
        return exported;
    }

    protected void initSysfsGpio(String gpioNum, boolean level) throws IOException {
        final String gpioPath = BASE_GPIO_PATH + RELIAGATE_10_05_GSM_RESET_GPIO_NUM;
        BufferedWriter bwGpioSelect = new BufferedWriter(new FileWriter(GPIO_EXPORT_PATH));
        try {
            bwGpioSelect.write(gpioNum);
            bwGpioSelect.flush();
        } finally {
            bwGpioSelect.close();
        }

        BufferedWriter bwGpioDirection = new BufferedWriter(new FileWriter(gpioPath + GPIO_DIRECTION_SUFFIX_PATH));
        try {
            bwGpioDirection.write("out");
            bwGpioDirection.flush();
        } finally {
            bwGpioDirection.close();
        }

        BufferedWriter fGpioValue = new BufferedWriter(new FileWriter(gpioPath + GPIO_VALUE_SUFFIX_PATH));
        try {
            fGpioValue.write(level ? "1" : "0");
            fGpioValue.flush();
        } finally {
            fGpioValue.close();
        }
    }

    protected void pulseSysfsGpio(boolean level, long duration) throws IOException {
        final String gpioPath = BASE_GPIO_PATH + RELIAGATE_10_05_GSM_RESET_GPIO_NUM;
        BufferedWriter fGpioValue = new BufferedWriter(new FileWriter(gpioPath + GPIO_VALUE_SUFFIX_PATH));
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

    private void toggleGpio(String valuePath) throws IOException, InterruptedException {
        try {
            invertGpioValue(valuePath);
            Thread.sleep(5000);
            invertGpioValue(valuePath);
        } catch (InterruptedException e) {
            logger.warn("Exception in the inversion process. {}", e.getMessage());
            throw e;
        }
    }

    private static void invertGpioValue(String valuePath) throws IOException {
        FileReader fGpioOldValueReader = null; // read current value
        BufferedReader fGpioOldValue = null;
        int oldValue = 0;
        try {
            fGpioOldValueReader = new FileReader(valuePath);
            fGpioOldValue = new BufferedReader(fGpioOldValueReader);
            oldValue = Integer.parseInt(fGpioOldValue.readLine());
        } catch (Exception e) {
            logger.debug("Error while trying to read gpio value", e);
        } finally {
            if (fGpioOldValue != null) {
                fGpioOldValue.close();
            }
            if (fGpioOldValueReader != null) {
                fGpioOldValueReader.close();
            }
        }

        try (FileWriter gpioValueWriter = new FileWriter(valuePath);
                BufferedWriter fGpioValue = new BufferedWriter(gpioValueWriter)) {
            int triggerValue = (oldValue + 1) % 2;

            // TODO: verify if this write can be removed
            fGpioValue.write(Integer.toString(oldValue));
            fGpioValue.flush();

            fGpioValue.write(Integer.toString(triggerValue));
            fGpioValue.flush();
        }
    }

    private static void setGpioDirection(String directionPath, String direction) throws IOException {
        try (FileWriter directionFileWriter = new FileWriter(directionPath);
                BufferedWriter bwGpioDirection = new BufferedWriter(directionFileWriter)) {
            bwGpioDirection.write(direction);
            bwGpioDirection.flush();
        } catch (Exception e) {
            logger.debug("Error while trying to write gpio direction", e);
        }
    }

    private static String getGpioDirection(String directionPath) throws IOException {
        String direction = null;
        try (FileReader directionFileReader = new FileReader(directionPath);
                BufferedReader brGpioDirection = new BufferedReader(directionFileReader)) {
            direction = brGpioDirection.readLine();
        } catch (Exception e) {
            logger.debug("Error while trying to get gpio direction", e);
        }
        return direction;
    }

    private static void exportGpio(String gpio, String gpioPath) throws IOException {
        File fgpioFolder = new File(gpioPath);
        if (!fgpioFolder.exists()) {
            try (FileWriter fwGpioExportWriter = new FileWriter(GPIO_EXPORT_PATH);
                    BufferedWriter bwGpioSelect = new BufferedWriter(fwGpioExportWriter)) {
                bwGpioSelect.write(gpio);
                bwGpioSelect.flush();
            }
        }
    }

    protected void echoSysfsResource(String resource, boolean level) throws IOException {
        BufferedWriter resourceValue = new BufferedWriter(new FileWriter(resource));
        try {
            resourceValue.write(level ? "1" : "0");
            resourceValue.flush();
            sleep(1000);
        } finally {
            resourceValue.close();
        }
    }

    private SupportedUsbModemInfo getSupportedUsbModemInfo() {
        if (!(this instanceof UsbModemDriver)) {
            return null;
        }

        final UsbModemDriver self = (UsbModemDriver) this;
        return SupportedUsbModems.getModem(self.getVendor(), self.getProduct());
    }

    private long getTurnOffDelay() {
        final SupportedUsbModemInfo usbModemInfo = getSupportedUsbModemInfo();
        if (usbModemInfo != null) {
            return usbModemInfo.getTurnOffDelay();
        }
        return 5000;
    }

    private boolean isOn() throws KuraException {
        boolean isModemOn;
        if (this instanceof UsbModemDriver) {
            try {
                isModemOn = SupportedUsbModems.isAttached(((UsbModemDriver) this).getVendor(),
                        ((UsbModemDriver) this).getProduct());
                logger.info("isOn() :: USB modem attached? {}", isModemOn);
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Error executing lsusb command");
            }
        } else if (this instanceof SerialModemDriver) {
            isModemOn = ((SerialModemDriver) this).isReachable();
            logger.info("isOn() :: Serial modem reachable? {}", isModemOn);
        } else {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "Unsupported modem device");
        }
        return isModemOn;
    }

    private static void turnOffGpio(String valuePath) throws IOException {
        try (FileWriter fw = new FileWriter(valuePath)) {
            fw.write("0");
            fw.flush();
        }
    }

    private static void turnOnGpio(String valuePath) throws IOException {
        try (FileWriter fw = new FileWriter(valuePath)) {
            fw.write("1");
            fw.flush();
        }
    }

    private static void disable1020Gpio() throws IOException {
        try (FileWriter fw = new FileWriter(RELIAGATE_10_20_GPIO_PATH)) {
            fw.write("0");
        }
    }

    private static void enable1020Gpio() throws IOException {
        try (FileWriter fw = new FileWriter(RELIAGATE_10_20_GPIO_PATH)) {
            fw.write("1");
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

    private static int getBaseGpio() throws KuraException {
        String baseFolder = GPIO_PATH + "/";
        File[] files = new File(baseFolder).listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith("gpiochip")) {
                File gpioChipLabelFile = new File(baseFolder + file.getName() + "/label");
                File gpioChipBaseFile = new File(baseFolder + file.getName() + "/base");
                try {
                    String fileContent = readFile(gpioChipLabelFile);

                    if (fileContent != null && fileContent.contains("eth-cortex-usb")) {
                        String baseValue = readFile(gpioChipBaseFile);
                        return Integer.parseInt(baseValue);
                    }
                } catch (IOException e) {
                    logger.warn("Exception while opening gpiochip file.", e);
                    throw new KuraException(KuraErrorCode.GPIO_EXCEPTION);
                } catch (NumberFormatException e) {
                    logger.warn("Exception while opening gpiochip base file.", e);
                    throw new KuraException(KuraErrorCode.GPIO_EXCEPTION);
                }
            }
        }
        return -1;
    }

    private static String readFile(File gpioChipFile) throws IOException {
        try (FileReader fr = new FileReader(gpioChipFile); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
