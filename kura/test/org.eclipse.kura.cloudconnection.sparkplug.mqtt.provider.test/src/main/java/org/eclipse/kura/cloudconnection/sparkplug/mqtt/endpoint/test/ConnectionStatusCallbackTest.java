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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.data.DataService;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class ConnectionStatusCallbackTest extends StepsCollection {

    private CloudConnectionListener listener = mock(CloudConnectionListener.class);
    private EventAdmin eventAdmin = mock(EventAdmin.class);
    private DataService dataService = mock(DataService.class);

    /*
     * Scenarios
     */

    @Test
    public void shouldNotifyOnConnectionEstabilishedAndPostEvent() {
        givenEventAdmin(this.eventAdmin);
        givenDataService(this.dataService);
        givenCloudConnectionListener(this.listener);

        whenOnConnectionEstabilished();

        thenCloudConnectionListenerNotifiedOnConnectionEstabilished(1);
        thenEventAdminPostedEvent(CloudConnectionEstablishedEvent.class);
    }

    @Test
    public void shouldNotNotifyOnConnectionEstabilishedButShouldPostEvent() {
        givenEventAdmin(this.eventAdmin);
        givenDataService(this.dataService);
        givenCloudConnectionListener(this.listener);
        givenUnregisterCloudConnectionListener(this.listener);

        whenOnConnectionEstabilished();

        thenCloudConnectionListenerNotifiedOnConnectionEstabilished(0);
        thenEventAdminPostedEvent(CloudConnectionEstablishedEvent.class);
    }

    @Test
    public void shouldNotifyOnDisconnectedAndPostEvent() {
        givenEventAdmin(this.eventAdmin);
        givenDataService(this.dataService);
        givenCloudConnectionListener(this.listener);

        whenOnDisconnected();

        thenCloudConnectionListenerNotifiedOnDisconnected(1);
        thenEventAdminPostedEvent(CloudConnectionLostEvent.class);
    }

    @Test
    public void shouldNotNotifyOnDisconnectedButShouldPostEvent() {
        givenEventAdmin(this.eventAdmin);
        givenDataService(this.dataService);
        givenCloudConnectionListener(this.listener);
        givenUnregisterCloudConnectionListener(this.listener);

        whenOnDisconnected();

        thenCloudConnectionListenerNotifiedOnDisconnected(0);
        thenEventAdminPostedEvent(CloudConnectionLostEvent.class);
    }

    @Test
    public void shouldNotifyOnConnectionLostAndPostEvent() {
        givenEventAdmin(this.eventAdmin);
        givenDataService(this.dataService);
        givenCloudConnectionListener(this.listener);

        whenOnConnectionLost();

        thenCloudConnectionListenerNotifiedOnConnectionLost(1);
        thenEventAdminPostedEvent(CloudConnectionLostEvent.class);
    }

    @Test
    public void shouldNotNotifyOnConnectionLostButShouldPostEvent() {
        givenEventAdmin(this.eventAdmin);
        givenDataService(this.dataService);
        givenCloudConnectionListener(this.listener);
        givenUnregisterCloudConnectionListener(this.listener);

        whenOnConnectionLost();

        thenCloudConnectionListenerNotifiedOnConnectionLost(0);
        thenEventAdminPostedEvent(CloudConnectionLostEvent.class);
    }

    /*
     * Steps
     */

    private void thenCloudConnectionListenerNotifiedOnConnectionEstabilished(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onConnectionEstablished();
    }

    private void thenCloudConnectionListenerNotifiedOnDisconnected(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onDisconnected();
    }

    private void thenCloudConnectionListenerNotifiedOnConnectionLost(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onConnectionLost();
    }

    private <T extends Event> void thenEventAdminPostedEvent(Class<T> eventType) {
        verify(this.eventAdmin, times(1)).postEvent(any(eventType));
    }

}
