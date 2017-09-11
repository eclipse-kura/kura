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

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.modem.ModemTechnologyType;

public enum SupportedSerialModemInfo {

    MiniGateway_Telit_HE910_NAD("HE910", new String[] { "HE910-NAD", "HE910-EUD" }, "Telit", KuraConstants.Mini_Gateway
            .getImageName(), KuraConstants.Mini_Gateway.getImageVersion(), KuraConstants.Mini_Gateway
                    .getTargetName(), Arrays.asList(ModemTechnologyType.HSPA,
                            ModemTechnologyType.UMTS), new SerialModemDriver("HE910", SerialModemComm.MiniGateway,
                                    "at+gmm\r\n"));

    private String modemName;
    private String[] modemModels;
    private String manufacturerName;
    private String osImageName;
    private String osImageVersion;
    private String targetName;
    private List<ModemTechnologyType> technologyTypes;
    private SerialModemDriver driver;

    private SupportedSerialModemInfo(String modemName, String[] modemModels, String manufacturerName,
            String osImageName, String osImageVersion, String targetName, List<ModemTechnologyType> technologyTypes,
            SerialModemDriver driver) {

        this.modemName = modemName;
        this.modemModels = modemModels;
        this.manufacturerName = manufacturerName;
        this.osImageName = osImageName;
        this.osImageVersion = osImageVersion;
        this.targetName = targetName;
        this.technologyTypes = technologyTypes;
        this.driver = driver;
    }

    public String getModemName() {
        return this.modemName;
    }

    public String[] getModemModels() {
        return this.modemModels;
    }

    public String getManufacturerName() {
        return this.manufacturerName;
    }

    public String getOsImageName() {
        return this.osImageName;
    }

    public String getOsImageVersion() {
        return this.osImageVersion;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public List<ModemTechnologyType> getTechnologyTypes() {
        return this.technologyTypes;
    }

    public SerialModemDriver getDriver() {
        return this.driver;
    }
}