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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.event.publisher.helper.CloudEndpointServiceTracker;
import org.eclipse.kura.event.publisher.helper.CloudEndpointTrackerListener;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class CloudEndpointServiceTrackerTest {

    private CloudEndpointServiceTracker tracker;
    private CloudEndpointTrackerListener listener = mock(CloudEndpointTrackerListener.class);
    @SuppressWarnings("unchecked")
    private ServiceReference<CloudEndpoint> reference = mock(ServiceReference.class);

    /*
     * Scenarios
     */

    /*
     * Steps
     */

    @Test
    public void listenerShouldBeNotifiedWhenServiceAdded() throws InvalidSyntaxException {
        givenCloudEndpointServiceTracker();
        givenCloudEndpointTrackerListener();

        whenAddingService();

        thenListenerOnCloudEndpointAdded();
    }

    @Test
    public void listenerShouldBeNotifiedWhenServiceRemoved() throws InvalidSyntaxException {
        givenCloudEndpointServiceTracker();
        givenCloudEndpointTrackerListener();

        whenRemovingService();

        thenListenerOnCloudEndpointRemoved();
    }

    /*
     * Given
     */

    private void givenCloudEndpointServiceTracker() throws InvalidSyntaxException {
        BundleContext mockContext = mock(BundleContext.class);
        Filter mockFilter = mock(Filter.class);
        CloudEndpoint mockCloudEndpoint = mock(CloudEndpoint.class);

        when(mockContext.createFilter(Mockito.any())).thenReturn(mockFilter);
        when(mockContext.getService(Mockito.any())).thenReturn(mockCloudEndpoint);
        when(mockContext.ungetService(Mockito.any())).thenReturn(true);

        this.tracker = new CloudEndpointServiceTracker(mockContext, "example-cs");
    }

    private void givenCloudEndpointTrackerListener() {
        this.tracker.registerCloudStackTrackerListener(this.listener);
    }

    /*
     * When
     */

    private void whenAddingService() {
        this.tracker.addingService(this.reference);
    }

    private void whenRemovingService() {
        this.tracker.removedService(this.reference, mock(CloudEndpoint.class));
    }

    /*
     * Then
     */

    private void thenListenerOnCloudEndpointAdded() {
        verify(this.listener, times(1)).onCloudEndpointAdded(Mockito.any());
    }

    private void thenListenerOnCloudEndpointRemoved() {
        verify(this.listener, times(1)).onCloudEndpointRemoved(Mockito.any());
    }

}
