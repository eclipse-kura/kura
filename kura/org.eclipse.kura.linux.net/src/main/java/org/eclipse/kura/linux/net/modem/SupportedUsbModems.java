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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.linux.util.ProcessStats;
import org.eclipse.kura.core.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedUsbModems {

    private static final Logger s_logger = LoggerFactory.getLogger(SupportedUsbModems.class);

    private static class LsusbEntry {

        private final String m_bus;
        private final String m_device;
        private final String m_vendor;
        private final String m_product;
        private String m_description;

        private LsusbEntry(String bus, String device, String vendor, String product) {
            this.m_bus = bus;
            this.m_device = device;
            this.m_vendor = vendor;
            this.m_product = product;
        }

        private LsusbEntry(String bus, String device, String vendor, String product, String description) {
            this(bus, device, vendor, product);
            this.m_description = description;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("USB Modem :-> ");
            sb.append("Bus ").append(this.m_bus).append(" Device ").append(this.m_device).append(" ID ")
                    .append(this.m_vendor).append(':').append(this.m_product);
            if (this.m_description != null) {
                sb.append(" - ").append(this.m_description);
            }
            return sb.toString();
        }
    }

    static {
        List<LsusbEntry> lsusbEntries = null;
        try {
            lsusbEntries = getLsusbInfo();
        } catch (Exception e1) {
            s_logger.error("failed to obtain lsusb information - {}", e1);
        }
        for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
            try {
                if (isAttached(modem.getVendorId(), modem.getProductId(), lsusbEntries)) {
                    // modprobe driver
                    s_logger.info("The {}:{} USB modem device is attached", modem.getVendorId(), modem.getProductId());
                    List<? extends UsbModemDriver> drivers = modem.getDeviceDrivers();
                    for (UsbModemDriver driver : drivers) {
                        driver.install();
                    }
                }
            } catch (Exception e) {
                s_logger.error("Failed to attach modem", e);
            }
        }
    }

    public static SupportedUsbModemInfo getModem(String vendorId, String productId, String productName) {
        if (vendorId == null || productId == null) {
            return null;
        }

        for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
            if (vendorId.equals(modem.getVendorId()) && productId.equals(modem.getProductId())
                    && (modem.getProductName().isEmpty() || productName.equals(modem.getProductName()))) {
                return modem;
            }
        }

        return null;
    }

    public static boolean isSupported(String vendorId, String productId, String productName) {
        return SupportedUsbModems.getModem(vendorId, productId, productName) != null;
    }

    public static boolean isAttached(String vendor, String product) throws Exception {
        boolean attached = false;
        String lsusbCmd = formLsusbCommand(vendor, product); // e.g. lsusb -d 1bc7:1010
        BufferedReader br = null;
        InputStreamReader isr = null;
        ProcessStats processStats = null;
        try {
            processStats = LinuxProcessUtil.startWithStats(lsusbCmd);
            isr = new InputStreamReader(processStats.getInputStream());
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                LsusbEntry lsusbEntry = getLsusbEntry(line);
                if (lsusbEntry != null && vendor != null && product != null && vendor.equals(lsusbEntry.m_vendor)
                        && product.equals(lsusbEntry.m_product)) {
                    s_logger.info("The '{}' command detected {}", lsusbCmd, lsusbEntry);
                    attached = true;
                    break;
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (processStats != null) {
                ProcessUtil.destroy(processStats.getProcess());
            }
        }

        return attached;
    }

    private static boolean isAttached(String vendor, String product, List<LsusbEntry> lsusbEntries) throws Exception {
        boolean attached = false;
        if (lsusbEntries == null || lsusbEntries.isEmpty()) {
            attached = isAttached(vendor, product);
        } else {
            for (LsusbEntry lsusbEntry : lsusbEntries) {
                if (vendor != null && product != null && vendor.equals(lsusbEntry.m_vendor)
                        && product.equals(lsusbEntry.m_product)) {
                    s_logger.info("The 'lsusb' command detected {}", lsusbEntry);
                    attached = true;
                    break;
                }
            }
        }
        return attached;
    }

    private static List<LsusbEntry> getLsusbInfo() throws Exception {
        List<LsusbEntry> lsusbEntries = new ArrayList<LsusbEntry>();
        ProcessStats processStats = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            processStats = LinuxProcessUtil.startWithStats("lsusb");
            isr = new InputStreamReader(processStats.getInputStream());
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                LsusbEntry lsusbEntry = getLsusbEntry(line);
                if (lsusbEntry != null) {
                    lsusbEntries.add(lsusbEntry);
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (processStats != null) {
                ProcessUtil.destroy(processStats.getProcess());
            }
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

    private static String formLsusbCommand(String vendor, String product) {
        StringBuffer sb = new StringBuffer();
        sb.append("lsusb -d ").append(vendor).append(":").append(product);
        return sb.toString();
    }
}