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

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.modem.ModemTechnologyType;

public enum SupportedUsbModemInfo {

    // device name, vendor, product, ttyDevs, blockDevs, AT Port, Data Port, GPS Port, technology types, device driver
    Telit_HE910_DG("HE910-DG", "1bc7", "0021", 6, 0, 3, 0, 3,
            Arrays.asList(ModemTechnologyType.HSPA, ModemTechnologyType.UMTS),
            Arrays.asList(new UsbModemDriver("cdc_acm", "1bc7", "0021")), "6 CDC-ACM"), Telit_HE910_D("HE910-D", "1bc7",
                    "0021", 7, 0, 3, 0, 3, Arrays.asList(ModemTechnologyType.HSPA, ModemTechnologyType.UMTS),
                    Arrays.asList(new UsbModemDriver("cdc_acm", "1bc7", "0021")), ""), Telit_GE910("GE910", "1bc7",
                            "0022", 2, 0, 0, 1, 0, Arrays.asList(ModemTechnologyType.GSM_GPRS),
                            Arrays.asList(new UsbModemDriver("cdc_acm", "1bc7", "0022")),
                            ""), Telit_DE910_DUAL("DE910-DUAL", "1bc7", "1010", 4, 0, 2, 3, 1,
                                    Arrays.asList(ModemTechnologyType.EVDO, ModemTechnologyType.CDMA),
                                    Arrays.asList(new De910ModemDriver()),
                                    ""), Telit_LE910("LE910", "1bc7", "1201", 5, 0, 2, 3, 1, Arrays.asList(
                                            ModemTechnologyType.LTE, ModemTechnologyType.HSPA,
                                            ModemTechnologyType.UMTS), Arrays.asList(new Le910ModemDriver()),
                                            ""), Telit_CE910_DUAL("CE910-DUAL", "1bc7", "1011", 2, 0, 1, 1, -1,
                                                    Arrays.asList(ModemTechnologyType.CDMA),
                                                    Arrays.asList(new Ce910ModemDriver()), ""), Sierra_MC8775("MC8775",
                                                            "1199", "6812", 3, 0, 2, 0, -1,
                                                            Arrays.asList(ModemTechnologyType.HSDPA),
                                                            Arrays.asList(new UsbModemDriver("sierra", "1199", "6812")),
                                                            ""), Sierra_MC8790("MC8790", "1199", "683c", 7, 0, 3, 4, -1,
                                                                    Arrays.asList(ModemTechnologyType.HSDPA),
                                                                    Arrays.asList(new UsbModemDriver("sierra", "1199",
                                                                            "683c")),
                                                                    ""), Sierra_USB598("USB598", "1199", "0025", 4, 1,
                                                                            0, 0, -1,
                                                                            Arrays.asList(ModemTechnologyType.EVDO),
                                                                            Arrays.asList(new UsbModemDriver("sierra",
                                                                                    "1199", "0025")),
                                                                            ""), Ublox_SARA_U2("SARA-U2", "1546",
                                                                                    "1102", 7, 0, 1, 0, -1,
                                                                                    Arrays.asList(
                                                                                            ModemTechnologyType.HSPA),
                                                                                    Arrays.asList(new UsbModemDriver(
                                                                                            "cdc_acm", "1546", "1102")),
                                                                                    "");

    private static final String TARGET_NAME = System.getProperty("target.device");

    private String m_deviceName;
    private String m_vendorId;
    private String m_productId;
    private int m_numTtyDevs;
    private int m_numBlockDevs;

    private int m_atPort;
    private int m_dataPort;
    private int m_gpsPort;

    private List<ModemTechnologyType> m_technologyTypes;
    private List<? extends UsbModemDriver> m_deviceDrivers;

    private String m_productName;

    private SupportedUsbModemInfo(String deviceName, String vendorId, String productId, int numTtyDevs,
            int numBlockDevs, int atPort, int dataPort, int gpsPort, List<ModemTechnologyType> modemTechnology,
            List<? extends UsbModemDriver> drivers, String prodName) {
        this.m_deviceName = deviceName;
        this.m_vendorId = vendorId;
        this.m_productId = productId;
        this.m_numTtyDevs = numTtyDevs;
        this.m_numBlockDevs = numBlockDevs;
        this.m_atPort = atPort;
        this.m_dataPort = dataPort;
        this.m_gpsPort = gpsPort;
        this.m_technologyTypes = modemTechnology;
        this.m_deviceDrivers = drivers;
        this.m_productName = prodName;
    }

    public String getDeviceName() {
        return this.m_deviceName;
    }

    public List<? extends UsbModemDriver> getDeviceDrivers() {
        return this.m_deviceDrivers;
    }

    public String getVendorId() {
        return this.m_vendorId;
    }

    public String getProductId() {
        return this.m_productId;
    }

    public int getNumTtyDevs() {
        int ret = this.m_numTtyDevs;
        if (TARGET_NAME != null
                && (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())
                        || TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName()))
                && this.m_deviceName.equals(Telit_LE910.m_deviceName)) {
            ret = this.m_numTtyDevs + 2;
        }
        return ret;
    }

    public int getNumBlockDevs() {
        return this.m_numBlockDevs;
    }

    public int getAtPort() {
        int ret = this.m_atPort;
        if (TARGET_NAME != null
                && (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())
                        || TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName()))
                && this.m_deviceName.equals(Telit_LE910.m_deviceName)) {
            ret = this.m_atPort + 2;
        }
        return ret;
    }

    public int getDataPort() {
        int ret = this.m_dataPort;
        if (TARGET_NAME != null
                && (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())
                        || TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName()))
                && this.m_deviceName.equals(Telit_LE910.m_deviceName)) {
            ret = this.m_dataPort + 2;
        }
        return ret;
    }

    public int getGpsPort() {
        int ret = this.m_gpsPort;
        if (TARGET_NAME != null
                && (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())
                        || TARGET_NAME.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getTargetName()))
                && this.m_deviceName.equals(Telit_LE910.m_deviceName)) {
            ret = this.m_gpsPort + 2;
        }
        return ret;
    }

    public List<ModemTechnologyType> getTechnologyTypes() {
        return this.m_technologyTypes;
    }

    public String getProductName() {
        return this.m_productName;
    }
}
