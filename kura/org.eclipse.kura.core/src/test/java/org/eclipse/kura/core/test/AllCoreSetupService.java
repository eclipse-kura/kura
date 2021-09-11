/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.broker.Server;

@Component(service = AllCoreSetupService.class ,immediate = true, scope = ServiceScope.SINGLETON)
public class AllCoreSetupService {

    private static final Logger s_logger = LoggerFactory.getLogger(AllCoreSetupService.class);

    @Reference
    ConfigurationService s_configService;
    @Reference
    DataService s_dataService;
    @Reference
    SystemService s_sysService;

    @Activate
    public void setUpClass() throws Exception {
        s_logger.info("setUpClass...");

        // start Moquette
        Server.main(new String[] {});

        try {
            // update the settings
            ComponentConfiguration mqttConfig = s_configService
                    .getComponentConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport");
            Map<String, Object> mqttProps = mqttConfig.getConfigurationProperties();

            // mqttProps.put("broker-url", "mqtt://iot.eclipse.org:1883/");
            // mqttProps.put("topic.context.account-name", "guest");
            // mqttProps.put("username", "guest");
            // mqttProps.put("password", "welcome");

            s_logger.info("Changing cloud credentials...");
            mqttProps.put("broker-url", "mqtt://localhost:1883/");
            mqttProps.put("topic.context.account-name", "ethdev");
            mqttProps.put("username", "");
            mqttProps.put("password", "");

            // cloudbees fails in getting the primary MAC address
            // we need to compensate for it.
            String clientId = null;
            try {
                clientId = s_sysService.getPrimaryMacAddress();
            } catch (Throwable t) {
                // ignore.
            }
            if (clientId == null || clientId.isEmpty()) {
                clientId = "cloudbees-kura";
            }
            mqttProps.put("client-id", clientId);
            s_configService.updateConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
                    mqttProps);

            ComponentConfiguration dataConfig = s_configService
                    .getComponentConfiguration("org.eclipse.kura.data.DataService");
            Map<String, Object> dataProps = dataConfig.getConfigurationProperties();
            dataProps.put("connect.auto-on-startup", false);
            dataProps.put("enable.rate.limit", false);
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

}
