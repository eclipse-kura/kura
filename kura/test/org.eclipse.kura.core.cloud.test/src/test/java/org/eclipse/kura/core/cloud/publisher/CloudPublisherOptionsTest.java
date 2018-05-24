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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.core.message.MessageType;
import org.junit.Test;

public class CloudPublisherOptionsTest {

    @Test(expected = NullPointerException.class)
    public void testEmptyProps() {
        new CloudPublisherOptions(null);
    }

    @Test
    public void testNotAllProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConnectionConstants.CLOUD_CONNECTION_SERVICE_PID_PROP_NAME.value(),
                "org.eclipse.kura.cloud.CloudService");
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertNotNull(options.getCloudServicePid());
        assertNotNull(options.getQos());
    }

    @Test
    public void testGetCloudServicePid() {
        Map<String, Object> props = new HashMap<>();
        String cloudServicePid = "org.eclipse.kura.cloud.CloudService1";
        props.put(CloudConnectionConstants.CLOUD_CONNECTION_SERVICE_PID_PROP_NAME.value(), cloudServicePid);
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(cloudServicePid, options.getCloudServicePid());
    }

    @Test
    public void testGetAppId() {
        String appId = "W2";

        Map<String, Object> props = new HashMap<>();
        props.put("appId", appId);
        
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(appId, options.getAppId());
    }
    
    @Test
    public void testGetAppTopic() {
        String appTopic = "A2/$assetName";

        Map<String, Object> props = new HashMap<>();
        props.put("app.topic", appTopic);
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(appTopic, options.getAppTopic());
    }
    
    @Test
    public void testGetQos() {
        int qos = 1;

        Map<String, Object> props = new HashMap<>();
        props.put("qos", qos);
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(qos, options.getQos());
    }
    
    @Test
    public void testIsRetain() {
        boolean retain = true;

        Map<String, Object> props = new HashMap<>();
        props.put("retain", retain);
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(retain, options.isRetain());
    }
    
    @Test
    public void testMessageType() {
        MessageType messageType = MessageType.CONTROL;

        Map<String, Object> props = new HashMap<>();
        props.put("message.type", "control");
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(messageType.name(), options.getMessageType().name());
    }
    
    @Test
    public void testGetPriority() {
        int priority= 1;

        Map<String, Object> props = new HashMap<>();
        props.put("priority", priority);
        CloudPublisherOptions options = new CloudPublisherOptions(props);
        assertNotNull(options);
        assertEquals(priority, options.getPriority());
    }

}
