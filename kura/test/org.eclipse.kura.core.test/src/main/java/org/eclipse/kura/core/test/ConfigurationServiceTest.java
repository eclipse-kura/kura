/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudCallService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.CloudConfigurationHandler;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.test.util.CoreTestXmlUtil;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.eclipse.kura.util.service.ServiceUtil;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class ConfigurationServiceTest extends TestCase implements IConfigurationServiceTest, ConfigurableComponent {

    private static final Logger s_logger = LoggerFactory.getLogger(ConfigurationServiceTest.class);

    private static CountDownLatch dependencyLatch = new CountDownLatch(4);	// initialize with number of dependencies
    private static Object lock = new Object(); // initialize with number of dependencies
    private static ConfigurationService configService;
    private static CloudCallService cloudCallService;
    private static SystemService systemService;
    private static DataService dataService;

    @SuppressWarnings("unused")
    private static ComponentContext componentContext;

    private static Map<String, Object> s_properties;

    private static long updateTime = 0;

    @Override
    public void setUp() {
        // Wait for OSGi dependencies
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);
            if (!ok) {
                fail("Dependencies not resolved!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        ConfigurationServiceTest.configService = configurationService;
        dependencyLatch.countDown();
    }

    public void setCloudCallService(CloudCallService cloudCallService) {
        ConfigurationServiceTest.cloudCallService = cloudCallService;
        dependencyLatch.countDown();
    }

    public void setSystemService(SystemService systemService) {
        ConfigurationServiceTest.systemService = systemService;
        dependencyLatch.countDown();
    }

    public void setDataService(DataService dataService) {
        ConfigurationServiceTest.dataService = dataService;
        dependencyLatch.countDown();
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        ConfigurationServiceTest.configService = null;
    }

    public void unsetCloudCallService(CloudCallService cloudCallService) {
        ConfigurationServiceTest.cloudCallService = null;
    }

    public void unsetSystemService(SystemService systemService) {
        ConfigurationServiceTest.systemService = null;
    }

    public void unsetDataService(DataService dataService) {
        ConfigurationServiceTest.dataService = null;
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(ConfigurationServiceTest.configService);
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("ConfigurationServiceTest.activate...");
        ConfigurationServiceTest.componentContext = componentContext;
        s_properties = properties;
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("ConfigurationServiceTest.deactivate...");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated called: " + properties);

        Set<Entry<String, Object>> entries = properties.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            System.err.println("\t\t" + entry.getKey() + " = " + entry.getValue());
        }

        s_properties = properties;
        updateTime = System.currentTimeMillis();
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testLocalConfiguration() throws Exception {
        String pid = "org.eclipse.kura.core.test.IConfigurationServiceTest";
        s_logger.info("configService 1:" + ConfigurationServiceTest.configService);
        synchronized (lock) {
            lock.wait(5000);
        }

        s_logger.info("Asserting default values...");

        //
        // test the default properties
        assertDefaultValues(s_properties);

        //
        // take a snapshot
        s_logger.info("configService 2:" + ConfigurationServiceTest.configService);
        s_logger.info("Taking snapshot...");

        long sid;
        sid = ConfigurationServiceTest.configService.snapshot();

        //
        // test a positive update flow
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        Set<String> keys = s_properties.keySet();
        for (String key : keys) {
            props.put(key, s_properties.get(key));
        }
        props.put("prop.string", "string_prop");
        props.put("prop.long", 9999L);
        props.put("prop.double", 99.99D);
        props.put("prop.float", 99.99F);
        props.put("prop.integer", 99999);
        props.put("prop.character", '9');
        props.put("prop.boolean", false);

        short s9 = (short) 9;
        props.put("prop.short", s9);

        byte b9 = (byte) 9;
        props.put("prop.byte", b9);

        s_logger.info("configService 3:" + ConfigurationServiceTest.configService);
        s_logger.info("Updating configuration with new values for " + pid + " with props: " + props);
        long previousUpdateTime = System.currentTimeMillis();
        ConfigurationServiceTest.configService.updateConfiguration(pid, props);
        assertTrue(waitForConfigurationUpdate(previousUpdateTime));

        s_logger.info("Asserting values...");
        assertEquals("string_prop", s_properties.get("prop.string"));
        assertEquals(9999L, s_properties.get("prop.long"));
        assertEquals(99.99D, s_properties.get("prop.double"));
        assertEquals(99.99F, s_properties.get("prop.float"));
        assertEquals(99999, s_properties.get("prop.integer"));
        assertEquals('9', s_properties.get("prop.character"));
        assertEquals(false, s_properties.get("prop.boolean"));
        assertEquals(s9, s_properties.get("prop.short"));
        assertEquals(b9, s_properties.get("prop.byte"));

        // test a negative update flow
        props.clear();
        keys = s_properties.keySet();
        for (String key : keys) {
            props.put(key, s_properties.get(key));
        }
        props.put("prop.long", "AAAA");
        try {
            ConfigurationServiceTest.configService.updateConfiguration(pid, props);
            assertFalse("Configuration update should have failed", false);
        } catch (KuraException e) {
            assertTrue("Configuration update has failed as expected", true);
            assertEquals(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e.getCode());
        }

        // test a negative update flow
        props.clear();
        keys = s_properties.keySet();
        for (String key : keys) {
            props.put(key, s_properties.get(key));
        }
        props.remove("prop.string");
        try {
            ConfigurationServiceTest.configService.updateConfiguration(pid, props);
            assertFalse("Configuration update should have failed", false);
        } catch (KuraException e) {
            assertTrue("Configuration update has failed as expected", true);
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
        }

        //
        // restore a snapshot
        previousUpdateTime = System.currentTimeMillis();

        s_logger.info("Rolling back...");
        ConfigurationServiceTest.configService.rollback(sid);

        // Wait for a configuration update
        assertTrue(waitForConfigurationUpdate(previousUpdateTime));
        assertDefaultValues(s_properties);
    }

    private boolean waitForConnection() throws InterruptedException {
        int attempts = 10;
        while (attempts > 0) {
            if (cloudCallService.isConnected()) {
                return true;
            }
            Thread.sleep(1000);
            attempts--;
        }
        return false;
    }

    private boolean waitForConfigurationUpdate(long previousUpdateTime) throws InterruptedException {
        synchronized (lock) {
            if (previousUpdateTime < updateTime) {
                return true;
            }
            lock.wait(30000);
            return previousUpdateTime < updateTime;
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testRemoteConfiguration() throws Exception {
        if (!dataService.isConnected()) {
            dataService.connect();
        }
        assertTrue(cloudCallService.isConnected());
        assertDefaultValues(s_properties);

        s_logger.info("Starting testRemoteConfiguration");

        String pid = "org.eclipse.kura.core.test.IConfigurationServiceTest";

        // load the current configuration
        s_logger.info("loading the current configuration");

        StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString()).append("/")
                .append(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS).append("/").append(pid);

        KuraResponsePayload resp = cloudCallService.call(CloudConfigurationHandler.APP_ID, sb.toString(), null, 10000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
        assertNotNull(resp.getBody());

        // unmarshall the response
        String s = new String(resp.getBody(), "UTF-8");
        XmlComponentConfigurations xmlConfigs = unmarshalXml(s, XmlComponentConfigurations.class);

        System.err.println("Checking current configuration");
        List<ComponentConfiguration> configs = xmlConfigs.getConfigurations();
        assertDefaultValues(configs.get(0).getConfigurationProperties());

        // take a snapshot
        System.err.println("taking a snapshot");

        sb = new StringBuilder(CloudletTopic.Method.EXEC.toString()).append("/")
                .append(CloudConfigurationHandler.RESOURCE_SNAPSHOT);

        resp = cloudCallService.call(CloudConfigurationHandler.APP_ID, sb.toString(), null, 10000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
        assertNotNull(resp.getBody());

        // unmarshall the response
        s = new String(resp.getBody(), "UTF-8");
        StringReader sr = new StringReader(s);
        // XmlSnapshotIdResult snapshotIds = XmlUtil.unmarshal(sr, XmlSnapshotIdResult.class);
        XmlSnapshotIdResult snapshotIds = CoreTestXmlUtil.unmarshal(sr, XmlSnapshotIdResult.class);

        s_logger.info("validating configuration");
        assertNotNull(snapshotIds);
        assertEquals(1, snapshotIds.getSnapshotIds().size());

        long sid = snapshotIds.getSnapshotIds().get(0);

        // modify the configuration
        s_logger.info("modifying configuration");
        ComponentConfigurationImpl ccnew = new ComponentConfigurationImpl();
        ccnew.setPid(pid);
        Hashtable<String, Object> propsnew = new Hashtable<String, Object>();
        propsnew.put("prop.string", "modified_value");
        ccnew.setProperties(propsnew);

        XmlComponentConfigurations newConfigs = new XmlComponentConfigurations();
        List<ComponentConfiguration> newccs = new ArrayList<ComponentConfiguration>();
        newccs.add(ccnew);
        newConfigs.setConfigurations(newccs);

        // TODO: solve this
        String result = marshalXml(newConfigs);

        KuraPayload payload = new KuraPayload();
        if (result != null) {
            payload.setBody(result.getBytes());
        }

        sb = new StringBuilder(CloudletTopic.Method.PUT.toString()).append("/")
                .append(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS).append("/").append(pid);

        long previousUpdateTime = this.updateTime;

        resp = cloudCallService.call(CloudConfigurationHandler.APP_ID, sb.toString(), payload, 10000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        assertTrue(waitForConfigurationUpdate(previousUpdateTime));

        s_logger.info("validating modified configuration");
        s_logger.info("Checking these are equal: " + s_properties.get("prop.string") + " AND " + "modified_value");
        assertEquals("modified_value", s_properties.get("prop.string"));

        // reload the current configuration
        s_logger.info("reloading the current configuration");

        sb = new StringBuilder(CloudletTopic.Method.GET.toString()).append("/")
                .append(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS).append("/").append(pid);

        resp = cloudCallService.call(CloudConfigurationHandler.APP_ID, sb.toString(), null, 10000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
        assertNotNull(resp.getBody());

        // unmarshall the response
        s = new String(resp.getBody(), "UTF-8");
        System.err.println(s);

        xmlConfigs = unmarshalXml(s, XmlComponentConfigurations.class);

        s_logger.info("validating modified configuration");
        assertNotNull(xmlConfigs);
        ComponentConfiguration ccmod = xmlConfigs.getConfigurations().get(0);
        s_logger.info("Checking these are equal: " + ccmod.getConfigurationProperties().get("prop.string") + " AND "
                + "modified_value");
        assertEquals("modified_value", ccmod.getConfigurationProperties().get("prop.string"));

        // rollback
        sb = new StringBuilder(CloudletTopic.Method.EXEC.toString()).append("/")
                .append(CloudConfigurationHandler.RESOURCE_ROLLBACK).append("/").append(sid);

        previousUpdateTime = System.currentTimeMillis();

        resp = cloudCallService.call(CloudConfigurationHandler.APP_ID, sb.toString(), null, 10000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        // Wait for everything to get stable
        assertTrue(waitForConfigurationUpdate(previousUpdateTime));
        assertDefaultValues(s_properties);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSnapshotsMaxCount() throws Exception {
        int maxCount = ConfigurationServiceTest.systemService.getKuraSnapshotsCount();
        for (int i = 0; i < maxCount * 2; i++) {
            ConfigurationServiceTest.configService.snapshot();
        }

        Set<Long> sids = ConfigurationServiceTest.configService.getSnapshots();
        assertEquals(maxCount, sids.size());
    }

    private void assertDefaultValues(Map<String, Object> properties) {
        // scalar properties
        assertEquals("prop.string.value", properties.get("prop.string"));
        assertEquals(1351589588L, properties.get("prop.long"));
        assertEquals(13515895.9999988, properties.get("prop.double"));
        assertEquals(3.14F, properties.get("prop.float"));
        assertEquals(314, properties.get("prop.integer"));
        assertEquals('c', properties.get("prop.character"));
        assertEquals(true, properties.get("prop.boolean"));

        short s = (short) 255;
        assertEquals(s, properties.get("prop.short"));

        byte b = (byte) 7;
        assertEquals(b, properties.get("prop.byte"));

        // array properties
        String[] stringValues = new String[] { "value1", "value2", "value3" };
        assertTrue(Arrays.equals(stringValues, (String[]) properties.get("prop.string.array")));

        Long[] longValues = new Long[] { 1351589588L, 1351589589L, 1351589590L };
        assertTrue(Arrays.equals(longValues, (Long[]) properties.get("prop.long.array")));

        Double[] doubleValues = new Double[] { 13515895.88, 13515895.89, 13515895.90 };
        assertTrue(Arrays.equals(doubleValues, (Double[]) properties.get("prop.double.array")));

        Float[] floatValues = new Float[] { 3.14F, 3.15F, 3.16F };
        assertTrue(Arrays.equals(floatValues, (Float[]) properties.get("prop.float.array")));

        Integer[] intValues = new Integer[] { 314, 315, 316 };
        assertTrue(Arrays.equals(intValues, (Integer[]) properties.get("prop.integer.array")));

        Character[] charValues = new Character[] { 'c', 'd', 'e' };
        assertTrue(Arrays.equals(charValues, (Character[]) properties.get("prop.character.array")));

        Boolean[] boolValues = new Boolean[] { true, false, true };
        assertTrue(Arrays.equals(boolValues, (Boolean[]) properties.get("prop.boolean.array")));

        Short[] shortValues = new Short[] { (short) 253, (short) 254, (short) 255 };
        assertTrue(Arrays.equals(shortValues, (Short[]) properties.get("prop.short.array")));

        Byte[] byteValues = new Byte[] { (byte) 7, (byte) 8, (byte) 9 };
        assertTrue(Arrays.equals(byteValues, (Byte[]) properties.get("prop.byte.array")));
    }

    private ServiceReference<Marshaller>[] getXmlMarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(componentContext.getBundleContext(), Marshaller.class, filterString);
    }

    private ServiceReference<Unmarshaller>[] getXmlUnmarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(componentContext.getBundleContext(), Unmarshaller.class, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(componentContext.getBundleContext(), refs);
    }

    private <T> T unmarshalXml(String xmlString, Class<T> clazz) throws KuraException {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getXmlUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = componentContext.getBundleContext().getService(unmarshallerSR);
                result = unmarshaller.unmarshal(xmlString, clazz);
            }
        } catch (Exception e) {
            s_logger.warn("Failed to extract persisted configuration.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }
        if (result == null) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        }
        return result;
    }

    private String marshalXml(Object object) {
        String result = null;
        ServiceReference<Marshaller>[] marshallerSRs = getXmlMarshallers();
        try {
            for (final ServiceReference<Marshaller> marshallerSR : marshallerSRs) {
                Marshaller marshaller = componentContext.getBundleContext().getService(marshallerSR);
                result = marshaller.marshal(object);
            }
        } catch (Exception e) {
            s_logger.warn("Failed to marshal configuration.");
        } finally {
            ungetServiceReferences(marshallerSRs);
        }
        return result;
    }
}
