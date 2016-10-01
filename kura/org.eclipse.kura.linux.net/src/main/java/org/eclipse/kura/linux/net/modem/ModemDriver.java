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

    private static final Logger s_logger = LoggerFactory.getLogger(ModemDriver.class);

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

    private static final String GPIO_DIRECTION = "out";

    private static int baseGpio;

    static {
        if (TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
            String gpioIndex = GPIO_INDEX_65;
            String gpioPath = BASE_GPIO_PATH + gpioIndex;
            String gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
            try {
                exportGpio(gpioIndex, gpioPath);  // Prepare gpios
                setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
            } catch (IOException e) {
                s_logger.warn("Failed to initialize GPIO {}", gpioIndex, e);
            }
        } else if (TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
            String gpioIndex = GPIO_INDEX_60;
            String gpioPath = BASE_GPIO_PATH + gpioIndex;
            String gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
            try {
                exportGpio(gpioIndex, gpioPath);  // Prepare gpios
                setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
            } catch (IOException e) {
                s_logger.warn("Failed to initialize GPIO {}", gpioIndex, e);
            }
        } else if (TARGET_NAME.equals(KuraConstants.Reliagate_20_25.getTargetName())) {
            baseGpio = -1;
            try {
                baseGpio = getBaseGpio();
            } catch (KuraException e) {
                s_logger.warn("BaseGpio fetch failed!", e);
            }

            if (baseGpio != -1) {
                // export gpio for internal modem. (Modem Power)
                String gpioIndex = Integer.toString(baseGpio + 30);
                String gpioPath = BASE_GPIO_PATH + gpioIndex;
                String gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
                try {
                    exportGpio(gpioIndex, gpioPath);  // Prepare gpios
                    setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
                } catch (IOException e) {
                    s_logger.warn("Failed to initialize GPIO {}", gpioIndex, e);
                }

                // export gpio for internal modem. (pci-ex slot 3)
                gpioIndex = Integer.toString(baseGpio + 23);
                gpioPath = BASE_GPIO_PATH + gpioIndex;
                gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
                try {
                    exportGpio(gpioIndex, gpioPath);  // Prepare gpios
                    setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
                } catch (IOException e) {
                    s_logger.warn("Failed to initialize GPIO {}", gpioIndex, e);
                }

                // export gpio for external modem. (J9 usb port)
                gpioIndex = Integer.toString(baseGpio + 35);
                gpioPath = BASE_GPIO_PATH + gpioIndex;
                gpioDirectionPath = gpioPath + GPIO_DIRECTION_SUFFIX_PATH;
                try {
                    exportGpio(gpioIndex, gpioPath);  // Prepare gpios
                    setGpioDirection(gpioDirectionPath, GPIO_DIRECTION);
                } catch (IOException e) {
                    s_logger.warn("Failed to initialize GPIO {}", gpioIndex, e);
                }
            }
        }
    }

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
            } else {
                s_logger.warn("turnModemOff() :: modem turnOff operation is not supported for the {} platform",
                        TARGET_NAME);
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
            } else {
                s_logger.warn("turnModemOn() :: modem turnOn operation is not supported for the {} platform",
                        TARGET_NAME);
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
        if (KuraConstants.ReliaGATE_10_05.getTargetName().equals(TARGET_NAME)) {
            // just pulse the modem reset pin
            try {
                if (!isSysfsGpioExported(RELIAGATE_10_05_GSM_RESET_GPIO_NUM)) {
                    initSysfsGpio(RELIAGATE_10_05_GSM_RESET_GPIO_NUM, false);
                }
                echoSysfsResource(RELIAGATE_10_05_GSM_USB_PATH, false);
                Thread.sleep(1000);
                pulseSysfsGpio(RELIAGATE_10_05_GSM_RESET_GPIO_NUM, true, 1000);
                Thread.sleep(1000);
                echoSysfsResource(RELIAGATE_10_05_GSM_USB_PATH, true);

                // wait until the modem is on again
                // isOn uses lsusb that is not supported on the Reliagate 10-05
                // int cnt = 10;
                // while (!isOn() && cnt > 0) {
                // sleep(1000);
                // cnt--;
                // }
                // if (!isOn()) {
                // retVal = false;
                // }
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

    protected void pulseSysfsGpio(String gpioNum, boolean level, long duration) throws IOException {
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
            s_logger.warn("Exception in the inversion process. {}", e.getMessage());
            throw e;
        }
    }

    private void invertGpioValue(String valuePath) throws IOException {
        FileReader fGpioOldValueReader = null;  // read current value
        BufferedReader fGpioOldValue = null;
        int oldValue = 0;
        try {
            fGpioOldValueReader = new FileReader(valuePath);
            fGpioOldValue = new BufferedReader(fGpioOldValueReader);
            oldValue = Integer.parseInt(fGpioOldValue.readLine());
        } catch (Exception e) {
            s_logger.debug("Error while trying to read gpio value", e);
        } finally {
            if (fGpioOldValue != null) {
                fGpioOldValue.close();
            }
            if (fGpioOldValueReader != null) {
                fGpioOldValueReader.close();
            }
        }

        FileWriter gpioValueWriter = null;
        BufferedWriter fGpioValue = null;
        try {
            gpioValueWriter = new FileWriter(valuePath);
            fGpioValue = new BufferedWriter(gpioValueWriter);
            int triggerValue = (oldValue + 1) % 2;

            // TODO: verify if this write can be removed
            fGpioValue.write(Integer.toString(oldValue));
            fGpioValue.flush();

            fGpioValue.write(Integer.toString(triggerValue));
            fGpioValue.flush();
        } finally {
            if (fGpioValue != null) {
                fGpioValue.close();
            }
            if (gpioValueWriter != null) {
                gpioValueWriter.close();
            }
        }
    }

    private static void setGpioDirection(String directionPath, String direction) throws IOException {
        FileWriter directionFileWriter = null;
        BufferedWriter bwGpioDirection = null;
        try {
            directionFileWriter = new FileWriter(directionPath);
            bwGpioDirection = new BufferedWriter(directionFileWriter);
            bwGpioDirection.write(direction);
            bwGpioDirection.flush();
        } catch (Exception e) {
            s_logger.debug("Error while trying to write gpio direction", e);
        } finally {
            if (bwGpioDirection != null) {
                bwGpioDirection.close();
            }
            if (directionFileWriter != null) {
                directionFileWriter.close();
            }
        }
    }

    private static void exportGpio(String gpio, String gpioPath) throws IOException {
        File fgpioFolder = new File(gpioPath);
        if (!fgpioFolder.exists()) {
            FileWriter bwGpioExportWriter = null;
            BufferedWriter bwGpioSelect = null;
            try {
                bwGpioExportWriter = new FileWriter(GPIO_EXPORT_PATH);
                bwGpioSelect = new BufferedWriter(bwGpioExportWriter);
                bwGpioSelect.write(gpio);
                bwGpioSelect.flush();
            } finally {
                if (bwGpioSelect != null) {
                    bwGpioSelect.close();
                }
                if (bwGpioExportWriter != null) {
                    bwGpioExportWriter.close();
                }
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

    private boolean isOn() throws Exception {

        boolean isModemOn;
        if (this instanceof UsbModemDriver) {
            isModemOn = SupportedUsbModems.isAttached(((UsbModemDriver) this).getVendor(),
                    ((UsbModemDriver) this).getProduct());
            s_logger.info("isOn() :: USB modem attached? {}", isModemOn);
        } else if (this instanceof SerialModemDriver) {
            isModemOn = ((SerialModemDriver) this).isReachable();
            s_logger.info("isOn() :: Serial modem reachable? {}", isModemOn);
        } else {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
        }
        return isModemOn;
    }

    private static void disable1020Gpio() throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(RELIAGATE_10_20_GPIO_PATH);
            fw.write("0");
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    private static void enable1020Gpio() throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(RELIAGATE_10_20_GPIO_PATH);
            fw.write("1");
        } finally {
            if (fw != null) {
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
                    s_logger.warn("Exception while opening gpiochip file.", e);
                    throw new KuraException(KuraErrorCode.GPIO_EXCEPTION);
                } catch (NumberFormatException e) {
                    s_logger.warn("Exception while opening gpiochip base file.", e);
                    throw new KuraException(KuraErrorCode.GPIO_EXCEPTION);
                }
            }
        }
        return -1;
    }

    private static String readFile(File gpioChipFile) throws IOException {
        BufferedReader br = null;
        FileReader fr = null;
        try {
            fr = new FileReader(gpioChipFile);
            br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            if (fr != null) {
                fr.close();
            }
            if (br != null) {
                br.close();
            }
        }
    }
}
