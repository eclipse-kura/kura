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
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem;

import java.util.List;

import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.admin.modem.hspa.HspaModemConfigGenerator;
import org.eclipse.kura.net.admin.modem.huawei.HuaweiModemFactory;
import org.eclipse.kura.net.admin.modem.quectel.bg96.QuectelBG96ModemFactory;
import org.eclipse.kura.net.admin.modem.quectel.ex25.QuectelEX25ModemFactory;
import org.eclipse.kura.net.admin.modem.quectel.generic.QuectelGenericConfigGenerator;
import org.eclipse.kura.net.admin.modem.sierra.mc87xx.SierraMc87xxConfigGenerator;
import org.eclipse.kura.net.admin.modem.sierra.mc87xx.SierraMc87xxModemFactory;
import org.eclipse.kura.net.admin.modem.sierra.usb598.SierraUsb598ConfigGenerator;
import org.eclipse.kura.net.admin.modem.sierra.usb598.SierraUsb598ModemFactory;
import org.eclipse.kura.net.admin.modem.simtech.sim7000.SimTechSim7000ConfigGenerator;
import org.eclipse.kura.net.admin.modem.simtech.sim7000.SimTechSim7000ModemFactory;
import org.eclipse.kura.net.admin.modem.telefonica.TelefonicaModemFactory;
import org.eclipse.kura.net.admin.modem.telit.de910.TelitDe910ConfigGenerator;
import org.eclipse.kura.net.admin.modem.telit.de910.TelitDe910ModemFactory;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910ConfigGenerator;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910ModemFactory;
import org.eclipse.kura.net.admin.modem.telit.le910.TelitLe910ConfigGenerator;
import org.eclipse.kura.net.admin.modem.telit.le910.TelitLe910ModemFactory;
import org.eclipse.kura.net.admin.modem.telit.le910v2.TelitLe910v2ConfigGenerator;
import org.eclipse.kura.net.admin.modem.telit.le910v2.TelitLe910v2ModemFactory;
import org.eclipse.kura.net.admin.modem.ublox.generic.UbloxModemConfigGenerator;
import org.eclipse.kura.net.admin.modem.ublox.generic.UbloxModemFactory;
import org.eclipse.kura.net.admin.modem.zte.me3630.ZteMe3630ConfigGenerator;
import org.eclipse.kura.net.admin.modem.zte.me3630.ZteMe3630ModemFactory;

public class SupportedUsbModemsFactoryInfo {

    public enum UsbModemFactoryInfo {

