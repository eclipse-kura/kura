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

	MiniGateway_Telit_HE910_NAD("HE910", new String[]{"HE910-NAD", "HE910-EUD"}, "Telit", KuraConstants.Mini_Gateway.getImageName(), 
			KuraConstants.Mini_Gateway.getImageVersion(), KuraConstants.Mini_Gateway.getTargetName(),
			Arrays.asList(ModemTechnologyType.HSPA, ModemTechnologyType.UMTS), 
			new SerialModemDriver("HE910", SerialModemComm.MiniGateway, "at+gmm\r\n")),
	Reliagate_10_11_Telit_HE910_NAD("HE910", new String[]{"HE910-NAD", "HE910-EUD"}, "Telit", KuraConstants.Reliagate_10_11.getImageName(), 
			KuraConstants.Reliagate_10_11.getImageVersion(), KuraConstants.Reliagate_10_11.getTargetName(),
			Arrays.asList(ModemTechnologyType.HSPA, ModemTechnologyType.UMTS), 
			new SerialModemDriver("HE910", SerialModemComm.Reliagate_10_11, "at+gmm\r\n"));			

	private String m_modemName;
	private String [] m_modemModels;
	private String m_manufacturerName;
	private String m_osImageName;
	private String m_osImageVersion;
	private String m_targetName;
	private List<ModemTechnologyType> m_technologyTypes;
	private SerialModemDriver m_driver;

	private SupportedSerialModemInfo(String modemName, String [] modemModels, String manufacturerName, String osImageName,
			String osImageVersion, String targetName, List<ModemTechnologyType> technologyTypes,
			SerialModemDriver driver) {

		m_modemName = modemName;
		m_modemModels = modemModels;
		m_manufacturerName = manufacturerName;
		m_osImageName = osImageName;
		m_osImageVersion = osImageVersion;
		m_targetName = targetName;
		m_technologyTypes = technologyTypes;
		m_driver = driver;
	}

	public String getModemName() {
		return m_modemName;
	}

	public String [] getModemModels() {
		return m_modemModels;
	}

	public String getManufacturerName() {
		return m_manufacturerName;
	}

	public String getOsImageName() {
		return m_osImageName;
	}

	public String getOsImageVersion() {
		return m_osImageVersion;
	}

	public String getTargetName() {
		return m_targetName;
	}
	
	public List<ModemTechnologyType> getTechnologyTypes() {
		return m_technologyTypes;
	}	

	public SerialModemDriver getDriver() {
		return m_driver;
	}
}