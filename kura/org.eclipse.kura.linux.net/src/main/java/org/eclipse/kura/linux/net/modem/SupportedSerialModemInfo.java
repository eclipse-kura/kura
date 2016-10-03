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

public enum SupportedSerialModemInfo {

    MiniGateway_Telit_HE910_NAD("HE910", new String[] { "HE910-NAD", "HE910-EUD" }, "Telit",
            KuraConstants.Mini_Gateway.getImageName(), KuraConstants.Mini_Gateway.getImageVersion(),
            KuraConstants.Mini_Gateway.getTargetName(),
            Arrays.asList(ModemTechnologyType.HSPA, ModemTechnologyType.UMTS),
            new SerialModemDriver("HE910", SerialModemComm.MiniGateway, "at+gmm\r\n"));

    private String m_modemName;
    private String[] m_modemModels;
    private String m_manufacturerName;
    private String m_osImageName;
    private String m_osImageVersion;
    private String m_targetName;
    private List<ModemTechnologyType> m_technologyTypes;
    private SerialModemDriver m_driver;

    private SupportedSerialModemInfo(String modemName, String[] modemModels, String manufacturerName,
            String osImageName, String osImageVersion, String targetName, List<ModemTechnologyType> technologyTypes,
            SerialModemDriver driver) {

        this.m_modemName = modemName;
        this.m_modemModels = modemModels;
        this.m_manufacturerName = manufacturerName;
        this.m_osImageName = osImageName;
        this.m_osImageVersion = osImageVersion;
        this.m_targetName = targetName;
        this.m_technologyTypes = technologyTypes;
        this.m_driver = driver;
    }

    public String getModemName() {
        return this.m_modemName;
    }

    public String[] getModemModels() {
        return this.m_modemModels;
    }

    public String getManufacturerName() {
        return this.m_manufacturerName;
    }

    public String getOsImageName() {
        return this.m_osImageName;
    }

    public String getOsImageVersion() {
        return this.m_osImageVersion;
    }

    public String getTargetName() {
        return this.m_targetName;
    }

    public List<ModemTechnologyType> getTechnologyTypes() {
        return this.m_technologyTypes;
    }

    public SerialModemDriver getDriver() {
        return this.m_driver;
    }
}