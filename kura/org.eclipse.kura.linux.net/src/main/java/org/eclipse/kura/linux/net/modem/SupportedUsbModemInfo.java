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
 *  3 PORT d.o.o.
 *  Sterwen Technologies
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.net.modem.ModemTechnologyType;

public enum SupportedUsbModemInfo {

    // device name, vendor, product, ttyDevs, blockDevs, AT Port, Data Port, GPS Port, turn off delay, turn on delay,
    // technology types,
    // device driver
    Telit_HE910_DG("HE910-DG", "1bc7", "0021", 6, 0, 3, 0, 3, 5000, 10000, Arrays.asList(ModemTechnologyType.HSPA,
            ModemTechnologyType.UMTS), Arrays.asList(new UsbModemDriver("cdc_acm", "1bc7", "0021")), "6 CDC-ACM"),
    Telit_HE910_D("HE910-D", "1bc7", "0021", 7, 0, 3, 0, 3, 5000, 10000, Arrays.asList(ModemTechnologyType.HSPA,
            ModemTechnologyType.UMTS), Arrays.asList(new UsbModemDriver("cdc_acm", "1bc7", "0021")), ""),
    Telit_GE910("GE910", "1bc7", "0022", 2, 0, 0, 1, 0, 5000, 10000, Arrays.asList(ModemTechnologyType.GSM_GPRS), Arrays
            .asList(new UsbModemDriver("cdc_acm", "1bc7", "0022")), ""),
    Telit_DE910_DUAL("DE910-DUAL", "1bc7", "1010", 4, 0, 2, 3, 1, 5000, 10000, Arrays.asList(ModemTechnologyType.EVDO,
            ModemTechnologyType.CDMA), Arrays.asList(new De910ModemDriver()), ""),
    Telit_LE910("LE910", "1bc7", "1201", 5, 0, 2, 3, 1, 5000, 30000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.HSPA, ModemTechnologyType.UMTS), Arrays.asList(new Le910ModemDriver()), ""),
    Telit_LE910_V2("LE910-V2", "1bc7", "0036", 6, 0, 3, 0, -1, 5000, 10000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.HSPA,
            ModemTechnologyType.UMTS), Arrays.asList(new UsbModemDriver("cdc_acm", "1bc7", "0036")), ""),
    Telit_CE910_DUAL("CE910-DUAL", "1bc7", "1011", 2, 0, 1, 1, -1, 5000, 10000, Arrays
            .asList(ModemTechnologyType.CDMA), Arrays.asList(new Ce910ModemDriver()), ""),
    Sierra_MC8775("MC8775", "1199", "6812", 3, 0, 2, 0, -1, 5000, 10000, Arrays
            .asList(ModemTechnologyType.HSDPA), Arrays.asList(new UsbModemDriver("sierra", "1199", "6812")), ""),
    Sierra_MC8790("MC8790", "1199", "683c", 7, 0, 3, 4, -1, 5000, 10000, Arrays
            .asList(ModemTechnologyType.HSDPA), Arrays.asList(new UsbModemDriver("sierra", "1199", "683c")), ""),
    Sierra_USB598("USB598", "1199", "0025", 4, 1, 0, 0, -1, 5000, 10000, Arrays.asList(ModemTechnologyType.EVDO), Arrays
            .asList(new UsbModemDriver("sierra", "1199", "0025")), ""),
    Ublox_SARA_U2("SARA-U2", "1546", "1102", 7, 0, 1, 0, -1, 5000, 10000, Arrays
            .asList(ModemTechnologyType.HSPA), Arrays.asList(new UsbModemDriver("cdc_acm", "1546", "1102")), ""),
    UBLOX_LARA_R2("LARA-R2", "1546", "110a", 6, 0, 1, 0, -1, 5000, 10000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.HSPA,
            ModemTechnologyType.UMTS), Arrays.asList(new UsbModemDriver("cdc_acm", "1546", "110a")), ""),
    Zte_ME3630("ME3630", "19d2", "1476", 3, 0, 1, 2, 1, 5000, 10000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.GSM_GPRS,
            ModemTechnologyType.UMTS), Arrays.asList(new OptionModemDriver("19d2", "1476")), ""),
    SimTech_SIM7000("SIM7000", "1e0e", "9001", 5, 0, 3, 2, 3, 5000, 10000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.GSM_GPRS), Arrays.asList(new OptionModemDriver("1e0e", "9001")), ""),
    QUECTEL_EX25("EX25", "2c7c", "0125", 4, 0, 2, 3, -1, 5000, 10000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.HSPA,
            ModemTechnologyType.UMTS), Arrays.asList(new UsbModemDriver("cdc_acm", "2c7c", "0125")), ""),
    QUECTEL_BG96("BG96", "2c7c", "0296", 4, 0, 2, 3, -1, 5000, 10000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.GSM_GPRS), Arrays.asList(new UsbModemDriver("cdc_acm", "2c7c", "0296")), ""),
    HUAWEI_MS2372("MS2372", "12d1", "1506", 3, 0, 2, 0, -1, 5000, 15000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.HSPA, ModemTechnologyType.HSDPA, ModemTechnologyType.UMTS,
            ModemTechnologyType.GSM_GPRS), Arrays.asList(new UsbModemDriver("option", "12d1", "1506")), ""),
    TELEFONICA_IK41VE("IK41VE", "1bbb", "00b6", 3, 0, 2, 0, -1, 5000, 15000, Arrays.asList(ModemTechnologyType.LTE,
            ModemTechnologyType.HSPA, ModemTechnologyType.HSDPA, ModemTechnologyType.UMTS,
            ModemTechnologyType.GSM_GPRS), Arrays.asList(new UsbModemDriver("option", "1bbb", "00b6")), "");

    private String deviceName;
    private String vendorId;
    private String productId;
    private int numTtyDevs;
    private int numBlockDevs;
    private long turnOffDelay;
    private long turnOnDelay;

    private int atPort;
    private int dataPort;
    private int gpsPort;

    private List<ModemTechnologyType> technologyTypes;
    private List<? extends UsbModemDriver> deviceDrivers;

    private String productName;

    @SuppressWarnings("checkstyle:parameterNumber")
    private SupportedUsbModemInfo(String deviceName, String vendorId, String productId, int numTtyDevs,
            int numBlockDevs, int atPort, int dataPort, int gpsPort, long turnOffDelay, long turnOnDelay,
            List<ModemTechnologyType> modemTechnology, List<? extends UsbModemDriver> drivers, String prodName) {
        this.deviceName = deviceName;
        this.vendorId = vendorId;
        this.productId = productId;
        this.numTtyDevs = numTtyDevs;
        this.numBlockDevs = numBlockDevs;
        this.atPort = atPort;
        this.dataPort = dataPort;
        this.gpsPort = gpsPort;
        this.turnOffDelay = turnOffDelay;
        this.turnOnDelay = turnOnDelay;
        this.technologyTypes = modemTechnology;
        this.deviceDrivers = drivers;
        this.productName = prodName;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public List<? extends UsbModemDriver> getDeviceDrivers() {
        return this.deviceDrivers;
    }

    public String getVendorId() {
        return this.vendorId;
    }

    public String getProductId() {
        return this.productId;
    }

    public int getNumTtyDevs() {
        return this.numTtyDevs;
    }

    public int getNumBlockDevs() {
        return this.numBlockDevs;
    }

    public int getAtPort() {
        return this.atPort;
    }

    public int getDataPort() {
        return this.dataPort;
    }

    public int getGpsPort() {
        return this.gpsPort;
    }

    public List<ModemTechnologyType> getTechnologyTypes() {
        return this.technologyTypes;
    }

    public String getProductName() {
        return this.productName;
    }

    public long getTurnOffDelay() {
        return this.turnOffDelay;
    }

    public long getTurnOnDelay() {
        return this.turnOnDelay;
    }
}
