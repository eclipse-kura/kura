/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.cloud.CloudPayloadEncoding;
import org.eclipse.kura.system.SystemService;
import org.junit.BeforeClass;
import org.junit.Test;

public class CloudServiceOptionsTest {

    private static final String TEST_TOPIC_CONTROL_PREFIX = "newTopicControlPrefix";
    private static final String TOPIC_CONTROL_PREFIX = "topic.control-prefix";
    private static final String TOPIC_CONTROL_PREFIX_DEFAULT = "$EDC";

    private static final String DEVICE_DISPLAY_NAME = "device.display-name";
    private static final String DEVICE_CUSTOM_NAME = "device.custom-name";
    private static final String ENCODE_GZIP = "encode.gzip";
    private static final String REPUB_BIRTH_ON_GPS_LOCK = "republish.mqtt.birth.cert.on.gps.lock";
    private static final String REPUB_BIRTH_ON_MODEM_DETECT = "republish.mqtt.birth.cert.on.modem.detect";
    private static final String ENABLE_DFLT_SUBSCRIPTIONS = "enable.default.subscriptions";
    private static final String BIRTH_CERT_POLICY = "birth.cert.policy";
    private static final String PAYLOAD_ENCODING = "payload.encoding";

    private static SystemService systemService;
    private static CloudServiceOptions options;

    @BeforeClass
    public static void testSetup() {
        systemService = mock(SystemService.class);

        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "device-name");
        properties.put(ENCODE_GZIP, true);
        properties.put(REPUB_BIRTH_ON_GPS_LOCK, true);
        properties.put(REPUB_BIRTH_ON_MODEM_DETECT, true);
        properties.put(TOPIC_CONTROL_PREFIX, TEST_TOPIC_CONTROL_PREFIX);
        properties.put(ENABLE_DFLT_SUBSCRIPTIONS, false);
        properties.put(BIRTH_CERT_POLICY, "disable");
        properties.put(PAYLOAD_ENCODING, "simple-json");

