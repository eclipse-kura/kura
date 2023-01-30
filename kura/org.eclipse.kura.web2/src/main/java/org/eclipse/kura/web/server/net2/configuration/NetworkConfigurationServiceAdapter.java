/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.net2.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;

/**
 * Adapter to retrieve {@link NetworkConfigurationService} properties and
 * convert them to a {@link GwtNetInterfaceConfig} object, and viceversa.
 *
 */
public class NetworkConfigurationServiceAdapter {

    private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
    private static final String NET_INTERFACES = "net.interfaces";

    private final ConfigurationService configurationService;
    private final List<String> ifnames;
    private final Map<String, Object> netConfServProperties;

    public NetworkConfigurationServiceAdapter() throws GwtKuraException, KuraException {
        this.configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        ComponentConfiguration config = configurationService
                .getComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID);
        this.netConfServProperties = config.getConfigurationProperties();

        String netInterfaces = (String) this.netConfServProperties.get(NET_INTERFACES);
        String[] interfaces = netInterfaces.split(",");
        this.ifnames = new ArrayList<>();
        for (String name : interfaces) {
            this.ifnames.add(name.trim());
        }
    }

    /**
     * 
     * @return the list of interface names currently configured in the
     *         {@link org.eclipse.kura.net.admin.NetworkConfigurationService}
     */
    public List<String> getNetInterfaces() {
        return this.ifnames;
    }

    /**
     * 
     * @param ifname
     * @return a new {@link GwtNetInterfaceConfig} with properties read from the
     *         {@link org.eclipse.kura.net.admin.NetworkConfigurationService}
     */
    public GwtNetInterfaceConfig getGwtNetInterfaceConfig(String ifname) {
        return new GwtNetInterfaceConfigBuilder(this.netConfServProperties).forInterface(ifname).build();
    }

    /**
     * Updates the {@link ConfigurationService} with the properties found in
     * {@code gwtConfig}
     * 
     * @param gwtConfig the configuration containing the properties to update
     * @throws KuraException
     */
    public void updateConfiguration(GwtNetInterfaceConfig gwtConfig) throws KuraException {
        NetworkConfigurationServicePropertiesBuilder builder = new NetworkConfigurationServicePropertiesBuilder(
                gwtConfig);
        Map<String, Object> newProperties = builder.build();

        this.configurationService.updateConfiguration(NETWORK_CONFIGURATION_SERVICE_PID, newProperties);
    }

}
