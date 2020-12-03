/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc 
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedUsbModems {

    private static final Logger logger = LoggerFactory.getLogger(SupportedUsbModems.class);

    private static class LsusbEntry {

        private final String bus;
        private final String device;
        private final String vendor;
        private final String product;
        private String description;

        private LsusbEntry(String bus, String device, String vendor, String product) {
            this.bus = bus;
            this.device = device;
            this.vendor = vendor;
            this.product = product;
        }

        private LsusbEntry(String bus, String device, String vendor, String product, String description) {
            this(bus, device, vendor, product);
            this.description = description;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("USB Modem :-> ");
            sb.append("Bus ").append(this.bus).append(" Device ").append(this.device).append(" ID ").append(this.vendor)
                    .append(':').append(this.product);
            if (this.description != null) {
                sb.append(" - ").append(this.description);
            }
            return sb.toString();
        }
    }

    private SupportedUsbModems() {
    }

    public static void installModemDrivers(CommandExecutorService executorService) {
        List<LsusbEntry> lsusbEntries = null;
        try {
            lsusbEntries = getLsusbInfo(executorService);
        } catch (IOException e) {
            logger.error("failed to obtain lsusb information", e);
        }
        for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
            try {
                if (isAttached(modem.getVendorId(), modem.getProductId(), lsusbEntries)) {
                    // modprobe driver
                    logger.info("The {}:{} USB modem device is attached", modem.getVendorId(), modem.getProductId());
                    List<? extends UsbModemDriver> drivers = modem.getDeviceDrivers();
                    for (UsbModemDriver driver : drivers) {
                        driver.install(executorService);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to attach modem", e);
            }
        }
    }

    @Deprecated
    public static SupportedUsbModemInfo getModem(String vendorId, String productId) {
        if (vendorId == null || productId == null) {
            return null;
        }

        return getModemInternal(vendorId, productId, "");
    }

    @Deprecated
    public static SupportedUsbModemInfo getModem(String vendorId, String productId, String productName) {
        if (vendorId == null || productId == null) {
            return null;
        }

        return getModemInternal(vendorId, productId, productName);
    }

    public static SupportedUsbModemInfo getModem(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return null;
        }

        return getModemInternal(usbDevice.getVendorId(), usbDevice.getProductId(), usbDevice.getProductName());
    }

    private static SupportedUsbModemInfo getModemInternal(String vendorId, String productId, String productName) {
        for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
            if (vendorId.equals(modem.getVendorId()) && productId.equals(modem.getProductId())
                    && (modem.getProductName().isEmpty() || productName.equals(modem.getProductName()))) {
                return modem;
            }
        }

        return null;
    }

    @Deprecated
    public static boolean isSupported(String vendorId, String productId, String productName) {
        return SupportedUsbModems.getModem(vendorId, productId, productName) != null;
    }

    public static boolean isSupported(UsbDevice usbDevice) {
        return SupportedUsbModems.getModem(usbDevice) != null;
    }

    @Deprecated
    public static boolean isAttached(String vendor, String product, CommandExecutorService executorService)
            throws IOException {
        boolean retVal = false;
        if (vendor == null || product == null) {
            return retVal;
        }
        List<LsusbEntry> lsusbEntries = getLsusbInfo(executorService);
        if (!lsusbEntries.isEmpty()) {
            for (LsusbEntry lsusbEntry : lsusbEntries) {
                if (vendor.equals(lsusbEntry.vendor) && product.equals(lsusbEntry.product)) {
                    retVal = true;
                    break;
                }
            }
        }
        return retVal;
    }

    public static boolean isAttached(UsbModemDevice usbModemDevice) throws IOException {
        if (usbModemDevice == null) {
            return false;
        }

        StringBuilder usbFilePath = new StringBuilder("/sys/bus/usb/devices/").append(usbModemDevice.getUsbPort())
                .append("/");
        return readUsbConfig(usbFilePath.toString() + "idVendor", usbModemDevice.getVendorId())
                && readUsbConfig(usbFilePath.toString() + "idProduct", usbModemDevice.getProductId());
    }

    private static boolean readUsbConfig(String usbConfigFilename, String usbConfigValue) {
        boolean retVal = false;
        try (BufferedReader br = new BufferedReader(new FileReader(new File(usbConfigFilename)))) {
            if (br.readLine().equals(usbConfigValue)) {
                retVal = true;
            }
        } catch (IOException e) {
            return retVal;
        }
        return retVal;
    }

    @Deprecated
    private static boolean isAttached(String vendor, String product, List<LsusbEntry> lsusbEntries,
            CommandExecutorService executorService) throws IOException {
        boolean attached = false;
        if (lsusbEntries == null || lsusbEntries.isEmpty()) {
            attached = isAttached(vendor, product, executorService);
        } else {
            return isAttached(vendor, product, lsusbEntries);
        }
        return attached;
    }

    private static boolean isAttached(String vendor, String product, List<LsusbEntry> lsusbEntries) throws IOException {
        boolean attached = false;
        if (lsusbEntries != null) {
            for (LsusbEntry lsusbEntry : lsusbEntries) {
                if (vendor != null && product != null && vendor.equals(lsusbEntry.vendor)
                        && product.equals(lsusbEntry.product)) {
                    logger.info("The 'lsusb' command detected {}", lsusbEntry);
                    attached = true;
                    break;
                }
            }
        }
        return attached;
    }

    /**
     * Execute command and return splitted lines
     *
     * @param command
     *            the command to execute
     * @return the lines output by the command
     * @throws IOException
     *             if executing the commands fails
     */
    private static List<String> execute(final String commandLine, CommandExecutorService executorService)
            throws IOException {
        Command command = new Command(commandLine.split("\\s+"));
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());

        CommandStatus status = executorService.execute(command);
        logger.debug("Called {} - rc = {}", commandLine, status.getExitStatus().getExitCode());

        return IOUtils
                .readLines(new ByteArrayInputStream(((ByteArrayOutputStream) command.getOutputStream()).toByteArray()));
    }

    private static List<LsusbEntry> getLsusbInfo(CommandExecutorService executorService) throws IOException {
        final List<LsusbEntry> lsusbEntries = new ArrayList<>();

        for (final String line : execute("lsusb", executorService)) {
            lsusbEntries.add(getLsusbEntry(line));
        }
        return lsusbEntries;
    }

    private static LsusbEntry getLsusbEntry(String line) {
        String[] tokens = line.split("\\s+");
        String bus = tokens[1];
        String device = tokens[3];
        device = device.substring(0, device.length() - 1);
        String[] vp = tokens[5].split(":");
        String vendor = vp[0];
        String product = vp[1];
        LsusbEntry lsusbEntry;
        if (tokens.length > 6) {
            StringBuilder description = new StringBuilder();
            for (int i = 6; i < tokens.length; i++) {
                description.append(tokens[i]);
                description.append(' ');
            }
            lsusbEntry = new LsusbEntry(bus, device, vendor, product, description.toString().trim());
        } else {
            lsusbEntry = new LsusbEntry(bus, device, vendor, product);
        }
        return lsusbEntry;
    }
}