/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.configuration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraPartialSuccessException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.xml.marshaller.unmarshaller.XmlMarshallUnmarshallImpl;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

public class ConfigurationServiceTest {

    @Test
    public void testGetFactoryComponentPids() throws NoSuchFieldException, KuraException {
        // test that the returned PIDs are the same as in the service and that they cannot be modified

        String[] expectedPIDs = { "pid1", "pid2", "pid3" };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        for (final String pid : expectedPIDs) {
            cs.registerComponentOCD(pid, null, true, null);
        }

        Set<String> factoryComponentPids = cs.getFactoryComponentPids();

        assertEquals("same length", 3, factoryComponentPids.size());

        Object[] pidsArray = factoryComponentPids.toArray();
        Arrays.sort(pidsArray);

        for (int i = 0; i < pidsArray.length; i++) {
            assertEquals(expectedPIDs[i], expectedPIDs[i], pidsArray[i]);
        }

        try {
            factoryComponentPids.add("unsupported");
            fail("Updating PIDs should not be possible.");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

    @Test
    public void testCreateFactoryConfigurationNulls() {
        // negative test; how null values are handled

        ConfigurationService cs = new ConfigurationServiceImpl();

        String factoryPid = null;
        String pid = null;
        Map<String, Object> properties = null;
        boolean takeSnapshot = false;

        try {
            cs.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

            fail("Exception expected with null pid value.");
        } catch (Exception e) {
            // OK, probably
        }
    }

    @Test
    public void testCreateFactoryExistingPid() throws KuraException, IOException, NoSuchFieldException {
        // negative test; what if existing PID is used

        final String factoryPid = "fpid";
        final String pid = "mypid";
        Map<String, Object> properties = null;
        final boolean takeSnapshot = false;

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        Map<String, String> pids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        pids.put(pid, pid);

        try {
            cs.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

            fail("Exception expected with existing pid value.");
        } catch (Exception e) {
            // OK, probably
        }
    }

    @Test
    public void testCreateFactoryConfigurationConfigException() throws KuraException, IOException {
        // negative test; invalid configuration exception

        final String factoryPid = "fpid";
        final String pid = "mypid";
        Map<String, Object> properties = null;
        final boolean takeSnapshot = false;

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        IOException ioe = new IOException("test");
        when(configAdminMock.createFactoryConfiguration(factoryPid, null)).thenThrow(ioe);

        try {
            cs.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

            fail("Exception expected");
        } catch (KuraException e) {
            // OK
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testCreateFactoryConfigurationNoSnapshot() throws KuraException, IOException {
        // a positive test, without snapshot creation

        final String factoryPid = "fpid";
        final String pid = "mypid";
        Map<String, Object> properties = null;
        final boolean takeSnapshot = false;
        final String caPid = "caPid";

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            // test that protected component registration was called with the proper parameters
            @Override
            synchronized void registerComponentConfiguration(String pid1, String servicePid, String factoryPid1) {
                assertEquals("PIDs match", pid, pid1);
                assertEquals("Service PIDs match", caPid, servicePid);
                assertEquals("PIDs match", factoryPid, factoryPid1);
            }

            // test that snapshot is not made if not configured so
            @Override
            public long snapshot() throws KuraException {
                if (!takeSnapshot) {
                    fail("Snapshot is turned off.");
                }
                return super.snapshot();
            }
        };

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        Configuration cfgMock = mock(Configuration.class);
        when(configAdminMock.createFactoryConfiguration(factoryPid, null)).thenReturn(cfgMock);

        when(cfgMock.getPid()).thenReturn(caPid);

        Configuration cfgMock2 = mock(Configuration.class);
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(cfgMock2);

        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Dictionary<String, Object> dict = (Dictionary<String, Object>) invocation.getArguments()[0];

                assertNotNull(dict);

                assertEquals("one element in properties list - pid", 1, dict.size());

                assertEquals("expected configuration update PID", pid, dict.elements().nextElement());

                return null;
            }
        }).when(cfgMock2).update((Dictionary<String, Object>) anyObject());

        cs.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

        verify(cfgMock2, times(1)).update((Dictionary<String, Object>) anyObject());
    }

    @Test
    public void testCreateFactoryConfigurationMergeProperties() throws KuraException, IOException {
        // a positive test, take passed properties into account, without snapshot creation

        final String factoryPid = "fpid";
        final String pid = "mypid";
        Map<String, Object> properties = new HashMap<>();
        final boolean takeSnapshot = false;
        final String caPid = "caPid";

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            synchronized void registerComponentConfiguration(String pid1, String servicePid, String factoryPid1) {
                // skip this method call
            }
        };

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        Configuration cfgMock = mock(Configuration.class);
        when(configAdminMock.createFactoryConfiguration(factoryPid, null)).thenReturn(cfgMock);

        when(cfgMock.getPid()).thenReturn(caPid);

        Configuration cfgMock2 = mock(Configuration.class);
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(cfgMock2);

        properties.put("key1", "val1");
        properties.put("key2", "val2");

        Mockito.doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Dictionary<String, Object> dict = (Dictionary<String, Object>) invocation.getArguments()[0];

                assertNotNull(dict);

                assertEquals("3 elements in properties list", 3, dict.size());

                assertEquals("additional key", "val1", dict.get("key1"));
                assertEquals("additional key", "val2", dict.get("key2"));

