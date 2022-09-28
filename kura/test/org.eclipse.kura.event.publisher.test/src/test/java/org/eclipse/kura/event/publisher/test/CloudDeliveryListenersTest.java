/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.event.publisher.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.event.publisher.EventPublisher;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

public class CloudDeliveryListenersTest {

    private EventPublisher publisher;
    private CloudDeliveryListener cloudDeliveryListener = mock(CloudDeliveryListener.class);
    private CloudEndpoint mockCloudEndpoint = mock(CloudEndpoint.class);

    /*
     * Scenarios
     */

    @Test
    public void cloudDeliveryListenersShouldBeNotified() throws InvalidSyntaxException {
        givenEventPublisher();
        givenCloudDeliveryListener();

        whenOnMessageConfirmed("1234");

        thenCloudConnectionDeliveryListenerNotified("1234");
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenEventPublisher() throws InvalidSyntaxException {
        this.publisher = new EventPublisher();

        ComponentContext mockComponentContext = mock(ComponentContext.class);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Filter mockFilter = mock(Filter.class);

        when(mockBundleContext.createFilter(Mockito.any())).thenReturn(mockFilter);
        when(mockBundleContext.getService(Mockito.any())).thenReturn(mockCloudEndpoint);
        when(mockBundleContext.ungetService(Mockito.any())).thenReturn(true);

        when(mockComponentContext.getBundleContext()).thenReturn(mockBundleContext);

        this.publisher.activate(mockComponentContext, new HashMap<>());
    }

    private void givenCloudDeliveryListener() {
        this.publisher.registerCloudDeliveryListener(this.cloudDeliveryListener);
    }

    /*
     * When
     */

    private void whenOnMessageConfirmed(String messageId) {
        this.publisher.onMessageConfirmed(messageId);
    }

    /*
     * Then
     */

    private void thenCloudConnectionDeliveryListenerNotified(String messageId) {
        verify(this.cloudDeliveryListener, timeout(1000).times(1)).onMessageConfirmed(messageId);
    }
}