        options = new CloudServiceOptions(properties, systemService);
    }

    @Test
    public void testGetDeviceDisplayNameNullProps() {
        SystemService systemService = mock(SystemService.class);

        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals("", deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameDeviceName() {
        String deviceName = "DeviceName";
        when(systemService.getDeviceName()).thenReturn(deviceName);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals(deviceName, deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameHostnameOther() {
        when(systemService.getOsName()).thenReturn("Other");
        when(systemService.getHostname()).thenReturn("UNKNOWN");

        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "hostname");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals("UNKNOWN", deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameCustomNullProp() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "custom");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals("", deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameCustomInteger() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "custom");
        properties.put(DEVICE_CUSTOM_NAME, 1);

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals("", deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameCustom() {
        String testDeviceCustomName = "test";

        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "custom");
        properties.put(DEVICE_CUSTOM_NAME, testDeviceCustomName);

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals(testDeviceCustomName, deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameServer() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "server");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals("", deviceDisplayName);
    }

    @Test
    public void testGetDeviceDisplayNameNotValid() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_DISPLAY_NAME, "invalid");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String deviceDisplayName = options.getDeviceDisplayName();

        assertNotNull(deviceDisplayName);
        assertEquals("", deviceDisplayName);
    }

    @Test
    public void testGetEncodeGZipNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        boolean gzip = options.getEncodeGzip();

        assertFalse(gzip);
    }

    @Test
    public void testGetEncodeGZipPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean gzip = options.getEncodeGzip();

        assertFalse(gzip);
    }

    @Test
    public void testGetEncodeGZipPropNotBoolean() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ENCODE_GZIP, "invalid");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean gzip = options.getEncodeGzip();

        assertFalse(gzip);
    }

    @Test
    public void testGetEncodeGZip() {

        boolean gzip = options.getEncodeGzip();

        assertTrue(gzip);
    }

    @Test
    public void testGetRepubBirthCertOnGpsLockNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        boolean republish = options.getRepubBirthCertOnGpsLock();

        assertFalse(republish);
    }

    @Test
    public void testGetRepubBirthCertOnGpsLockPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean republish = options.getRepubBirthCertOnGpsLock();

        assertFalse(republish);
    }

    @Test
    public void testGetRepubBirthCertOnGpsLockPropNotBoolean() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(REPUB_BIRTH_ON_GPS_LOCK, "invalid");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean republish = options.getRepubBirthCertOnGpsLock();

        assertFalse(republish);
    }

    @Test
    public void testGetRepubBirthCertOnGpsLock() {

        boolean republish = options.getRepubBirthCertOnGpsLock();

        assertTrue(republish);
    }

    @Test
    public void testGetRepubBirthCertOnModemDetectionNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        boolean republish = options.getRepubBirthCertOnModemDetection();

        assertFalse(republish);
    }

    @Test
    public void testGetRepubBirthCertOnModemDetectionPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean republish = options.getRepubBirthCertOnModemDetection();

        assertFalse(republish);
    }

    @Test
    public void testGetRepubBirthCertOnModemDetectionPropNotBoolean() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(REPUB_BIRTH_ON_MODEM_DETECT, "invalid");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean republish = options.getRepubBirthCertOnModemDetection();

        assertFalse(republish);
    }

    @Test
    public void testGetRepubBirthCertOnModemDetection() {

        boolean republish = options.getRepubBirthCertOnModemDetection();

        assertTrue(republish);
    }

    @Test
    public void testGetTopicControlPrefixNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        String topicControlPrefix = options.getTopicControlPrefix();

        assertNotNull(topicControlPrefix);
        assertEquals(TOPIC_CONTROL_PREFIX_DEFAULT, topicControlPrefix);
    }

    @Test
    public void testGetTopicControlPrefixPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String topicControlPrefix = options.getTopicControlPrefix();

        assertNotNull(topicControlPrefix);
        assertEquals(TOPIC_CONTROL_PREFIX_DEFAULT, topicControlPrefix);
    }

    @Test
    public void testGetTopicControlPrefixPropNotString() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(TOPIC_CONTROL_PREFIX, true);

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        String topicControlPrefix = options.getTopicControlPrefix();

        assertNotNull(topicControlPrefix);
        assertEquals(TOPIC_CONTROL_PREFIX_DEFAULT, topicControlPrefix);
    }

    @Test
    public void testGetTopicControlPrefix() {

        String topicControlPrefix = options.getTopicControlPrefix();

        assertNotNull(topicControlPrefix);
        assertEquals(TEST_TOPIC_CONTROL_PREFIX, topicControlPrefix);
    }

    @Test
    public void testGetEnableDefaultSubscriptionsNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        boolean enable = options.getEnableDefaultSubscriptions();

        assertTrue(enable);
    }

    @Test
    public void testGetEnableDefaultSubscriptionsPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.getEnableDefaultSubscriptions();

        assertTrue(enable);
    }

    @Test
    public void testGetEnableDefaultSubscriptionsPropNotBoolean() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ENABLE_DFLT_SUBSCRIPTIONS, "invalid");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.getEnableDefaultSubscriptions();

        assertTrue(enable);
    }

    @Test
    public void testEnableDefaultSubscriptions() {

        boolean enable = options.getEnableDefaultSubscriptions();

        assertFalse(enable);
    }

    @Test
    public void testGetLifecycleCertsDisabledNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        boolean enable = options.isLifecycleCertsDisabled();

        assertFalse(enable);
    }

    @Test
    public void testGetLifecycleCertsDisabledPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.isLifecycleCertsDisabled();

        assertFalse(enable);
    }

    @Test
    public void testGetLifecycleCertsDisabledPropNotString() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BIRTH_CERT_POLICY, true);

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.isLifecycleCertsDisabled();

        assertFalse(enable);
    }

    @Test
    public void testLifecycleCertsDisabled() {

        boolean enable = options.isLifecycleCertsDisabled();

        assertTrue(enable);
    }

    @Test
    public void testGetRepubBirthCertOnReconnectNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        boolean enable = options.getRepubBirthCertOnReconnect();

        assertFalse(enable);
    }

    @Test
    public void testGetRepubBirthCertOnReconnectPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.getRepubBirthCertOnReconnect();

        assertFalse(enable);
    }

    @Test
    public void testGetRepubBirthCertOnReconnectPropNotString() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BIRTH_CERT_POLICY, true);

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.getRepubBirthCertOnReconnect();

        assertFalse(enable);
    }

    @Test
    public void testRepubBirthCertOnReconnectBirthConnectReconnect() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BIRTH_CERT_POLICY, "birth-connect-reconnect");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.getRepubBirthCertOnReconnect();

        assertTrue(enable);
    }

    @Test
    public void testRepubBirthCertOnReconnectBirthConnect() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BIRTH_CERT_POLICY, "birth-connect");

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        boolean enable = options.getRepubBirthCertOnReconnect();

        assertFalse(enable);
    }

    @Test
    public void testGetPayloadEncodingNullProps() {
        CloudServiceOptions options = new CloudServiceOptions(null, systemService);

        CloudPayloadEncoding cloudPayloadEncoding = options.getPayloadEncoding();

        assertNotNull(cloudPayloadEncoding);
        assertEquals(CloudPayloadEncoding.KURA_PROTOBUF, cloudPayloadEncoding);
    }

    @Test
    public void testGetPayloadEncodingPropNull() {
        Map<String, Object> properties = new HashMap<>();

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        CloudPayloadEncoding cloudPayloadEncoding = options.getPayloadEncoding();

        assertNotNull(cloudPayloadEncoding);
        assertEquals(CloudPayloadEncoding.KURA_PROTOBUF, cloudPayloadEncoding);
    }

    @Test
    public void testGetPayloadEncodingPropNotString() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(TOPIC_CONTROL_PREFIX, true);

        CloudServiceOptions options = new CloudServiceOptions(properties, systemService);

        CloudPayloadEncoding cloudPayloadEncoding = options.getPayloadEncoding();

        assertNotNull(cloudPayloadEncoding);
        assertEquals(CloudPayloadEncoding.KURA_PROTOBUF, cloudPayloadEncoding);
    }

    @Test
    public void testGetPayloadEncoding() {

        CloudPayloadEncoding cloudPayloadEncoding = options.getPayloadEncoding();

        assertNotNull(cloudPayloadEncoding);
        assertEquals(CloudPayloadEncoding.SIMPLE_JSON, cloudPayloadEncoding);
    }
}
