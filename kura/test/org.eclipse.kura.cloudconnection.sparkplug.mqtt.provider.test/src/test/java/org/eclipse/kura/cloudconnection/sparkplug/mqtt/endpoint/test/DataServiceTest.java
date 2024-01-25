/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.junit.Test;

public class DataServiceTest extends StepsCollection {

    private DataService dataService = mock(DataService.class);

    /*
     * Scenarios
     */

    @Test
    public void shouldRegisterAsDataServiceListenerToDataService() {
        givenDataService(this.dataService);

        whenActivate(new PropertiesBuilder().add(ConfigurationService.KURA_SERVICE_PID, "test-pid").build());

        thenDataServiceAddedDataServiceListener(this.endpoint);
    }

    @Test
    public void shouldCallDataServiceIsConnected() {
        givenDataService(this.dataService);

        whenIsConnected();

        thenDataServiceCalledIsConnected();
    }

    @Test
    public void shouldCallDataServiceDisconnect() throws KuraDisconnectException {
        givenDataService(this.dataService);

        whenDisconnect();

        thenDataServiceCalledDisconnect(0);
    }

    @Test
    public void shouldCallDataServiceConnect() throws KuraConnectException {
        givenDataService(this.dataService);

        whenConnect();

        thenDataServiceCalledConnect();
    }

    /*
     * Steps
     */

    private void thenDataServiceAddedDataServiceListener(DataServiceListener listener) {
        verify(this.dataService, times(1)).addDataServiceListener(listener);
    }

    private void thenDataServiceCalledIsConnected() {
        verify(this.dataService, times(1)).isConnected();
    }

    private void thenDataServiceCalledDisconnect(long expectedQuiesceTimeout) {
        verify(this.dataService, times(1)).disconnect(expectedQuiesceTimeout);
    }

    private void thenDataServiceCalledConnect() throws KuraConnectException {
        verify(this.dataService, times(1)).connect();
    }

}
