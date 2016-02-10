/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem;

import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910ConfigGenerator;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910ModemFactory;

public class SupportedSerialModemsFactoryInfo {

	public enum SerialModemFactoryInfo {
		
		Telit_HE910_NAD	(SupportedSerialModemsInfo.getModem(), TelitHe910ModemFactory.class, TelitHe910ConfigGenerator.class);
		
		private SupportedSerialModemInfo m_serialModemInfo;
		private Class<? extends CellularModemFactory> m_factoryClass;
		private Class<? extends ModemPppConfigGenerator> m_configClass;
		
		private SerialModemFactoryInfo(SupportedSerialModemInfo modemInfo,
		        Class<? extends CellularModemFactory> factoryClass,
		        Class<? extends ModemPppConfigGenerator> configClass) {
		    this.m_serialModemInfo = modemInfo;
		    this.m_factoryClass = factoryClass;
		    this.m_configClass = configClass;
		}
		
		public SupportedSerialModemInfo getSerialModemInfo() {
		    return m_serialModemInfo;
		}
        
        public Class<? extends CellularModemFactory> getModemFactoryClass() {
            return m_factoryClass;
        }
		
		public Class<? extends ModemPppConfigGenerator> getConfigGeneratorClass() {
			return m_configClass;
		}
	}
	
	public static SerialModemFactoryInfo getModem(SupportedSerialModemInfo modemInfo) {
        if (modemInfo == null)
            return null;
        
        for (SerialModemFactoryInfo modem : SerialModemFactoryInfo.values()) {
            if (modemInfo.equals(modem.getSerialModemInfo())) {
                return modem;
            }
        }
        
        return null;
	}
}