        // modem info, implementation factory, config reader/writer class
        Telit_HE910_DG(SupportedUsbModemInfo.Telit_HE910_DG, TelitHe910ModemFactory.class, TelitHe910ConfigGenerator.class),
        Telit_HE910_D(SupportedUsbModemInfo.Telit_HE910_D, TelitHe910ModemFactory.class, TelitHe910ConfigGenerator.class),
        Telit_GE910(SupportedUsbModemInfo.Telit_GE910, TelitHe910ModemFactory.class, TelitHe910ConfigGenerator.class),
        Telit_DE910_DUAL(SupportedUsbModemInfo.Telit_DE910_DUAL, TelitDe910ModemFactory.class, TelitDe910ConfigGenerator.class),
        Telit_CE910_DUAL(SupportedUsbModemInfo.Telit_CE910_DUAL, TelitDe910ModemFactory.class, TelitDe910ConfigGenerator.class),
        Telit_LE910(SupportedUsbModemInfo.Telit_LE910, TelitLe910ModemFactory.class, TelitLe910ConfigGenerator.class),
        Telit_LE910_V2(SupportedUsbModemInfo.Telit_LE910_V2, TelitLe910v2ModemFactory.class, TelitLe910v2ConfigGenerator.class),
        Sierra_MC8775(SupportedUsbModemInfo.Sierra_MC8775, SierraMc87xxModemFactory.class, SierraMc87xxConfigGenerator.class),
        Sierra_MC8790(SupportedUsbModemInfo.Sierra_MC8790, SierraMc87xxModemFactory.class, SierraMc87xxConfigGenerator.class),
        Sierra_USB598(SupportedUsbModemInfo.Sierra_USB598, SierraUsb598ModemFactory.class, SierraUsb598ConfigGenerator.class),
        Ublox_SARA_U2(SupportedUsbModemInfo.Ublox_SARA_U2, UbloxModemFactory.class, UbloxModemConfigGenerator.class),
        UBLOX_LARA_R2(SupportedUsbModemInfo.UBLOX_LARA_R2, UbloxModemFactory.class, UbloxModemConfigGenerator.class),
        Zte_ME3630(SupportedUsbModemInfo.Zte_ME3630, ZteMe3630ModemFactory.class, ZteMe3630ConfigGenerator.class),
        SimTech_SIM7000(SupportedUsbModemInfo.SimTech_SIM7000, SimTechSim7000ModemFactory.class, SimTechSim7000ConfigGenerator.class),
        QUECTEL_EX25(SupportedUsbModemInfo.QUECTEL_EX25, QuectelEX25ModemFactory.class, QuectelGenericConfigGenerator.class),
        QUECTEL_BG96(SupportedUsbModemInfo.QUECTEL_BG96, QuectelBG96ModemFactory.class, QuectelGenericConfigGenerator.class),
        HUAWEI_MS2372(SupportedUsbModemInfo.HUAWEI_MS2372, HuaweiModemFactory.class, HspaModemConfigGenerator.class),
        TELEFONICA_IK41VE(SupportedUsbModemInfo.TELEFONICA_IK41VE, TelefonicaModemFactory.class, HspaModemConfigGenerator.class);

        private final SupportedUsbModemInfo usbModemInfo;
        private final Class<? extends CellularModemFactory> factoryClass;
        private final Class<? extends ModemPppConfigGenerator> configClass;

        private UsbModemFactoryInfo(SupportedUsbModemInfo modemInfo, Class<? extends CellularModemFactory> factoryClass,
                Class<? extends ModemPppConfigGenerator> configClass) {
            this.usbModemInfo = modemInfo;
            this.factoryClass = factoryClass;
            this.configClass = configClass;
        }

        public SupportedUsbModemInfo getUsbModemInfo() {
            return this.usbModemInfo;
        }

        public Class<? extends CellularModemFactory> getModemFactoryClass() {
            return this.factoryClass;
        }

        public Class<? extends ModemPppConfigGenerator> getConfigGeneratorClass() {
            return this.configClass;
        }
    }

    private SupportedUsbModemsFactoryInfo() {
    }

    public static UsbModemFactoryInfo getModem(SupportedUsbModemInfo modemInfo) {
        if (modemInfo == null) {
            return null;
        }

        for (UsbModemFactoryInfo modem : UsbModemFactoryInfo.values()) {
            if (modemInfo.equals(modem.getUsbModemInfo())) {
                return modem;
            }
        }

        return null;
    }

    public static UsbModemFactoryInfo getModem(String vendor, String product) {

        UsbModemFactoryInfo modemFactoryInfo = null;
        for (UsbModemFactoryInfo modem : UsbModemFactoryInfo.values()) {
            if (modem.getUsbModemInfo().getVendorId().equals(vendor)
                    && modem.getUsbModemInfo().getProductId().equals(product)) {
                modemFactoryInfo = modem;
                break;
            }
        }

        return modemFactoryInfo;
    }

    public static List<? extends UsbModemDriver> getDeviceDrivers(String vendor, String product) {

        List<? extends UsbModemDriver> drivers = null;
        for (UsbModemFactoryInfo modem : UsbModemFactoryInfo.values()) {
            if (modem.getUsbModemInfo().getVendorId().equals(vendor)
                    && modem.getUsbModemInfo().getProductId().equals(product)) {
                drivers = modem.getUsbModemInfo().getDeviceDrivers();
                break;
            }
        }
        return drivers;
    }
}
