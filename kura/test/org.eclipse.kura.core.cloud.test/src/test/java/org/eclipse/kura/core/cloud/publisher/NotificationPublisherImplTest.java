/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.cloud.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.cloud.CloudServiceImpl;
import org.eclipse.kura.core.cloud.CloudServiceOptions;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.mockito.Matchers;

public class NotificationPublisherImplTest {

    private static final String MESSAGE_TYPE_KEY = "messageType";

    private static final String REQUESTOR_CLIENT_ID_KEY = "requestorClientId";

    private static final String APP_ID_KEY = "appId";

    @Test(expected = KuraException.class)
    public void testPublishNullCloudService() throws KuraException {
        NotificationPublisherImpl notificationPublisherImpl = new NotificationPublisherImpl(null);

        notificationPublisherImpl.publish(new KuraMessage(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishNullMessage() throws KuraException {
        CloudServiceImpl cloudServiceImpl = mock(CloudServiceImpl.class);
        NotificationPublisherImpl notificationPublisherImpl = new NotificationPublisherImpl(cloudServiceImpl);

        notificationPublisherImpl.publish(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishNullProps() throws KuraException {
        CloudServiceImpl cloudServiceImpl = mock(CloudServiceImpl.class);
        NotificationPublisherImpl notificationPublisherImpl = new NotificationPublisherImpl(cloudServiceImpl);

        KuraPayload payload = new KuraPayload();
        KuraMessage message = new KuraMessage(payload);
        notificationPublisherImpl.publish(message);
    }

    @Test
    public void testPublish() throws KuraException {
        CloudServiceImpl cloudServiceImpl = mock(CloudServiceImpl.class);
        SystemService systemService = mock(SystemService.class);
        DataService dataService = mock(DataService.class);

        Map<String, Object> optionsProps = new HashMap<>();
        CloudServiceOptions options = new CloudServiceOptions(optionsProps, systemService);

        when(cloudServiceImpl.getCloudServiceOptions()).thenReturn(options);
        when(cloudServiceImpl.encodePayload(Matchers.any())).thenReturn(new byte[0]);
        when(cloudServiceImpl.getDataService()).thenReturn(dataService);
        when(cloudServiceImpl.publish(Matchers.any())).thenReturn("1");

        NotificationPublisherImpl notificationPublisherImpl = new NotificationPublisherImpl(cloudServiceImpl);

        Map<String, Object> properties = new HashMap<>();
        properties.put(APP_ID_KEY, "appId");
        properties.put(MESSAGE_TYPE_KEY, "messageType");
        properties.put(REQUESTOR_CLIENT_ID_KEY, "requestorClientId");
        KuraPayload payload = new KuraPayload();
        KuraMessage message = new KuraMessage(payload, properties);
        String messageId = notificationPublisherImpl.publish(message);

        assertNotNull(messageId);
        assertEquals("1", messageId);
    }

}
