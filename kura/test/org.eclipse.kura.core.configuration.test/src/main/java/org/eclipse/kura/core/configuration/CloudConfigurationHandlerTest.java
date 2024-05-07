/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration;

import static org.mockito.Mockito.mock;

import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;

public class CloudConfigurationHandlerTest {

    private CloudConfigurationHandler cloudConfigurationHandler;

    private ConfigurationService mockConfigurationService = mock(ConfigurationService.class);
    private RequestHandlerRegistry mockRequestHandlerRegistry = mock(RequestHandlerRegistry.class);
    private SystemService mockSystemService = mock(SystemService.class);

    @Test
    public void a() {

    }

    private void givenCloudConfigurationHandler() {
        this.cloudConfigurationHandler = new CloudConfigurationHandler();

        this.cloudConfigurationHandler.setConfigurationService(this.mockConfigurationService);
        this.cloudConfigurationHandler.setRequestHandlerRegistry(this.mockRequestHandlerRegistry);
        this.cloudConfigurationHandler.setSystemService(this.mockSystemService);
    }
}
