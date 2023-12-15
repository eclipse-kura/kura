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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.junit.Test;

public class CloudDeliveryListenerTest extends StepsCollection {

    private CloudDeliveryListener cloudDeliveryListener = mock(CloudDeliveryListener.class);

    /*
     * Scenarios
     */

    @Test
    public void shouldNotifyOnMessageConfirmed() {
        givenCloudDeliveryListener(this.cloudDeliveryListener);

        whenOnMessageConfirmed(121, "test");

        thenCloudDeliveryListenerNotifiedOnMessageConfirmed("121", 1);
    }

    @Test
    public void shouldNotNotifyOnMessageConfirmed() {
        givenCloudDeliveryListener(this.cloudDeliveryListener);
        givenUnregisterCloudDeliveryListener(this.cloudDeliveryListener);

        whenOnMessageConfirmed(121, "test");

        thenCloudDeliveryListenerNotifiedOnMessageConfirmed("121", 0);
    }

    /*
     * Steps
     */

    private void thenCloudDeliveryListenerNotifiedOnMessageConfirmed(String expectedMessageId, int expectedTimes) {
        verify(this.cloudDeliveryListener, times(expectedTimes)).onMessageConfirmed(expectedMessageId);
    }

}