                return null;
            }
        }).when(cfgMock2).update((Dictionary<String, Object>) Matchers.anyObject());

        cs.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

        verify(cfgMock2, Mockito.times(1)).update((Dictionary<String, Object>) Matchers.anyObject());
    }

    @Test
    public void testCreateFactoryConfigurationWithSnapshot() throws KuraException, IOException {
        // a positive test, check only snapshot creation

        final String factoryPid = "fpid";
        final String pid = "mypid";
        Map<String, Object> properties = null;
        final boolean takeSnapshot = true;
        final String caPid = "caPid";

        final boolean[] snapshots = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            // test that protected component registration was called with the proper parameters
            @Override
            synchronized void registerComponentConfiguration(String pid1, String servicePid, String factoryPid1) {
                // skip this method call
            }

            // test that snapshot is not made if not configured so
            @Override
            public long snapshot() throws KuraException {
                snapshots[0] = true;

                return 1L;
            }
        };

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        Configuration cfgMock = mock(Configuration.class);
        when(configAdminMock.createFactoryConfiguration(factoryPid, null)).thenReturn(cfgMock);

        when(cfgMock.getPid()).thenReturn(caPid);

        Configuration cfgMock2 = mock(Configuration.class);
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(cfgMock2);

        assertFalse("snapshots init OK", snapshots[0]);

        cs.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

        assertTrue("snapshot() called", snapshots[0]);
    }

    @Test
    public void testDeleteFactoryConfigurationNulls() throws KuraException {
        // negative test; null pid

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = null;
        boolean takeSnapshot = false;

        try {
            cs.deleteFactoryConfiguration(pid, takeSnapshot);

            fail("Null parameter - exception expected.");
        } catch (KuraException e) {
            assertTrue(e.getMessage().contains("INVALID_PARAMETER"));
        }
    }

    @Test
    public void testDeleteFactoryConfigurationNonFactoryComponent()
            throws KuraException, NoSuchFieldException, IOException, InvalidSyntaxException {

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";
        boolean takeSnapshot = false;

        final Configuration configMock = prepareConfigForDeleteFactoryConfigTests(pid, null);
        final ConfigurationAdmin configAdmin = prepareConfigAdminForDeleteFactoryConfigTests(configMock);

        cs.setConfigurationAdmin(configAdmin);

        try {
            cs.deleteFactoryConfiguration(pid, takeSnapshot);
        } catch (Exception e) {
            fail("Exception not expected.");
        }

        verify(configMock, Mockito.times(0)).delete();
    }

    @Test
    public void testDeleteFactoryConfigurationNonExistingServicePid() throws KuraException, NoSuchFieldException {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();
        cs.setConfigurationAdmin(mock(ConfigurationAdmin.class));

        String pid = "pid";
        boolean takeSnapshot = false;

        try {
            cs.deleteFactoryConfiguration(pid, takeSnapshot);
        } catch (Exception e) {
            fail("Exception not expected.");
        }
    }

    private Configuration prepareConfigForDeleteFactoryConfigTests(final String configPid,
            final String configFactoryPid) {
        if (configPid == null) {
            return null;
        }

        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(ConfigurationService.KURA_SERVICE_PID, configPid);
        Configuration configMock = mock(Configuration.class);
        if (configFactoryPid != null) {
            properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, configFactoryPid);
            when(configMock.getFactoryPid()).thenReturn(configFactoryPid);
        }

        when(configMock.getProperties()).thenReturn(properties);

        when(configMock.getPid()).thenReturn(configPid);

        return configMock;
    }

    private ConfigurationAdmin prepareConfigAdminForDeleteFactoryConfigTests(Configuration config)
            throws IOException, InvalidSyntaxException {
        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);

        if (config != null) {
            when(configAdminMock.listConfigurations(anyObject())).thenReturn(new Configuration[] { config });
        }

        return configAdminMock;
    }

    @Test
    public void testDeleteFactoryConfigurationNoSnapshot()
            throws KuraException, IOException, NoSuchFieldException, InvalidSyntaxException {
        // positive test; pid registered in factory and service pids, configuration delete is expected, no snapshot

        String factoryPid = "fpid";
        final String servicePid = "spid";
        final boolean takeSnapshot = false;

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            synchronized void unregisterComponentConfiguration(String pid) {
                assertEquals("service pid to unregister", servicePid, pid);
            }

            @Override
            public long snapshot() throws KuraException {
                if (!takeSnapshot) {
                    fail("Snapshot is turned off.");
                }

                return 1L;
            }
        };

        final Configuration configMock = prepareConfigForDeleteFactoryConfigTests(servicePid, factoryPid);
        final ConfigurationAdmin configAdmin = prepareConfigAdminForDeleteFactoryConfigTests(configMock);

        cs.setConfigurationAdmin(configAdmin);

        cs.deleteFactoryConfiguration(servicePid, takeSnapshot);

        verify(configMock, Mockito.times(1)).delete();
    }

    @Test
    public void testDeleteFactoryConfigurationWithSnapshot()
            throws KuraException, IOException, NoSuchFieldException, InvalidSyntaxException {
        // positive test; pid registered in factory and service pids, configuration delete is expected, take a snapshot

        String factoryPid = "fpid";
        final String servicePid = "spid";
        final boolean takeSnapshot = true;

        final boolean[] snapshots = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            synchronized void unregisterComponentConfiguration(String pid) {
                assertEquals("service pid to unregister", servicePid, pid);
            }

            @Override
            public long snapshot() throws KuraException {
                snapshots[0] = true;

                return 1L;
            }
        };

        final Configuration configMock = prepareConfigForDeleteFactoryConfigTests(servicePid, factoryPid);
        final ConfigurationAdmin configAdmin = prepareConfigAdminForDeleteFactoryConfigTests(configMock);

        cs.setConfigurationAdmin(configAdmin);

        assertFalse("snapshot still untouched", snapshots[0]);

        cs.deleteFactoryConfiguration(servicePid, takeSnapshot);

        verify(configMock, Mockito.times(1)).delete();
        assertTrue("snapshot taken", snapshots[0]);
    }

    @Test
    public void testGetConfigurableComponentPidsEmpty() {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        Set<String> configurableComponentPids = cs.getConfigurableComponentPids();

        assertEquals("same length", 0, configurableComponentPids.size());

        try {
            configurableComponentPids.add("unsupported");
            fail("Updating PIDs should not be possible.");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

    @Test
    public void testGetConfigurableComponentPids() throws NoSuchFieldException {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String[] expectedPIDs = { "pid1", "pid2", "pid3" };

        Set<String> s = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        s.addAll(Arrays.asList(expectedPIDs));

        Set<String> configurableComponentPids = cs.getConfigurableComponentPids();

        assertEquals("same length", 3, configurableComponentPids.size());

        Object[] pidsArray = configurableComponentPids.toArray();
        Arrays.sort(pidsArray);

        for (int i = 0; i < pidsArray.length; i++) {
            assertEquals(expectedPIDs[i], expectedPIDs[i], pidsArray[i]);
        }

        try {
            configurableComponentPids.add("unsupported");
            fail("Updating PIDs should not be possible.");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

    @Test
    public void testDecryptPasswords() throws KuraException {
        // test password decryption

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        ComponentConfigurationImpl config = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        config.setProperties(props);
        String passStr = "passval1";
        Password pass = new Password(passStr);
        String passKey = "pass1";
        props.put(passKey, pass);
        props.put("k2", "val2");

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        char[] decpass = "decpass".toCharArray();
        when(cryptoServiceMock.decryptAes(passStr.toCharArray())).thenReturn(decpass);

        assertEquals("config size", 2, props.size());

        cs.decryptConfigurationProperties(config.getConfigurationProperties());

        verify(cryptoServiceMock, times(1)).decryptAes(passStr.toCharArray());

        Map<String, Object> result = config.getConfigurationProperties();
        assertNotNull("properties not null", result);
        assertEquals("config properties size", 2, result.size());
        assertTrue("contains password", result.containsKey(passKey));
        assertArrayEquals("decrypted pass OK", decpass, ((Password) result.get(passKey)).getPassword());
        assertArrayEquals("decrypted pass OK - reference", decpass, ((Password) props.get(passKey)).getPassword());
    }

    @Test
    public void testDecryptPasswordsException() throws KuraException {
        // test error in password decryption
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        ComponentConfigurationImpl config = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        config.setProperties(props);
        Password pass = new Password("passval1");
        String passKey = "pass1";
        props.put(passKey, pass);

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        KuraException exc = new KuraException(KuraErrorCode.STORE_ERROR);
        when(cryptoServiceMock.decryptAes((char[]) Matchers.anyObject())).thenThrow(exc);

        assertEquals("config size before decryption", 1, props.size());

        cs.decryptConfigurationProperties(config.getConfigurationProperties());

        verify(cryptoServiceMock, times(1)).decryptAes((char[]) Matchers.anyObject());

        assertEquals("config size after decryption", 1, props.size());
    }

    @Test(expected = NullPointerException.class)
    public void testMergeWithDefaultsNulls() throws KuraException {
        // test with null parameters - null properties means error and NPE is expected

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        OCD ocd = null;
        Map<String, Object> properties = null;

        cs.mergeWithDefaults(ocd, properties);
    }

    @Test
    public void testMergeWithDefaultsEmpty() throws KuraException {
        // empty input

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        OCD ocd = new Tocd();
        Map<String, Object> properties = new HashMap<>();

        boolean merged = cs.mergeWithDefaults(ocd, properties);

        assertFalse("nothing to merge", merged);
        assertEquals("still empty", 0, properties.size());
    }

    @Test
    public void testMergeWithDefaults() throws KuraException {
        // a few default values, a few overrides, one ovelap

        final Map<String, Object> props = new HashMap<>();
        String prop1Key = "prop1";
        String prop1DefValue = "prop1DefValue";
        props.put(prop1Key, prop1DefValue);
        props.put("defKey2", "defValue2");

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            Map<String, Object> getDefaultProperties(OCD ocd) throws KuraException {
                return props;
            }
        };

        Tocd ocd = new Tocd();
        Map<String, Object> properties = new HashMap<>();
        String prop1Value = "value1";
        properties.put(prop1Key, prop1Value);
        properties.put("key2", "value2");

        assertNotEquals(prop1Value, prop1DefValue);

        boolean merged = cs.mergeWithDefaults(ocd, properties);

        assertTrue("properties merged", merged);
        assertEquals("added a property", 3, properties.size());
        assertEquals("value override OK", prop1Value, properties.get(prop1Key));
        assertTrue("value override only", properties.containsKey("key2"));
        assertTrue("default value only", properties.containsKey("defKey2"));
    }

    @Test
    public void testRegisterSelfConfiguringComponentNull() throws NoSuchFieldException {
        // test behavior with null - just abort

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        Set<String> allPidsMock = mock(Set.class);
        TestUtil.setFieldValue(cs, "allActivatedPids", allPidsMock);

        String pid = null;

        when(allPidsMock.contains(Matchers.anyObject())).thenThrow(new RuntimeException());

        cs.registerSelfConfiguringComponent(pid, pid);

        verify(allPidsMock, times(0)).contains(Matchers.anyObject());
    }

    @Test
    public void testRegisterSelfConfiguringComponentNonExistingPID() throws NoSuchFieldException {
        // test behavior with non-existing pid

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        Set<String> allPidsMock = mock(Set.class);
        TestUtil.setFieldValue(cs, "allActivatedPids", allPidsMock);

        String pid = "pid";

        when(allPidsMock.contains(pid)).thenReturn(false);
        when(allPidsMock.add(pid)).thenReturn(true);

        Map<String, String> spbp = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        Set<String> asc = (Set<String>) TestUtil.getFieldValue(cs, "activatedSelfConfigComponents");

        assertEquals("empty service pids", 0, spbp.size());
        assertEquals("empty activated configured components", 0, asc.size());

        cs.registerSelfConfiguringComponent(pid, pid);

        verify(allPidsMock, times(1)).contains(pid);
        verify(allPidsMock, times(1)).add(pid);

        assertEquals("added pid to service pids", 1, spbp.size());
        assertEquals("added pid to activated configured components", 1, asc.size());
    }

    @Test
    public void testRegisterSelfConfiguringComponentExistingPID() throws NoSuchFieldException {
        // test behavior with existing pid

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        Set<String> allPidsMock = mock(Set.class);
        TestUtil.setFieldValue(cs, "allActivatedPids", allPidsMock);

        String pid = "pid";

        when(allPidsMock.contains(pid)).thenReturn(true);
        when(allPidsMock.add(pid)).thenThrow(new RuntimeException());

        Map<String, String> spbp = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        Set<String> asc = (Set<String>) TestUtil.getFieldValue(cs, "activatedSelfConfigComponents");

        assertEquals("empty service pids", 0, spbp.size());
        assertEquals("empty activated configured components", 0, asc.size());

        cs.registerSelfConfiguringComponent(pid, pid);

        verify(allPidsMock, times(1)).contains(pid);
        verify(allPidsMock, times(0)).add(pid);

        assertEquals("not added pid to service pids", 1, spbp.size());
        assertEquals("not added pid to activated configured components", 1, asc.size());
    }

    @Test
    public void testUnregisterComponentConfigurationNull() throws NoSuchFieldException {
        // test behavior with null - just abort

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        Set<String> allPidsMock = mock(Set.class);
        TestUtil.setFieldValue(cs, "allActivatedPids", allPidsMock);

        String pid = null;

        when(allPidsMock.contains(Matchers.anyObject())).thenThrow(new RuntimeException());

        cs.unregisterComponentConfiguration(pid);

        verify(allPidsMock, times(0)).contains(Matchers.anyObject());
    }

    @Test
    public void testUnregisterComponentConfiguration() throws NoSuchFieldException {
        // test behavior with non-existing pid

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";

        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        allPids.add(pid + "1");
        Map<String, String> spbp = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        spbp.put(pid + "1", pid);
        Set<String> asc = (Set<String>) TestUtil.getFieldValue(cs, "activatedSelfConfigComponents");
        asc.add(pid + "1");

        assertEquals("all pids size", 1, allPids.size());
        assertEquals("service pids size", 1, spbp.size());
        assertEquals("activated pids size", 1, asc.size());

        assertFalse("all pids don't contain pid", allPids.contains(pid));
        assertFalse("service pids don't contain pid", spbp.containsKey(pid));
        assertFalse("activated pids don't contain pid", asc.contains(pid));

        cs.unregisterComponentConfiguration(pid);

        // no change
        assertEquals("all pids size", 1, allPids.size());
        assertEquals("service pids size", 1, spbp.size());
        assertEquals("activated pids size", 1, asc.size());

        assertFalse("all pids don't contain pid", allPids.contains(pid));
        assertFalse("service pids don't contain pid", spbp.containsKey(pid));
        assertFalse("activated pids don't contain pid", asc.contains(pid));
    }

    @Test
    public void testEncryptConfigsNull() throws NoSuchMethodException {
        // test with null parameter

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        List<? extends ComponentConfiguration> configs = null;

        try {
            TestUtil.invokePrivate(cs, "encryptConfigs", configs);
        } catch (Throwable e) {
            fail("Parameters not checked.");
        }

    }

    @Test
    public void testEncryptConfigsNoConfigs() throws Throwable {
        // empty list

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        List<? extends ComponentConfiguration> configs = new ArrayList<>();

        TestUtil.invokePrivate(cs, "encryptConfigs", configs);

        // runs without problems, but there's nothing else to check, here
    }

    @Test
    public void testEncryptConfigsEncryptionException() throws Throwable {
        // test failed encryption of a password: add a password and run; fail
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        // first decryption must fail
        when(cryptoServiceMock.decryptAes("pass".toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.DECODER_ERROR));
        // then also encryption can fail
        when(cryptoServiceMock.encryptAes("pass".toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.ENCODE_ERROR));

        List<ComponentConfigurationImpl> configs = new ArrayList<>();

        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", new Password("pass"));
        cfg.setProperties(props);

        configs.add(cfg);

        TestUtil.invokePrivate(cs, "encryptConfigs", configs);

        verify(cryptoServiceMock, times(1)).decryptAes("pass".toCharArray());
        verify(cryptoServiceMock, times(1)).encryptAes("pass".toCharArray());

        assertEquals("property was deleted", 0, props.size());
    }

    @Test
    public void testEncryptConfigs() throws Throwable {
        // test encrypting a password: add a password and run
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        // first decryption must fail
        when(cryptoServiceMock.decryptAes("pass".toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.DECODER_ERROR));
        // so that encryption is attempted at all
        when(cryptoServiceMock.encryptAes("pass".toCharArray())).thenReturn("encrypted".toCharArray());

        List<ComponentConfigurationImpl> configs = new ArrayList<>();

        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", new Password("pass"));
        cfg.setProperties(props);

        configs.add(cfg);

        TestUtil.invokePrivate(cs, "encryptConfigs", configs);

        verify(cryptoServiceMock, times(1)).decryptAes("pass".toCharArray());
        verify(cryptoServiceMock, times(1)).encryptAes("pass".toCharArray());

        assertEquals("property was updated", 1, props.size());
        assertTrue("key still exists", props.containsKey("key1"));
        assertArrayEquals("key is encrypted", "encrypted".toCharArray(), ((Password) props.get("key1")).getPassword());
    }

    @Test
    public void testEncryptConfigsPreencryptedPassword() throws Throwable {
        // test encrypting a password when the password is already encrypted
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        // decryption succeeds this time
        when(cryptoServiceMock.decryptAes("pass".toCharArray())).thenReturn("pass".toCharArray());

        List<ComponentConfigurationImpl> configs = new ArrayList<>();

        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", new Password("pass"));
        cfg.setProperties(props);

        configs.add(cfg);

        TestUtil.invokePrivate(cs, "encryptConfigs", configs);

        verify(cryptoServiceMock, times(1)).decryptAes("pass".toCharArray());
        verify(cryptoServiceMock, times(0)).encryptAes((char[]) Matchers.anyObject());

        assertEquals("property remains", 1, props.size());
        assertTrue("key still exists", props.containsKey("key1"));
        assertArrayEquals("key is already encrypted", "pass".toCharArray(),
                ((Password) props.get("key1")).getPassword());
    }

    @Test
    public void testUpdateConfigurationStringMapOfStringObject() throws Throwable {
        // test delegation

        final String pid = "pid";
        final Map<String, Object> properties = new HashMap<>();

        final boolean[] calls = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public synchronized void updateConfiguration(String pidToUpdate,
                    java.util.Map<String, Object> propertiesToUpdate, boolean takeSnapshot) throws KuraException {

                calls[0] = true;

                assertEquals("pid matches", pid, pidToUpdate);
                assertEquals("properties match", properties, propertiesToUpdate);
            }
        };

        cs.updateConfiguration(pid, properties);

        assertTrue("method called", calls[0]);
    }

    @Test
    public void testUpdateConfigurationStringMapOfStringObjectBoolean() throws Throwable {
        // test delegation

        final String pid = "pid";
        final Map<String, Object> propertiesToUpdate = new HashMap<>();

        final boolean[] calls = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public synchronized void updateConfigurations(List<ComponentConfiguration> configsToUpdate,
                    boolean takeSnapshot) throws KuraException {

                calls[0] = true;

                assertEquals("one configuration added", 1, configsToUpdate.size());

                ComponentConfiguration cfg = configsToUpdate.get(0);
                assertNotNull("new config initialized", cfg);
                assertEquals("pid matches", pid, cfg.getPid());
                assertEquals("properties match", propertiesToUpdate, cfg.getConfigurationProperties());

                assertTrue("take snapshot - true", takeSnapshot);
            }
        };

        cs.updateConfiguration(pid, propertiesToUpdate, true);

        assertTrue("method called", calls[0]);
    }

    @Test
    public void testUpdateConfigurationsListOfComponentConfiguration() throws KuraException {
        // test delegation

        final List<ComponentConfiguration> configs = new ArrayList<>();

        final boolean[] calls = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public synchronized void updateConfigurations(List<ComponentConfiguration> configsToUpdate,
                    boolean takeSnapshot) throws KuraException {

                calls[0] = true;

                assertEquals("list", configs, configsToUpdate);

                assertTrue("take snapshot - true", takeSnapshot);
            }
        };

        cs.updateConfigurations(configs);

        assertTrue("method called", calls[0]);
    }

    @Test
    public void testUpdateConfigurationsListOfComponentConfigurationBoolean()
            throws KuraException, NoSuchFieldException {
        // test that password encryption is attempted (but decrypt doesn't fail, which is OK) and some other calls are
        // made - stop with usage of allActivatedPids in getComponentConfigurationsInternal

        boolean takeSnapshot = false;
        final List<ComponentConfiguration> configs = new ArrayList<>();
        configs.add(null);
        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        cfg.setProperties(props);
        props.put("pass", new Password("pass"));
        configs.add(cfg);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        when(cryptoServiceMock.decryptAes((char[]) anyObject())).thenReturn("dec".toCharArray());

        // make updateConfigurationsInternal fail with NPE
        TestUtil.setFieldValue(cs, "allActivatedPids", null);

        try {
            cs.updateConfigurations(configs, takeSnapshot);
            fail("Exception expected");
        } catch (NullPointerException e) {
            // OK
        }

        verify(cryptoServiceMock, times(1)).decryptAes((char[]) anyObject());
    }

    @Test
    public void testGetSnapshots() throws KuraException {
        // test that lower-level method (getSnapshotsInternal) is called; with no/null snapshot directory
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        Set<Long> snapshots = cs.getSnapshots();

        assertNotNull("list is initialized", snapshots);
        assertEquals("list is empty", 0, snapshots.size());

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
    }

    @Test
    public void testGetSnapshotsInternalNullDir() throws Throwable {
        // test with no/null snapshot directory
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        Set<Long> snapshots = (Set<Long>) TestUtil.invokePrivate(cs, "getSnapshotsInternal");

        assertNotNull("list is initialized", snapshots);
        assertEquals("list is empty", 0, snapshots.size());

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
    }

    @Test
    public void testGetSnapshotsInternalNotDir() throws Throwable {
        // test with non-existing snapshot directory
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsDirectory()).thenReturn("nonExistingDir");

        Set<Long> snapshots = (Set<Long>) TestUtil.invokePrivate(cs, "getSnapshotsInternal");

        assertNotNull("list is initialized", snapshots);
        assertEquals("list is empty", 0, snapshots.size());

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
    }

    @Test
    public void testGetSnapshotsInternal() throws Throwable {
        // test with existing and full snapshot directory
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        String dir = "existingDirGSI";
        when(systemServiceMock.getKuraSnapshotsDirectory()).thenReturn(dir);

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "f1.xml");
        File f2 = new File(dir, "snapshot2.xml");
        File f3 = new File(dir, "snapshot_3.xml");
        File f4 = new File(dir, "snapshot_4_.xml");
        File f5 = new File(dir, "Snapshot_5.XML");

        f1.createNewFile();
        f2.createNewFile();
        f3.createNewFile();
        f4.createNewFile();
        f5.createNewFile();

        f1.deleteOnExit();
        f2.deleteOnExit();
        f3.deleteOnExit();
        f4.deleteOnExit();
        f5.deleteOnExit();

        Set<Long> snapshots = (Set<Long>) TestUtil.invokePrivate(cs, "getSnapshotsInternal");

        f1.delete();
        f2.delete();
        f3.delete();
        f4.delete();
        f5.delete();
        d1.delete();

        assertNotNull("list is initialized", snapshots);
        assertEquals("list has only so many pids", 1, snapshots.size());
        assertEquals("expected pid", 3, (long) snapshots.iterator().next());

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
    }

    @Test
    public void testGetSnapshotFileNull() throws Throwable {
        // test if it works with null directory
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        try {
            Object obj = TestUtil.invokePrivate(cs, "getSnapshotFile", 123);
            assertNull("Null expected to produce null", obj);
        } catch (NullPointerException e) {
            fail("Method result not checked.");
        }

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
    }

    @Test
    public void testGetSnapshotFile() throws Throwable {
        // verify that the file path and name are OK
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        String dir = "dirGSF";
        when(systemServiceMock.getKuraSnapshotsDirectory()).thenReturn(dir);

        File file = (File) TestUtil.invokePrivate(cs, "getSnapshotFile", 123);

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();

        assertTrue("path pattern matches", file.getAbsolutePath().matches(".*dirGSF[/\\\\]snapshot_123.xml$"));
    }

    @Test
    public void testGetSnapshotNullXmlCfgs() throws KuraException {
        // test calling with null configurations list
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
                XmlComponentConfigurations cfgs = null;
                return cfgs;
            }
        };

        long sid = 0;
        try {
            List<ComponentConfiguration> snapshot = cs.getSnapshot(sid);
            assertNotNull("Null not expected", snapshot);
            assertTrue("Should be empty", snapshot.isEmpty());
        } catch (Exception e) {
            fail("Method result not checked.");
        }
    }

    @Test
    public void testGetSnapshotPasswordDecryptionException() throws KuraException {
        // test password decryption failure - log only
        final boolean[] calls = { false, false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
                XmlComponentConfigurations cfgs = new XmlComponentConfigurations();
                List<ComponentConfiguration> configurations = new ArrayList<>();
                cfgs.setConfigurations(configurations);

                configurations.add(null);
                ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
                configurations.add(cfg);

                calls[0] = true;

                return cfgs;
            }

            @Override
            void decryptConfigurationProperties(Map<String, Object> configProps) {
                calls[1] = true;

                throw new RuntimeException("test");
            }
        };

        long sid = 0;
        List<ComponentConfiguration> configs = null;
        try {
            configs = cs.getSnapshot(sid);
        } catch (Exception e) {
            fail("Exception not expected.");
        }

        assertTrue("config loaded", calls[0]);
        assertTrue("passwords decrypted", calls[1]);
        assertNotNull("configurations list exists", configs);
        assertEquals("configurations list filled", 2, configs.size());
        assertNull(configs.get(0));
        assertNotNull(configs.get(1));
    }

    @Test
    public void testGetSnapshot() throws KuraException {
        // test successful run

        final boolean[] calls = { false, false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
                XmlComponentConfigurations cfgs = new XmlComponentConfigurations();
                List<ComponentConfiguration> configurations = new ArrayList<>();
                cfgs.setConfigurations(configurations);

                configurations.add(null);
                ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
                configurations.add(cfg);

                calls[0] = true;

                return cfgs;
            }

            @Override
            void decryptConfigurationProperties(Map<String, Object> configProps) {
                calls[1] = true;
            }
        };

        long sid = 0;
        List<ComponentConfiguration> configs = cs.getSnapshot(sid);

        assertTrue("config loaded", calls[0]);
        assertTrue("passwords decrypted", calls[1]);
        assertNotNull("configurations list exists", configs);
        assertEquals("configurations list filled", 2, configs.size());
        assertNull(configs.get(0));
        assertNotNull(configs.get(1));
    }

    @Test
    public void testLoadEncryptedSnapshotFileContentNoFile() throws KuraException {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        long snapshotID = 123;

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        String dir = "someDir";
        when(systemServiceMock.getKuraSnapshotsDirectory()).thenReturn(dir);

        try {
            cs.loadEncryptedSnapshotFileContent(snapshotID);

            fail("Expected exception: file not found");
        } catch (KuraException e) {
            assertEquals("correct code", KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, e.getCode());
        }

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
    }

    @Test
    public void testLoadEncryptedSnapshotFileContentNullDecrypt() throws KuraException, IOException {
        // test decryption failure while loading an encrypted snapshot

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        long snapshotID = 123;

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        String dir = "someDir";
        when(systemServiceMock.getKuraSnapshotsDirectory()).thenReturn(dir);

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_" + snapshotID + ".xml");
        f1.createNewFile();
        f1.deleteOnExit();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        when(cryptoServiceMock.decryptAes((char[]) anyObject())).thenReturn(null);

        try {
            cs.loadEncryptedSnapshotFileContent(snapshotID);
        } catch (NullPointerException e) {
            fail("Decryption result not checked for null value.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.DECODER_ERROR, e.getCode());
        }

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();

        f1.delete();
        d1.delete();
    }

    @Test
    public void testLoadEncryptedSnapshotFileContent() throws Exception {
        // load an 'encrypted' snapshot file

        String decrypted = prepareSnapshotXML();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        long snapshotID = 123;

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        String dir = "someDir";
        when(systemServiceMock.getKuraSnapshotsDirectory()).thenReturn(dir);

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_" + snapshotID + ".xml");
        f1.createNewFile();
        f1.deleteOnExit();

        FileWriter fw = new FileWriter(f1);
        fw.append("test");
        fw.close();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        // ensure the proper file is read
        when(cryptoServiceMock.decryptAes("test".toCharArray())).thenReturn(decrypted.toCharArray());

        XmlComponentConfigurations configurations = cs.loadEncryptedSnapshotFileContent(snapshotID);

        verify(systemServiceMock, times(1)).getKuraSnapshotsDirectory();
        verify(cryptoServiceMock, times(1)).decryptAes("test".toCharArray());

        f1.delete();
        d1.delete();

        assertNotNull("configurations object is returned", configurations);
        assertNotNull("configurations list is returned", configurations.getConfigurations());
        assertEquals("configurations list is not empty", 1, configurations.getConfigurations().size());

        ComponentConfiguration cfg1 = configurations.getConfigurations().get(0);
        assertEquals("correct snapshot", "123", cfg1.getPid());
        assertNotNull("configuration properties map is returned", cfg1.getConfigurationProperties());
        assertEquals("configuration properties map is not empty", 1, cfg1.getConfigurationProperties().size());
    }

    @Test
    public void testLoadLatestSnapshotConfigurationsNullSnapshots() throws Throwable {
        // test null snapshot pids list

        final Set<Long> snapshotList = null;

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return snapshotList;
            }
        };

        List<ComponentConfigurationImpl> result = (List<ComponentConfigurationImpl>) TestUtil.invokePrivate(cs,
                "loadLatestSnapshotConfigurations");

        assertNull("null result", result);
    }

    @Test
    public void testLoadLatestSnapshotConfigurationsEmptySnapshots() throws Throwable {
        // test empty snapshot pids list

        final Set<Long> snapshotList = new TreeSet<>();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return snapshotList;
            }
        };

        List<ComponentConfigurationImpl> result = (List<ComponentConfigurationImpl>) TestUtil.invokePrivate(cs,
                "loadLatestSnapshotConfigurations");

        assertNull("null result", result);
    }

    @Test
    public void testLoadLatestSnapshotConfigurationsNullXML() throws Throwable {
        // test no XML being returned

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(123L);
        snapshotList.add(1234L);

        final boolean[] calls = { false, false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                calls[0] = true;
                return snapshotList;
            }

            @Override
            XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
                calls[1] = true;

                assertEquals(1234L, snapshotID);

                return null;
            }
        };

        List<ComponentConfigurationImpl> result = (List<ComponentConfigurationImpl>) TestUtil.invokePrivate(cs,
                "loadLatestSnapshotConfigurations");

        assertNull("null result", result);

        assertTrue("call snapshots", calls[0]);
        assertTrue("call load xml", calls[1]);
    }

    @Test
    public void testLoadLatestSnapshotConfigurationsXmlLoads() throws Throwable {
        // test scenario where XML is actually loaded from encrypted file

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(123L);
        snapshotList.add(1234L);

        final XmlComponentConfigurations xmlComponentConfigurations = new XmlComponentConfigurations();
        List<ComponentConfiguration> configurations = new ArrayList<>();
        xmlComponentConfigurations.setConfigurations(configurations);

        final boolean[] calls = { false, false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                calls[0] = true;
                return snapshotList;
            }

            @Override
            XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
                calls[1] = true;

                assertEquals(1234L, snapshotID);

                return xmlComponentConfigurations;
            }
        };

        List<ComponentConfigurationImpl> result = (List<ComponentConfigurationImpl>) TestUtil.invokePrivate(cs,
                "loadLatestSnapshotConfigurations");

        assertNotNull("xml config not null", result);

        assertTrue("call snapshots", calls[0]);
        assertTrue("call load xml", calls[1]);
    }

    @Test
    public void testLoadLatestSnapshotConfigurationsRecursiveAfterEncryption() throws Throwable {
        // test scenario where latest snapshot is not encrypted and all snapshots are encrypted before being loaded

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(123L);
        snapshotList.add(1234L);

        final XmlComponentConfigurations xmlComponentConfigurations = new XmlComponentConfigurations();
        List<ComponentConfiguration> configurations = new ArrayList<>();
        xmlComponentConfigurations.setConfigurations(configurations);

        final String dir = "snapDir";
        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(d1, "snapshot_123.xml");
        f1.createNewFile();
        f1.deleteOnExit();
        File f2 = new File(d1, "snapshot_1234.xml");
        f2.createNewFile();
        f2.deleteOnExit();

        final int[] calls = { 0, 0 };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                calls[0]++;
                if (calls[0] < 3) {
                    return snapshotList;
                } else {
                    return null;
                }
            }

            @Override
            XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
                calls[1]++;
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
            }
        };

        List<ComponentConfigurationImpl> result = (List<ComponentConfigurationImpl>) TestUtil.invokePrivate(cs,
                "loadLatestSnapshotConfigurations");

        assertNull("xml config null", result);

        assertEquals("call snapshots", 4, calls[0]);
        assertEquals("call load xml", 3, calls[1]);
    }

    @Test
    public void testEncryptPlainSnapshotsNoFile() throws Throwable {
        // snapshot file doesn't exist

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(234L);

        final String dir = "snapshotDirEPSNF";

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return snapshotList;
            }

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        try {
            TestUtil.invokePrivate(cs, "encryptPlainSnapshots");
            fail("Exception expected.");
        } catch (KuraException e) {
            assertEquals("exception code OK", KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
        }
    }

    @Test
    public void testEncryptPlainSnapshots() throws Throwable {
        // test that everything works

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(223L);

        // prepare a valid snapshot_123.xml
        String cfgxml = prepareSnapshotXML();

        final String dir = "snapshotDirEPS";
        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_223.xml");
        f1.createNewFile();
        f1.deleteOnExit();

        FileWriter fw = new FileWriter(f1);
        fw.append(cfgxml);
        fw.close();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return snapshotList;
            }

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String encCfg = "encrypted";
        char[] encrypted = encCfg.toCharArray();
        when(cryptoServiceMock.encryptAes((char[]) Matchers.anyObject())).thenReturn(encrypted);

        BundleContext bundleContext = mock(BundleContext.class);
        TestUtil.setFieldValue(cs, "bundleContext", bundleContext);

        TestUtil.invokePrivate(cs, "encryptPlainSnapshots");

        verify(cryptoServiceMock, times(1)).encryptAes((char[]) Matchers.anyObject());

        FileReader fr = new FileReader(f1);
        char[] chars = new char[encCfg.length()];
        int read = fr.read(chars);
        fr.close();

        assertEquals("proper length", encCfg.length(), read);
        assertArrayEquals("proper encrypted contents", encrypted, chars);

        f1.delete();
        d1.delete();
    }

    private String prepareSnapshotXml(final XmlComponentConfigurations configs) throws KuraException {
        XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
        return xmlMarshaller.marshal(configs);
    }

    private String prepareSnapshotXML() throws Exception {
        return prepareSnapshotXml(prepareSnapshot());
    }

    private XmlComponentConfigurations prepareSnapshot(final Map<String, Object> configProps) {
        XmlComponentConfigurations cfgs = new XmlComponentConfigurations();

        List<ComponentConfiguration> cfglist = new ArrayList<>();
        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        cfg.setPid("123");
        cfg.setProperties(configProps);
        Tocd definition = new Tocd();
        definition.setDescription("description");
        cfg.setDefinition(definition);
        cfglist.add(cfg);
        cfgs.setConfigurations(cfglist);

        return cfgs;
    }

    private XmlComponentConfigurations prepareSnapshot() {
        return prepareSnapshot(Collections.singletonMap("pass", "pass"));
    }

    @Test
    public void testWriteSnapshotFileNotFile() throws Throwable {
        // force a FileNotFound exception resulting in internal error KuraException

        long sid = 323L;

        XmlComponentConfigurations cfg = prepareSnapshot();

        final String dir = "snapshotDirWSFNF";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File d2 = new File(d1, "snapshot_" + sid + ".xml");
        d2.mkdirs();
        d2.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String encCfg = "encrypted";
        char[] encrypted = encCfg.toCharArray();
        when(cryptoServiceMock.encryptAes((char[]) Matchers.anyObject())).thenReturn(encrypted);

        try {
            TestUtil.invokePrivate(cs, "writeSnapshot", sid, cfg);
            fail("Exception expected due to 'file' being directory.");
        } catch (KuraException e) {
            assertEquals("Error code.", KuraErrorCode.INTERNAL_ERROR, e.getCode());
        }

        verify(cryptoServiceMock, times(1)).encryptAes((char[]) Matchers.anyObject());

        d1.delete();
        d2.delete();
    }

    @Test
    public void testWriteSnapshot() throws Throwable {
        // test the normal flow

        long sid = 323L;

        XmlComponentConfigurations cfg = prepareSnapshot();

        final String dir = "snapshotDirWS";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String encCfg = "encrypted";
        char[] encrypted = encCfg.toCharArray();
        when(cryptoServiceMock.encryptAes((char[]) Matchers.anyObject())).thenReturn(encrypted);

        BundleContext bundleContext = mock(BundleContext.class);
        TestUtil.setFieldValue(cs, "bundleContext", bundleContext);

        TestUtil.invokePrivate(cs, "writeSnapshot", sid, cfg);

        verify(cryptoServiceMock, times(1)).encryptAes((char[]) Matchers.anyObject());

        File f1 = new File(d1, "snapshot_" + sid + ".xml");
        f1.deleteOnExit();
        assertTrue("snapshot file was created", f1.exists());

        FileReader fr = new FileReader(f1);
        char[] chars = new char[encCfg.length()];
        int read = fr.read(chars);
        fr.close();

        assertEquals("proper length", encCfg.length(), read);
        assertArrayEquals("proper encrypted contents", encrypted, chars);

        f1.delete();
        d1.delete();
    }

    @Test
    public void testGarbageCollectionOldSnapshotsZero() throws Throwable {
        // test scenario where 0 snapshots are configured to remain, but snapshot_0.xml prevents deletion of all of them
        final String dir = "gcosDir0";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f0 = new File(dir, "snapshot_0.xml"); // special snapshot file
        f0.createNewFile();
        f0.deleteOnExit();
        File f1 = new File(dir, "snapshot_121.xml");
        f1.createNewFile();
        f1.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        // API doesn't state 0 snapshots is illegal return value
        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(0);

        try {
            TestUtil.invokePrivate(cs, "garbageCollectionOldSnapshots");
        } catch (NullPointerException e) {
            fail("Exception not expected.");
        }

        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertTrue("file not deleted", f0.exists());
        assertFalse("file deleted", f1.exists());

        f0.delete();
        d1.delete();
    }

    @Test
    public void testGarbageCollectionOldSnapshotsZeroNoZero() throws Throwable {
        // test scenario where 0 snapshots are configured to remain, but snapshot_0.xml is not present
        final String dir = "gcosDir00";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f0 = new File(dir, "snapshot_1.xml");
        f0.createNewFile();
        f0.deleteOnExit();
        File f1 = new File(dir, "snapshot_121.xml");
        f1.createNewFile();
        f1.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(0);

        TestUtil.invokePrivate(cs, "garbageCollectionOldSnapshots");

        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertFalse("file deleted", f0.exists());
        assertFalse("file deleted", f1.exists());

        d1.delete();
    }

    @Test
    public void testGarbageCollectionOldSnapshotsZeroOnly() throws Throwable {
        // test that 0 is left, even if not newest
        final String dir = "gcosDir01";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f0 = new File(dir, "snapshot_0.xml"); // special snapshot file
        f0.createNewFile();
        f0.deleteOnExit();
        File f1 = new File(dir, "snapshot_121.xml");
        f1.createNewFile();
        f1.deleteOnExit();
        File f2 = new File(dir, "snapshot_122.xml");
        f2.createNewFile();
        f2.deleteOnExit();
        File f3 = new File(dir, "snapshot_123.xml");
        f3.createNewFile();
        f3.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(1);

        TestUtil.invokePrivate(cs, "garbageCollectionOldSnapshots");

        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertTrue("file not deleted", f0.exists());
        assertFalse("file deleted", f1.exists());
        assertFalse("file deleted", f2.exists());
        assertFalse("file deleted", f3.exists());

        f3.delete();
        d1.delete();
    }

    @Test
    public void testGarbageCollectionOldSnapshotsNoZero1() throws Throwable {
        // no zero => newest is left

        final String dir = "gcosDir";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_121.xml");
        f1.createNewFile();
        f1.deleteOnExit();
        File f2 = new File(dir, "snapshot_122.xml");
        f2.createNewFile();
        f2.deleteOnExit();
        File f3 = new File(dir, "snapshot_123.xml");
        f3.createNewFile();
        f3.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(1);

        TestUtil.invokePrivate(cs, "garbageCollectionOldSnapshots");

        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertFalse("file deleted", f1.exists());
        assertFalse("file deleted", f2.exists());
        assertTrue("file not deleted", f3.exists());

        f3.delete();
        d1.delete();
    }

    @Test
    public void testGarbageCollectionOldSnapshots2() throws Throwable {
        // zero and more than one to live => 0 and newest are left
        final String dir = "gcosDir";

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f0 = new File(dir, "snapshot_0.xml"); // special snapshot file
        f0.createNewFile();
        f0.deleteOnExit();
        File f1 = new File(dir, "snapshot_121.xml");
        f1.createNewFile();
        f1.deleteOnExit();
        File f2 = new File(dir, "snapshot_122.xml");
        f2.createNewFile();
        f2.deleteOnExit();
        File f3 = new File(dir, "snapshot_123.xml");
        f3.createNewFile();
        f3.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(2);

        TestUtil.invokePrivate(cs, "garbageCollectionOldSnapshots");

        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertTrue("file not deleted", f0.exists());
        assertFalse("file deleted", f1.exists());
        assertFalse("file deleted", f2.exists());
        assertTrue("file not deleted", f3.exists());

        f0.delete();
        f3.delete();
        d1.delete();
    }

    @Test
    public void testSaveSnapshotNulls() throws Throwable {
        // test new snapshot creation - no old ones

        final String dir = "dirSSN";
        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return null;
            }

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String encCfg = "encrypted";
        char[] encrypted = encCfg.toCharArray();
        when(cryptoServiceMock.encryptAes((char[]) Matchers.anyObject())).thenReturn(encrypted);

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(1);

        XmlComponentConfigurations snapshot = prepareSnapshot();
        List<ComponentConfiguration> configs = snapshot.getConfigurations();

        Long sid = (Long) TestUtil.invokePrivate(cs, "saveSnapshot", configs);

        verify(cryptoServiceMock, times(1)).encryptAes((char[]) Matchers.anyObject());
        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertNotNull(sid);

        File f1 = new File(d1, "snapshot_" + sid + ".xml");
        assertTrue("snapshot file created", f1.exists());

        FileReader fr = new FileReader(f1);
        char[] chars = new char[encCfg.length()];
        fr.read(chars);
        fr.close();

        assertArrayEquals("snapshot file content matches", encCfg.toCharArray(), chars);

        f1.delete();
        d1.delete();
    }

    @Test
    public void testSaveSnapshotNewerLastPid() throws Throwable {
        // new snapshot, too recent old PID => predictable SID

        final String dir = "dirSSNLP";
        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(123L);
        long lastSid = System.currentTimeMillis() + 1000;
        snapshotList.add(lastSid);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return snapshotList;
            }

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String encCfg = "encrypted";
        char[] encrypted = encCfg.toCharArray();
        when(cryptoServiceMock.encryptAes((char[]) Matchers.anyObject())).thenReturn(encrypted);

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(1);

        XmlComponentConfigurations snapshot = prepareSnapshot();
        List<ComponentConfiguration> configs = snapshot.getConfigurations();

        BundleContext bundleContext = mock(BundleContext.class);
        TestUtil.setFieldValue(cs, "bundleContext", bundleContext);

        Long sid = (Long) TestUtil.invokePrivate(cs, "saveSnapshot", configs);

        verify(cryptoServiceMock, times(1)).encryptAes((char[]) Matchers.anyObject());
        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertNotNull(sid);
        assertEquals("sid as expected", lastSid + 1, sid.longValue());

        File f1 = new File(d1, "snapshot_" + sid + ".xml");
        assertTrue("snapshot file created", f1.exists());

        FileReader fr = new FileReader(f1);
        char[] chars = new char[encCfg.length()];
        fr.read(chars);
        fr.close();

        assertArrayEquals("snapshot file content matches", encCfg.toCharArray(), chars);

        f1.delete();
        d1.delete();
    }

    @Test
    public void testSaveSnapshot() throws Throwable {
        // new snapshot, old last PID => take SID from current time

        final String dir = "dirSS";
        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        final Set<Long> snapshotList = new TreeSet<>();
        snapshotList.add(123L);
        long lastSid = 1234;
        snapshotList.add(lastSid);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return snapshotList;
            }

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String encCfg = "encrypted";
        char[] encrypted = encCfg.toCharArray();
        when(cryptoServiceMock.encryptAes((char[]) Matchers.anyObject())).thenReturn(encrypted);

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(1);

        XmlComponentConfigurations snapshot = prepareSnapshot();
        List<ComponentConfiguration> configs = snapshot.getConfigurations();

        Long sid = (Long) TestUtil.invokePrivate(cs, "saveSnapshot", configs);

        verify(cryptoServiceMock, times(1)).encryptAes((char[]) Matchers.anyObject());
        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        assertNotNull(sid);
        assertTrue("Expected higher sid", sid.longValue() > lastSid + 1);
        assertTrue("Expected sid <= current time", sid.longValue() <= System.currentTimeMillis());

        File f1 = new File(d1, "snapshot_" + sid + ".xml");
        assertTrue("Expected snapshot file to be created", f1.exists());

        FileReader fr = new FileReader(f1);
        char[] chars = new char[encCfg.length()];
        fr.read(chars);
        fr.close();

        assertArrayEquals("Expected snapshot file content to match", encCfg.toCharArray(), chars);

        f1.delete();
        d1.delete();
    }

    @Test
    public void testLineBreakHandling() throws KuraException, IOException {
        final CryptoService csMock = mock(CryptoService.class);

        when(csMock.encryptAes(Mockito.any(char[].class))).thenAnswer(invocation -> {
            return invocation.getArgumentAt(0, char[].class);
        });

        when(csMock.decryptAes(Mockito.any(char[].class))).thenAnswer(invocation -> {
            return invocation.getArgumentAt(0, char[].class);
        });

        final File snapshotsDir = new File("/tmp/snapshot_test_dir_" + System.currentTimeMillis());
        snapshotsDir.mkdir();
        snapshotsDir.deleteOnExit();

        final SystemService ssMock = mock(SystemService.class);
        when(ssMock.getKuraSnapshotsDirectory()).thenReturn(snapshotsDir.getAbsolutePath().toString());

        final Map<String, Object> expectedConfig = Collections.singletonMap("prop", "contains\nline\nbreaks\n");

        final String snapshot = prepareSnapshotXml(prepareSnapshot(expectedConfig));

        final File snapshotFile = new File(snapshotsDir, "snapshot_0.xml");
        snapshotFile.deleteOnExit();

        try (final FileWriter fw = new FileWriter(snapshotFile)) {
            fw.write(snapshot);
            fw.flush();
        }

        ConfigurationServiceImpl configurationService = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return Collections.singleton(0L);
            }

            @Override
            String getSnapshotsDirectory() {
                return snapshotsDir.getAbsolutePath().toString();
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        configurationService.setCryptoService(csMock);

        final List<ComponentConfiguration> loadedSnapshot = configurationService.getSnapshot(0);

        assertEquals(expectedConfig.get("prop"), loadedSnapshot.get(0).getConfigurationProperties().get("prop"));

    }

    @Test
    public void testUpdateWithDefaultConfigurationPidsNull() throws Throwable {
        // test null values
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = null;
        Tocd ocd = null;

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        String caPid = pid;
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(null);

        TestUtil.invokePrivate(cs, "updateWithDefaultConfiguration", pid, ocd);

        verify(configAdminMock, times(1)).getConfiguration(caPid, "?");
    }

    @Test
    public void testUpdateWithDefaultConfigurationParamPidOverride() throws Throwable {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "123";
        Tocd ocd = null;

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        String caPid = pid;
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(null);

        TestUtil.invokePrivate(cs, "updateWithDefaultConfiguration", pid, ocd);

        verify(configAdminMock, times(1)).getConfiguration(caPid, "?");
    }

    @Test
    public void testUpdateWithDefaultConfigurationsServicePidNull() throws Throwable {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "123";
        Tocd ocd = null;

        Map<String, String> pids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        pids.put(pid, null);

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        String caPid = pid;
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(null);

        TestUtil.invokePrivate(cs, "updateWithDefaultConfiguration", pid, ocd);

        verify(configAdminMock, times(1)).getConfiguration(caPid, "?");
    }

    @Test
    public void testUpdateWithDefaultConfigurationsServicePid() throws Throwable {
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "123";
        Tocd ocd = null;

        String sPid = "1234";
        Map<String, String> pids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        pids.put(pid, sPid);

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        String caPid = sPid;
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(null);

        TestUtil.invokePrivate(cs, "updateWithDefaultConfiguration", pid, ocd);

        verify(configAdminMock, times(1)).getConfiguration(caPid, "?");
    }

    @Test
    public void testUpdateWithDefaultConfigurationServicePidExists() throws Throwable {
        final String spid = "1234";

        String pid = "123";
        Tocd ocd = null;

        final boolean[] calls = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            boolean mergeWithDefaults(OCD ocd, Map<String, Object> properties) throws KuraException {
                assertEquals("size", 2, properties.size());
                assertTrue("new property", properties.containsKey(ConfigurationService.KURA_SERVICE_PID));
                assertEquals("property value", spid, properties.get(ConfigurationService.KURA_SERVICE_PID));

                calls[0] = true;
                return true;
            }
        };

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        Configuration cfgMock = mock(Configuration.class);
        String caPid = pid;
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(cfgMock);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(ConfigurationService.KURA_SERVICE_PID, spid);
        props.put("test", "test");
        when(cfgMock.getProperties()).thenReturn(props);

        TestUtil.invokePrivate(cs, "updateWithDefaultConfiguration", pid, ocd);

        assertTrue("method called", calls[0]);

        verify(cfgMock, times(1)).update((Dictionary<String, ?>) anyObject());
    }

    @Test
    public void testUpdateWithDefaultConfiguration() throws Throwable {
        final String pid = "123";
        Tocd ocd = null;

        final boolean[] calls = { false };

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            boolean mergeWithDefaults(OCD ocd, Map<String, Object> properties) throws KuraException {
                assertEquals("size", 1, properties.size());
                assertTrue("new property", properties.containsKey(ConfigurationService.KURA_SERVICE_PID));
                assertEquals("property value", pid, properties.get(ConfigurationService.KURA_SERVICE_PID));

                calls[0] = true;
                return true;
            }
        };

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        Configuration cfgMock = mock(Configuration.class);
        String caPid = pid;
        when(configAdminMock.getConfiguration(caPid, "?")).thenReturn(cfgMock);

        when(cfgMock.getProperties()).thenReturn(null);

        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Dictionary<String, Object> dict = (Dictionary<String, Object>) invocation.getArguments()[0];

                assertNotNull(dict);

                assertEquals("one element in properties list - pid", 1, dict.size());

                assertEquals("expected configuration update PID", pid, dict.elements().nextElement());

                return null;
            }
        }).when(cfgMock).update((Dictionary<String, ?>) anyObject());

        TestUtil.invokePrivate(cs, "updateWithDefaultConfiguration", pid, ocd);

        assertTrue("method called", calls[0]);

        verify(cfgMock, times(1)).update((Dictionary<String, ?>) anyObject());
    }

    @Test
    public void testRegisterComponentConfigurationAllNulls() {
        // only null inputs
        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = null;
        String servicePid = null;
        String factoryPid = null;

        cs.registerComponentConfiguration(pid, servicePid, factoryPid);

        // no checks really possible...
    }

    @Test
    public void testRegisterComponentConfigurationPreActivated() throws NoSuchFieldException {
        // pid is already activated, so it's not added to service pids

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";
        String servicePid = "spid";
        String factoryPid = null;

        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        allPids.add(pid);

        Map<String, String> sPids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        assertEquals("spid size OK", 0, sPids.size());

        cs.registerComponentConfiguration(pid, servicePid, factoryPid);

        assertEquals("size still OK", 0, sPids.size());
    }

    @Test
    public void testRegisterComponentConfigurationActivateNoFactory() throws NoSuchFieldException {
        // not activated, but no factory pid available => add to service and activated pids

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";
        String servicePid = "spid";
        String factoryPid = null;

        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        assertEquals("active pids size OK", 0, allPids.size());

        Map<String, String> sPids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        assertEquals("spid size OK", 0, sPids.size());

        cs.registerComponentConfiguration(pid, servicePid, factoryPid);

        assertEquals("size still OK", 1, sPids.size());
        assertEquals("spid in there", servicePid, sPids.get(pid));

        assertEquals("active size increased", 1, allPids.size());
        assertTrue("spid active", allPids.contains(pid));
    }

    @Test
    public void testRegisterComponentConfigurationWithFactoryPid() throws NoSuchFieldException {
        // add also factory PID, but no OCD mapped

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";
        String servicePid = "spid";
        String factoryPid = "fpid";

        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        assertEquals("active pids size OK", 0, allPids.size());

        Map<String, String> sPids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        assertEquals("spid size OK", 0, sPids.size());

        Map<String, String> fPids = (Map<String, String>) TestUtil.getFieldValue(cs, "factoryPidByPid");
        assertEquals("fpid size OK", 0, fPids.size());

        cs.registerComponentConfiguration(pid, servicePid, factoryPid);

        assertEquals("size still OK", 1, sPids.size());
        assertEquals("spid in there", servicePid, sPids.get(pid));

        assertEquals("active size increased", 1, allPids.size());
        assertTrue("spid active", allPids.contains(pid));

        assertEquals("factory size increased", 1, fPids.size());
        assertEquals("fpid in there", factoryPid, fPids.get(pid));
    }

    @Test
    public void testRegisterComponentConfigurationConfigException() throws IOException, NoSuchFieldException {
        // test exception in cfgadmin

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";
        String servicePid = "spid";
        String factoryPid = "fpid";

        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        assertEquals("active pids size OK", 0, allPids.size());

        Map<String, String> sPids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        assertEquals("spid size OK", 0, sPids.size());

        Map<String, String> fPids = (Map<String, String>) TestUtil.getFieldValue(cs, "factoryPidByPid");
        assertEquals("fpid size OK", 0, fPids.size());

        Map<String, Tocd> ocds = (Map<String, Tocd>) TestUtil.getFieldValue(cs, "ocds");
        Tocd ocd = new Tocd();
        ocds.put(factoryPid, ocd);

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        when(configAdminMock.getConfiguration(servicePid, "?")).thenThrow(new IOException("test"));

        cs.registerComponentConfiguration(pid, servicePid, factoryPid);

        verify(configAdminMock, times(1)).getConfiguration(servicePid, "?");

        assertEquals("size still OK", 1, sPids.size());
        assertEquals("spid in there", servicePid, sPids.get(pid));

        assertEquals("active size increased", 1, allPids.size());
        assertTrue("spid active", allPids.contains(pid));

        assertEquals("factory size increased", 1, fPids.size());
        assertEquals("fpid in there", factoryPid, fPids.get(pid));
    }

    @Test
    public void testRegisterComponentConfiguration() throws IOException, NoSuchFieldException {
        // check that updateWithDefaultConfiguration is called successfully

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        String pid = "pid";
        String servicePid = "spid";
        String factoryPid = "fpid";

        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        assertEquals("active pids size OK", 0, allPids.size());

        Map<String, String> sPids = (Map<String, String>) TestUtil.getFieldValue(cs, "servicePidByPid");
        assertEquals("spid size OK", 0, sPids.size());

        Map<String, String> fPids = (Map<String, String>) TestUtil.getFieldValue(cs, "factoryPidByPid");
        assertEquals("fpid size OK", 0, fPids.size());

        Map<String, Tocd> ocds = (Map<String, Tocd>) TestUtil.getFieldValue(cs, "ocds");
        Tocd ocd = new Tocd();
        ocds.put(factoryPid, ocd);

        ConfigurationAdmin configAdminMock = mock(ConfigurationAdmin.class);
        cs.setConfigurationAdmin(configAdminMock);

        when(configAdminMock.getConfiguration(servicePid, "?")).thenReturn(null);

        cs.registerComponentConfiguration(pid, servicePid, factoryPid);

        verify(configAdminMock, times(1)).getConfiguration(servicePid, "?");

        assertEquals("size still OK", 1, sPids.size());
        assertEquals("spid in there", servicePid, sPids.get(pid));

        assertEquals("active size increased", 1, allPids.size());
        assertTrue("spid active", allPids.contains(pid));

        assertEquals("factory size increased", 1, fPids.size());
        assertEquals("fpid in there", factoryPid, fPids.get(pid));
    }

    @Test
    public void testRollbackNoPids() throws KuraException {
        // test rollback with no available shapshots - failure

        final boolean[] calls = { false };
        final Set<Long> pids = new HashSet<>();

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return pids;
            }

            @Override
            public synchronized void rollback(long id) throws KuraException {
                calls[0] = true;
            }
        };

        try {
            cs.rollback();
            fail("Exception expected with < 2 pids.");
        } catch (KuraException e) {
            assertEquals("code matches", KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, e.getCode());
        }
    }

    @Test
    public void testRollbackOnePid() throws KuraException {
        // test rollback with one available shapshot - failure

        final boolean[] calls = { false };
        final Set<Long> pids = new HashSet<>();
        pids.add(123L);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return pids;
            }

            @Override
            public synchronized void rollback(long id) throws KuraException {
                calls[0] = true;
            }
        };

        try {
            cs.rollback();
            fail("Exception expected with < 2 pids.");
        } catch (KuraException e) {
            assertEquals("code matches", KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, e.getCode());
        }
    }

    @Test
    public void testRollbackTwoPids() throws KuraException {
        // test rollback with 2 pids - OK
        final boolean[] calls = { false };
        final long pid = 123;
        final Set<Long> pids = new HashSet<>();
        pids.add(pid);
        pids.add(124L);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return pids;
            }

            @Override
            public synchronized void rollback(long id) throws KuraException {
                calls[0] = true;

                assertEquals("correct pid", pid, id);
            }
        };

        cs.rollback();

        assertTrue("delegated", calls[0]);
    }

    @Test
    public void testRollback() throws KuraException {
        // test rollback with more than 2 pids

        final boolean[] calls = { false };
        final long pid = 123;
        final Set<Long> pids = new HashSet<>();
        pids.add(121L);
        pids.add(122L);
        pids.add(pid);
        pids.add(124L);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            public Set<Long> getSnapshots() throws KuraException {
                return pids;
            }

            @Override
            public synchronized void rollback(long id) throws KuraException {
                calls[0] = true;

                assertEquals("correct pid", pid, id);
            }
        };

        cs.rollback();

        assertTrue("delegated", calls[0]);
    }

    public void testRollbackIdPartialSvcRef() throws Exception {
        long id = 123;
        final String dir = "dirRIPSR";

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_" + id + ".xml");
        f1.createNewFile();
        f1.deleteOnExit();

        FileWriter fw = new FileWriter(f1);
        fw.append("test");
        fw.close();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String decrypted = prepareSnapshotXML();
        when(cryptoServiceMock.decryptAes("test".toCharArray())).thenReturn(decrypted.toCharArray());

        when(cryptoServiceMock.encryptAes((char[]) anyObject())).thenReturn("encrypted".toCharArray());

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(5);

        String pid = "pid";
        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        allPids.add(pid);

        ComponentContext componentCtxMock = mock(ComponentContext.class);
        TestUtil.setFieldValue(cs, "ctx", componentCtxMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentCtxMock.getBundleContext()).thenReturn(bundleCtxMock);

        when(bundleCtxMock.getServiceReferences((String) null, null))
                .thenThrow(new InvalidSyntaxException("test", null));

        cs.rollback(id);

        verify(cryptoServiceMock, times(1)).decryptAes("test".toCharArray());
        verify(cryptoServiceMock, times(1)).encryptAes((char[]) anyObject());
        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        File[] files = d1.listFiles();

        assertEquals(2, files.length);

        for (File f : files) {
            f.deleteOnExit();
        }
        String expect = "test";

        FileReader fr = new FileReader(files[0]);
        char[] chars = new char[expect.length()];
        fr.read(chars);
        fr.close();

        assertEquals(expect, new String(chars));

        expect = "encrypted";

        fr = new FileReader(files[1]);
        chars = new char[expect.length()];
        fr.read(chars);
        fr.close();

        assertEquals(expect, new String(chars));
    }

    @Test
    public void testRollbackIdPartial() throws Exception {
        long id = 123;
        final String dir = "dirRIP";

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();

                return xmlMarshaller.unmarshal(xmlString, clazz);
            }

            @Override
            protected String marshal(Object object) {
                XmlMarshallUnmarshallImpl xmlMarshaller = new XmlMarshallUnmarshallImpl();
                try {
                    return xmlMarshaller.marshal(object);
                } catch (KuraException e) {

                }
                return null;
            }
        };

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_" + id + ".xml");
        f1.createNewFile();
        f1.deleteOnExit();

        FileWriter fw = new FileWriter(f1);
        fw.append("test");
        fw.close();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String decrypted = prepareSnapshotXML();
        when(cryptoServiceMock.decryptAes("test".toCharArray())).thenReturn(decrypted.toCharArray());

        when(cryptoServiceMock.encryptAes((char[]) anyObject())).thenReturn("encrypted".toCharArray());

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(5);

        String pid = "pid";
        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        allPids.add(pid);

        Map<String, String> factoryPidByPid = new HashMap<>(); // will cause exception in deleteFactoryConfiguration
        factoryPidByPid.put("key", null);
        TestUtil.setFieldValue(cs, "factoryPidByPid", factoryPidByPid);

        Map<String, Tocd> ocds = new HashMap<>(); // for getRegisteredOCD in rollbackConfigurationInternal
        Tocd ocd = new Tocd();
        ocds.put(pid, ocd);
        TestUtil.setFieldValue(cs, "ocds", ocds);

        ComponentContext componentCtxMock = mock(ComponentContext.class);
        TestUtil.setFieldValue(cs, "ctx", componentCtxMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentCtxMock.getBundleContext()).thenReturn(bundleCtxMock);

        ServiceReference svcRefMock = mock(ServiceReference.class);
        ServiceReference[] svcReferences = { svcRefMock };
        when(bundleCtxMock.getServiceReferences((String) null, null)).thenReturn(svcReferences);

        String ppid = pid;
        when(svcRefMock.getProperty(Constants.SERVICE_PID)).thenReturn(ppid);

        Bundle bundleMock = mock(Bundle.class);
        when(svcRefMock.getBundle()).thenReturn(bundleMock);

        when(bundleMock.getResource(Matchers.anyString())).thenThrow(new NullPointerException("test"));

        try {
            cs.rollback(id);
            fail("Rigged for exception.");
        } catch (KuraPartialSuccessException e) {
            // OK
        }

        verify(cryptoServiceMock, times(1)).decryptAes("test".toCharArray());

        File[] files = d1.listFiles();

        assertEquals(1, files.length);

        for (File f : files) {
            f.deleteOnExit();
        }
        String expect = "test";

        FileReader fr = new FileReader(files[0]);
        char[] chars = new char[expect.length()];
        fr.read(chars);
        fr.close();

        assertEquals(expect, new String(chars));
    }

    public void testRollbackId() throws Exception {
        long id = 123;
        final String dir = "dirRI";

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl() {

            @Override
            String getSnapshotsDirectory() {
                return dir;
            }
        };

        File d1 = new File(dir);
        d1.mkdirs();
        d1.deleteOnExit();

        File f1 = new File(dir, "snapshot_" + id + ".xml");
        f1.createNewFile();
        f1.deleteOnExit();

        FileWriter fw = new FileWriter(f1);
        fw.append("test");
        fw.close();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        String decrypted = prepareSnapshotXML();
        when(cryptoServiceMock.decryptAes("test".toCharArray())).thenReturn(decrypted.toCharArray());

        when(cryptoServiceMock.encryptAes((char[]) anyObject())).thenReturn("encrypted".toCharArray());

        SystemService systemServiceMock = mock(SystemService.class);
        cs.setSystemService(systemServiceMock);

        when(systemServiceMock.getKuraSnapshotsCount()).thenReturn(5);

        String pid = "pid";
        Set<String> allPids = (Set<String>) TestUtil.getFieldValue(cs, "allActivatedPids");
        allPids.add(pid);

        ComponentContext componentCtxMock = mock(ComponentContext.class);
        TestUtil.setFieldValue(cs, "ctx", componentCtxMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentCtxMock.getBundleContext()).thenReturn(bundleCtxMock);

        ServiceReference svcRefMock = mock(ServiceReference.class);
        ServiceReference[] svcReferences = { svcRefMock };
        when(bundleCtxMock.getServiceReferences((String) null, null)).thenReturn(svcReferences);

        String ppid = pid;
        when(svcRefMock.getProperty(Constants.SERVICE_PID)).thenReturn(ppid);

        Bundle bundleMock = mock(Bundle.class);
        when(svcRefMock.getBundle()).thenReturn(bundleMock);

        when(bundleMock.getResource(Matchers.anyString())).thenReturn(null);

        cs.rollback(id);

        verify(cryptoServiceMock, times(1)).decryptAes("test".toCharArray());
        verify(cryptoServiceMock, times(1)).encryptAes((char[]) anyObject());
        verify(systemServiceMock, times(1)).getKuraSnapshotsCount();

        File[] files = d1.listFiles();

        assertEquals(2, files.length);

        for (File f : files) {
            f.deleteOnExit();
        }
        String expect = "test";

        FileReader fr = new FileReader(files[0]);
        char[] chars = new char[expect.length()];
        fr.read(chars);
        fr.close();

        assertEquals(expect, new String(chars));

        expect = "encrypted";

        fr = new FileReader(files[1]);
        chars = new char[expect.length()];
        fr.read(chars);
        fr.close();

        assertEquals(expect, new String(chars));
    }

    private ComponentDescriptionDTO createMockComponent(final String pid, final String... implementedServices) {
        final ComponentDescriptionDTO result = mock(ComponentDescriptionDTO.class);
        result.name = pid;
        result.serviceInterfaces = implementedServices;

        return result;
    }

    private OCDService createMockConfigurationServiceForOCDTests(List<String> registeredFactories,
            List<Tocd> registeredOcds, List<ComponentDescriptionDTO> registeredComponents)
            throws NoSuchFieldException, KuraException {

        assertEquals(registeredFactories.size(), registeredOcds.size());
        ServiceComponentRuntime scrService = mock(ServiceComponentRuntime.class);
        when(scrService.getComponentDescriptionDTOs()).thenReturn(registeredComponents);

        final ConfigurationServiceImpl result = new ConfigurationServiceImpl();
        result.setScrService(scrService);
        for (int i = 0; i < registeredOcds.size(); i++) {
            result.registerComponentOCD(registeredFactories.get(i), registeredOcds.get(i), true, null);
        }

        return result;
    }

    private boolean isOCDFor(ComponentConfiguration config, String factoryPid, Tocd ocd) {
        return config.getPid().equals(factoryPid) && config.getDefinition() == ocd;
    }

    @Test
    public void testShouldReturnEmptyFactoryOCDList() throws NoSuchFieldException, KuraException {
        final OCDService ocdService = createMockConfigurationServiceForOCDTests(Arrays.asList(), Arrays.asList(),
                Arrays.asList());
        final List<ComponentConfiguration> configs = ocdService.getFactoryComponentOCDs();
        assertTrue(configs.isEmpty());
    }

    @Test
    public void testShouldGetFactoryOCDList() throws NoSuchFieldException, KuraException {
        final Tocd ocd1 = mock(Tocd.class);
        final Tocd ocd2 = mock(Tocd.class);
        final Tocd ocd3 = mock(Tocd.class);
        final OCDService ocdService = createMockConfigurationServiceForOCDTests(Arrays.asList("foo", "bar", "baz"),
                Arrays.asList(ocd1, ocd2, ocd3), Arrays.asList());
        final List<ComponentConfiguration> configs = ocdService.getFactoryComponentOCDs();
        assertEquals(3, configs.size());
        assertTrue(configs.stream().filter(config -> isOCDFor(config, "foo", ocd1)).findAny().isPresent());
        assertTrue(configs.stream().filter(config -> isOCDFor(config, "bar", ocd2)).findAny().isPresent());
        assertTrue(configs.stream().filter(config -> isOCDFor(config, "baz", ocd3)).findAny().isPresent());
    }

    @Test
    public void testShouldReturnNullFactoryOCD() throws NoSuchFieldException, KuraException {
        final OCDService ocdService = createMockConfigurationServiceForOCDTests(Arrays.asList(), Arrays.asList(),
                Arrays.asList());
        assertNull(ocdService.getFactoryComponentOCD("bar"));
        assertNull(ocdService.getFactoryComponentOCD(null));
    }

    @Test
    public void testShouldGetSingleFactoryOCD() throws NoSuchFieldException, KuraException {
        final Tocd ocd1 = mock(Tocd.class);
        final Tocd ocd2 = mock(Tocd.class);
        final Tocd ocd3 = mock(Tocd.class);
        final OCDService ocdService = createMockConfigurationServiceForOCDTests(Arrays.asList("foo", "bar", "baz"),
                Arrays.asList(ocd1, ocd2, ocd3), Arrays.asList());
        assertTrue(isOCDFor(ocdService.getFactoryComponentOCD("foo"), "foo", ocd1));
        assertTrue(isOCDFor(ocdService.getFactoryComponentOCD("bar"), "bar", ocd2));
        assertTrue(isOCDFor(ocdService.getFactoryComponentOCD("baz"), "baz", ocd3));
        assertNull(ocdService.getFactoryComponentOCD("nonExisting"));
        assertNull(ocdService.getFactoryComponentOCD(null));
    }

    @Test
    public void testShouldReturnEmptyFactoryOCDListForServiceProvider() throws NoSuchFieldException, KuraException {
        final OCDService ocdService = createMockConfigurationServiceForOCDTests(Arrays.asList(), Arrays.asList(),
                Arrays.asList());
        assertTrue(ocdService.getServiceProviderOCDs(new Class<?>[0]).isEmpty());
        assertTrue(ocdService.getServiceProviderOCDs(String.class).isEmpty());
    }

    @Test
    public void testShouldReturnFactoryOCDListForServiceProvider() throws NoSuchFieldException, KuraException {
        final Tocd fooOcd = mock(Tocd.class);
        final Tocd barOcd = mock(Tocd.class);
        final Tocd bazOcd = mock(Tocd.class);
        final Tocd otherOcd = mock(Tocd.class);

        final ComponentDescriptionDTO comp1 = createMockComponent("foo", "java.lang.String", "java.lang.Integer");
        final ComponentDescriptionDTO comp2 = createMockComponent("bar", "java.lang.Double", "java.lang.Long");
        final ComponentDescriptionDTO comp3 = createMockComponent("baz", "java.lang.Double", "java.lang.Integer");
        final ComponentDescriptionDTO comp4 = createMockComponent("other");

        final OCDService ocdService = createMockConfigurationServiceForOCDTests(
                Arrays.asList("foo", "bar", "baz", "other"), Arrays.asList(fooOcd, barOcd, bazOcd, otherOcd),
                Arrays.asList(comp1, comp2, comp3, comp4));

        assertTrue(ocdService.getServiceProviderOCDs(new Class<?>[0]).isEmpty());

        final List<ComponentConfiguration> implementingString = ocdService.getServiceProviderOCDs(String.class);
        assertEquals(1, implementingString.size());
        assertTrue(implementingString.stream().filter(config -> isOCDFor(config, "foo", fooOcd)).findAny().isPresent());

        final List<ComponentConfiguration> implementingStringOrInteger = ocdService.getServiceProviderOCDs(String.class,
                Integer.class);
        assertEquals(2, implementingStringOrInteger.size());
        assertTrue(implementingStringOrInteger.stream().filter(config -> isOCDFor(config, "foo", fooOcd)).findAny()
                .isPresent());
        assertTrue(implementingStringOrInteger.stream().filter(config -> isOCDFor(config, "baz", bazOcd)).findAny()
                .isPresent());

        final List<ComponentConfiguration> implementingLongOrBoolean = ocdService.getServiceProviderOCDs(Long.class,
                Boolean.class);
        assertEquals(1, implementingLongOrBoolean.size());
        assertTrue(implementingLongOrBoolean.stream().filter(config -> isOCDFor(config, "bar", barOcd)).findAny()
                .isPresent());

        final List<ComponentConfiguration> implementingBoolean = ocdService.getServiceProviderOCDs(Boolean.class);
        assertTrue(implementingBoolean.isEmpty());

        final List<ComponentConfiguration> implementingLong = ocdService.getServiceProviderOCDs(Long.class);
        assertEquals(1, implementingLong.size());
        assertTrue(implementingLong.stream().filter(config -> isOCDFor(config, "bar", barOcd)).findAny().isPresent());

        final List<ComponentConfiguration> implementingDouble = ocdService.getServiceProviderOCDs(Double.class);
        assertEquals(2, implementingDouble.size());
        assertTrue(implementingDouble.stream().filter(config -> isOCDFor(config, "bar", barOcd)).findAny().isPresent());
        assertTrue(implementingDouble.stream().filter(config -> isOCDFor(config, "baz", bazOcd)).findAny().isPresent());
    }
}
