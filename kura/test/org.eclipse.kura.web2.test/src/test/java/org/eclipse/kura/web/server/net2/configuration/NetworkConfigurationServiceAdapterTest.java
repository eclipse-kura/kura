/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.junit.Test;

public class NetworkConfigurationServiceAdapterTest {

    private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";

    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private NetworkConfigurationServiceAdapter adapter;
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void missingNetworkConfigurationIsReportedAsServiceNotEnabled() throws KuraException {
        givenNoNetworkConfiguration();

        whenAdapterIsCreated();

        thenServiceNotEnabledIsReported();
    }

    @Test
    public void networkConfigurationWithoutPropertiesIsReportedAsServiceNotEnabled() throws KuraException {
        givenNetworkConfigurationWithProperties(null);

        whenAdapterIsCreated();

        thenServiceNotEnabledIsReported();
    }

    @Test
    public void configuredInterfaceNamesAreReadFromTheNetworkConfiguration() throws KuraException {
        givenNetworkConfigurationWithInterfaces("lo, eth0,wlan0");

        whenAdapterIsCreated();

        thenNoExceptionOccurred();
        thenConfiguredInterfaceNamesAre("lo", "eth0", "wlan0");
    }

    /*
     * Given
     */

    private void givenNoNetworkConfiguration() throws KuraException {
        when(this.configurationService.getComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID)).thenReturn(null);
    }

    private void givenNetworkConfigurationWithProperties(Map<String, Object> properties) throws KuraException {
        ComponentConfiguration networkConfiguration = mock(ComponentConfiguration.class);
        when(networkConfiguration.getConfigurationProperties()).thenReturn(properties);
        when(this.configurationService.getComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID))
                .thenReturn(networkConfiguration);
    }

    private void givenNetworkConfigurationWithInterfaces(String interfaces) throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("net.interfaces", interfaces);
        givenNetworkConfigurationWithProperties(properties);
    }

    /*
     * When
     */

    private void whenAdapterIsCreated() {
        try {
            this.adapter = new NetworkConfigurationServiceAdapter(this.configurationService);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenServiceNotEnabledIsReported() {
        assertNull(this.adapter);
        assertNotNull(this.occurredException);
        assertEquals(GwtKuraException.class, this.occurredException.getClass());
        assertEquals(GwtKuraErrorCode.SERVICE_NOT_ENABLED, ((GwtKuraException) this.occurredException).getCode());
    }

    private void thenNoExceptionOccurred() {
        assertNull(this.occurredException);
    }

    private void thenConfiguredInterfaceNamesAre(String... expectedNames) {
        List<String> names = this.adapter.getConfiguredNetworkInterfaceNames();
        assertEquals(Arrays.asList(expectedNames), names);
    }
}
