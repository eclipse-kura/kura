/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *      Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.test.hw.CommTest;
import org.eclipse.kura.core.test.hw.RxTxTest;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.system.SystemService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@SuiteClasses({ DataServiceTest.class,
				CloudDeploymentHandlerTest.class,
				CloudServiceTest.class,
				CommURITest.class,
	            ComponentConfigurationImplTest.class,
	            ConfigurationServiceTest.class,
	            NetUtilTest.class,
				NetworkServiceTest.class,
				SystemAdminServiceTest.class,
				SystemServiceTest.class,
				XmlUtilTest.class,
				CommTest.class,
				RxTxTest.class })
public class AllCoreTests {
	private static final Logger s_logger = LoggerFactory.getLogger(AllCoreTests.class);

	private static ConfigurationService s_configService;
	private static DataService          s_dataService;
	private static SystemService        s_sysService;

	public void setConfigurationService(ConfigurationService configService) {
		s_configService = configService;
	}

	public void unsetConfigurationService(ConfigurationService configService) {
		s_configService = configService;
	}

	public void setDataService(DataService dataService) {
		s_dataService = dataService;
	}

	public void unsetDataService(DataService dataService) {
		s_dataService = dataService;
	}

	public void setSystemService(SystemService sysService) {
		s_sysService = sysService;
	}

	public void unsetSystemService(SystemService sysService) {
		s_sysService = sysService;
	}
		
	@BeforeClass
	public static void setUpClass() throws Exception {
		s_logger.info("setUpClass...");

		int waitCount = 10;
		while ((s_configService == null || s_dataService == null) && waitCount > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			waitCount--;
			s_logger.info("Waiting for ConfigAdmin and DataService " + waitCount + "...");
		}

		if (s_configService == null || s_dataService == null) {
			throw new Exception("ConfigAdmin or DataService not set.");
		}

		try {

			// update the settings
			ComponentConfiguration mqttConfig = s_configService.getComponentConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport");
			Map<String, Object> mqttProps = mqttConfig.getConfigurationProperties();
			
			mqttProps.put("broker-url", "mqtt://iot.eclipse.org:1883/");
			mqttProps.put("topic.context.account-name", "guest");
			mqttProps.put("username", "guest");
			mqttProps.put("password", "welcome");
			
			// cloudbees fails in getting the primary MAC address
			// we need to compensate for it.
			String clientId = "cloudbees-kura"; 
			try {
				clientId = s_sysService.getPrimaryMacAddress();
			}
			catch (Throwable t) {
				// ignore.
			}
			mqttProps.put("client-id", clientId);
			s_configService.updateConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport", mqttProps);

			ComponentConfiguration dataConfig = s_configService.getComponentConfiguration("org.eclipse.kura.data.DataService");
			Map<String, Object> dataProps = dataConfig.getConfigurationProperties();
			dataProps.put("connect.auto-on-startup", false);
			s_configService.updateConfiguration("org.eclipse.kura.data.DataService", dataProps);

			// waiting for the configuration to be applied
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			throw new Exception("Failed to reconfigure the broker settings - failing out", e);
		}

		// connect
		if (!s_dataService.isConnected()) {
			s_dataService.connect();
		}
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		s_logger.info("tearDownClass...");
		if (s_dataService.isConnected()) {
			s_dataService.disconnect(0);
		}
	}
}
