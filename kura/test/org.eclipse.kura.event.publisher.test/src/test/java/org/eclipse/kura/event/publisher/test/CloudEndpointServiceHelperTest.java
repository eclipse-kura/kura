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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.event.publisher.helper.CloudEndpointServiceHelper;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class CloudEndpointServiceHelperTest {

    private CloudEndpointServiceHelper helper;
    private BundleContext mockContext;
    private CloudEndpoint mockCloudEndpoint = mock(CloudEndpoint.class);
    private Exception occurredException;

    /*
     * Scenarios
     */

    /*
     * Steps
     */

    @Test
    public void trackedEndpointShouldPublish() throws InvalidSyntaxException, KuraException {
        givenCloudEndpointServiceHelper();
        givenCloudEndpointTracked();

        whenPublish();

        thenNoExceptionOccurred();
        thenCloudEndpointPublishes(1);
    }

    @Test
    public void untrackedEndpointShouldNotPublish() throws InvalidSyntaxException, KuraException {
        givenCloudEndpointServiceHelper();

        whenPublish();

        thenKuraException();
        thenCloudEndpointPublishes(0);
    }

    @Test
    public void closedHelperShouldNotPublish() throws InvalidSyntaxException, KuraException {
        givenCloudEndpointServiceHelper();
        givenCloudEndpointTracked();
        givenClosedHelper();

        whenPublish();

        thenKuraException();
        thenCloudEndpointPublishes(0);
    }

    @Test
    public void removedCloudEndpointShouldNotPublish() throws InvalidSyntaxException, KuraException {
        givenCloudEndpointServiceHelper();
        givenCloudEndpointTracked();
        givenOnCloudEndpointRemoved();

        whenPublish();

        thenKuraException();
        thenCloudEndpointPublishes(0);
    }

    /*
     * Given
     */

    private void givenCloudEndpointServiceHelper() throws InvalidSyntaxException {
        mockContext = mock(BundleContext.class);
        Filter mockFilter = mock(Filter.class);

        when(mockContext.createFilter(Mockito.any())).thenReturn(mockFilter);
        when(mockContext.getService(Mockito.any())).thenReturn(mockCloudEndpoint);
        when(mockContext.ungetService(Mockito.any())).thenReturn(true);

        this.helper = new CloudEndpointServiceHelper(mockContext, "example-cs");
    }

    private void givenCloudEndpointTracked() {
        this.helper.onCloudEndpointAdded(this.mockCloudEndpoint);
    }

    private void givenClosedHelper() {
        this.helper.close();
    }

    private void givenOnCloudEndpointRemoved() {
        this.helper.onCloudEndpointRemoved(this.mockCloudEndpoint);
    }

    /*
     * When
     */

    private void whenPublish() {
        try {
            this.helper.publish(new KuraMessage(new KuraPayload()));
        }catch(Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionOccurred() {
        assertNull(this.occurredException);
    }

    private void thenKuraException() {
        assertTrue(this.occurredException instanceof KuraException);
    }

    private void thenCloudEndpointPublishes(int expectedPublishes) throws KuraException {
        verify(this.mockCloudEndpoint, times(expectedPublishes)).publish(Mockito.any());
    }

    /*
     * Utilities
     */

    @Before
    public void cleanup() {
        this.occurredException = null;
    }

}
