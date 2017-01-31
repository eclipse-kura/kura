/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.configuration.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraPartialSuccessException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.ConfigurationServiceImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.test.ConfigurationServiceTest.CSValidator;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationServiceTest.class);

    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String TEST_COMPONENT_PID = "org.eclipse.kura.core.configuration.test.CfgSvcTestComponent";
    private static final String TEST_COMPONENT_FPID = "org.eclipse.kura.core.configuration.test.TestFactoryComponent";
    private static final String TEST_COMPONENT_PROPERTY_TEST = "field.test";

    private static CountDownLatch dependencyLatch = new CountDownLatch(2);

    private static ConfigurationService configurationService;
    private static SystemService systemService;

    private int kuraSnapshotsCount = 10;
    private String kuraSnapshotsDir = "/tmp/kura/snapshots";

    private static CryptoService csMock;
    private static SystemService ssMock;

    @BeforeClass
    public static void setUpClass() throws Exception {

        BundleContext bundleContext = FrameworkUtil.getBundle(ConfigurationServiceTest.class).getBundleContext();

        csMock = mock(CryptoService.class);
        bundleContext.registerService(CryptoService.class.getName(), csMock, null);

        resetMocks();

        ServiceReference<SystemService> serviceReference = bundleContext.getServiceReference(SystemService.class);
        if (serviceReference != null) {
            serviceReference.getBundle().stop();
        }
        ssMock = mock(SystemService.class);
        bundleContext.registerService(SystemService.class.getName(), ssMock, null);

        when(ssMock.getKuraSnapshotsDirectory()).thenReturn("/tmp/kura/snapshots");
        when(ssMock.getKuraSnapshotsCount()).thenReturn(10);

        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);

            assertTrue("Dependencies OK", ok);
        } catch (final InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
        }
    }

    @Before
    public void setup() throws KuraException {
        if (systemService != null) {
            kuraSnapshotsCount = systemService.getKuraSnapshotsCount();
            kuraSnapshotsDir = systemService.getKuraSnapshotsDirectory();
        }

        resetMocks();

        // remove all other snapshots
        File dir = new File(kuraSnapshotsDir);
        File[] snapshots = dir.listFiles();
        for (File f : snapshots) {
            f.delete();
        }
    }

    interface CSValidator {

        public String getEncrypted();

        public String getDecrypted();

        public boolean validateEncryptArgs(Object[] args);

        public boolean validateDecryptArgs(Object[] args) throws KuraException;
    }

    private static void resetMocks() throws KuraException {
        CSValidator validator = new CSValidator() {

            @Override
            public boolean validateEncryptArgs(Object[] args) {
                return true;
            }

            @Override
            public boolean validateDecryptArgs(Object[] args) throws KuraException {
                String arg0 = new String((char[]) args[0]);
                if (arg0.startsWith("<?xml")) {
                    throw new KuraException(KuraErrorCode.DECODER_ERROR);
                }

                return true;
            }

            @Override
            public String getEncrypted() {
                return "encrypted";
            }

            @Override
            public String getDecrypted() {
                try {
                    return loadConfigsXml();
                } catch (Exception e) {
                    // OK...
                }
                return "";
            }
        };

        resetMocks(validator);
    }

    private static void resetMocks(final CSValidator validator) throws KuraException {
        reset(csMock);

        when(csMock.encryptAes((char[]) Mockito.anyObject())).thenAnswer(new Answer<char[]>() {

            @Override
            public char[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                validator.validateEncryptArgs(args);
                return validator.getEncrypted().toCharArray();
            }

        });
        when(csMock.decryptAes((char[]) Mockito.anyObject())).thenAnswer(new Answer<char[]>() {

            @Override
            public char[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String arg0 = new String((char[]) args[0]);
                validator.validateDecryptArgs(args);
                return validator.getDecrypted().toCharArray();
            }

        });
    }

    protected void bindConfigService(final ConfigurationService configService) {
        if (configurationService == null) {
            configurationService = configService;
            dependencyLatch.countDown();
        }
    }

    protected void unbindConfigService(final ConfigurationService configService) {
        if (configurationService == configService) {
            configurationService = null;
        }
    }

    protected void bindSystemService(final SystemService sysService) {
        if (systemService == null) {
            systemService = sysService;
            dependencyLatch.countDown();
        }
    }

    protected void unbindSystemService(final SystemService sysService) {
        if (systemService == sysService) {
            systemService = null;
        }
    }

    @Test
    public void testServiceBound() {
        assertNotNull("Configuration service should not be null.", configurationService);
        assertNotNull("System service should not be null.", systemService);
    }

    @Test
    public void testGetFactoryComponentPids() throws NoSuchFieldException {
        Set<String> factoryComponentPids = configurationService.getFactoryComponentPids();

        assertTrue("At least 1 PID should be available.", factoryComponentPids.size() >= 1);

        assertTrue("Should contain test factory component", factoryComponentPids.contains(TEST_COMPONENT_FPID));
    }

    @Test
    public void testCreateFactoryConfigurationNulls() {
        // negative test; how null values are handled

        String factoryPid = null;
        String pid = null;
        Map<String, Object> properties = null;
        boolean takeSnapshot = false;

        try {
            configurationService.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

            fail("Exception expected with null pid value.");
        } catch (Exception e) {
            // OK, probably
        }
    }

    @Test
    public void testCreateFactoryExistingPid() throws KuraException, IOException, NoSuchFieldException {
        // negative test; what if existing PID is used

        final String factoryPid = DATA_SERVICE_FACTORY_PID;
        final String pid = "cfepid_pid_" + System.currentTimeMillis();
        Map<String, Object> properties = null;
        final boolean takeSnapshot = false;

        // first registration should succeed
        try {
            configurationService.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);
        } catch (Exception e) {
            fail("Exception not expected with a new pid value.");
        }

        try {
            configurationService.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

            fail("Exception expected with existing pid value.");
        } catch (Exception e) {
            // OK, probably
        }
    }

    @Test
    public void testCreateFactoryConfigurationMergePropertiesAndSnapshot() throws KuraException, IOException {
        // a positive test, take passed properties into account, with snapshot creation

        final String factoryPid = TEST_COMPONENT_FPID;
        final String pid = "cfcmp_pid_" + System.currentTimeMillis();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("key", "value");
        final boolean takeSnapshot = true;

        CSValidator validator = new MultiStepCSValidator(factoryPid, pid);

        resetMocks(validator);

        Set<Long> snapshots = configurationService.getSnapshots();

        configurationService.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

        Set<Long> snapshotsAfter = configurationService.getSnapshots();

        int expectedSize = Math.min(kuraSnapshotsCount, snapshots.size() + 1);
        assertEquals("One more snapshot expected", expectedSize, snapshotsAfter.size());

        // verify that the new snapshot contains our new property
        snapshotsAfter.removeAll(snapshots);

        List<ComponentConfiguration> snapshot = configurationService
                .getSnapshot(snapshotsAfter.iterator().next().longValue());
        boolean found = false;
        for (ComponentConfiguration cc : snapshot) {
            if (pid.compareTo(cc.getPid()) == 0) {
                Map<String, Object> props = cc.getConfigurationProperties();
                assertTrue("Should contain our key", props.containsKey("key"));
                assertEquals("Should contain the right value", "value", props.get("key"));

                found = true;

                break;
            }
        }
        assertTrue("Configuration should be verified", found);
    }

    @Test
    public void testDeleteFactoryConfigurationNulls() throws KuraException {
        // negative test; null pid

        String pid = null;
        boolean takeSnapshot = false;

        try {
            configurationService.deleteFactoryConfiguration(pid, takeSnapshot);

            fail("Null parameter - exception expected.");
        } catch (KuraException e) {
            assertTrue(e.getMessage().contains("INVALID_PARAMETER"));
        }
    }

    @Test
    public void testDeleteFactoryConfigurationNonExistingFactoryPid() throws KuraException {
        // negative test; pid not registered

        String pid = "pid_" + System.currentTimeMillis();
        boolean takeSnapshot = false;

        try {
            configurationService.deleteFactoryConfiguration(pid, takeSnapshot);

            fail("Nonexisting parameter - exception expected.");
        } catch (KuraException e) {
            assertTrue(e.getMessage().contains("INVALID_PARAMETER"));
        }
    }

    @Test
    public void testDeleteFactoryConfigurationWithSnapshot() throws KuraException, IOException, NoSuchFieldException {
        // positive test; pid registered in factory and service pids, configuration delete is expected, with snapshots

//        String factoryPid = "fpid_" + System.currentTimeMillis();
        final String factoryPid = "fpid_" + System.currentTimeMillis();
        final String servicePid = "spid_" + System.currentTimeMillis();
        final boolean takeSnapshot = true;

        CSValidator validator = new MultiStepCSValidator(factoryPid, servicePid);

        resetMocks(validator);

        Set<Long> snapshotsBefore = configurationService.getSnapshots();
        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, takeSnapshot);
        Set<Long> snapshotsAfter = configurationService.getSnapshots();

        int expectedSize = Math.min(kuraSnapshotsCount, snapshotsBefore.size() + 1);
        assertEquals("One more configuration expected", expectedSize, snapshotsAfter.size());
        snapshotsAfter.removeAll(snapshotsBefore);
        long sid = snapshotsAfter.iterator().next().longValue();

        List<ComponentConfiguration> snapshot = configurationService.getSnapshot(sid);
        boolean found = false;
        for (ComponentConfiguration cc : snapshot) {
            if (servicePid.compareTo(cc.getPid()) == 0) {
                found = true;

                break;
            }
        }
        assertTrue("Configuration for PID should be present", found);

        // the main call...
        configurationService.deleteFactoryConfiguration(servicePid, takeSnapshot);

        // verify the result
        Set<Long> snapshotsAfterAfter = configurationService.getSnapshots();

        snapshotsAfterAfter.removeAll(snapshotsAfter);
        sid = snapshotsAfterAfter.iterator().next().longValue();

        snapshot = configurationService.getSnapshot(sid);
        found = false;
        for (ComponentConfiguration cc : snapshot) {
            if (servicePid.compareTo(cc.getPid()) == 0) {
                found = true;

                break;
            }
        }
        assertFalse("Configuration for PID should not be present", found);
    }

    @Test
    public void testGetConfigurableComponentPids() {
        // positive test: get pids, assert they are not modifiable outside
        Set<String> configurableComponentPids = configurationService.getConfigurableComponentPids();

        assertFalse("Should not be empty", configurableComponentPids.isEmpty());

        try {
            configurableComponentPids.add("unsupported");
            fail("Updating PIDs should not be possible.");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

    @Test
    public void testGetConfigurableComponentPidsAdd() throws KuraException {
        // positive test: add a new configuration and find it later

        String factoryPid = "fpid_" + System.currentTimeMillis();
        final String servicePid = "spid_ccpa_" + System.currentTimeMillis();
        final boolean takeSnapshot = true;

        Set<String> configurableComponentPids = configurationService.getConfigurableComponentPids();

        assertFalse("Should not be empty", configurableComponentPids.isEmpty());
        assertFalse("Should not contain our service PID", configurableComponentPids.contains(servicePid));

        int size1 = configurableComponentPids.size();

        // create new configuration
        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, takeSnapshot);

        // check results
        Set<String> configurableComponentPids2 = configurationService.getConfigurableComponentPids();

        int size2 = configurableComponentPids2.size();

        assertEquals("Should have additional PID", size1 + 1, size2);
        assertTrue("Should contain our service PID", configurableComponentPids2.contains(servicePid));
    }

    @Test
    public void testGetComponentConfigurations() throws KuraException {
        // positive test; new pid registered => new configuration

        String factoryPid = "fpid_gcc_" + System.currentTimeMillis();
        final String servicePid = "spid_gcc_" + System.currentTimeMillis();
        final boolean takeSnapshot = false;

        List<ComponentConfiguration> configurations = configurationService.getComponentConfigurations();

        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, takeSnapshot);

        List<ComponentConfiguration> configurationsAfter = configurationService.getComponentConfigurations();

        assertEquals("Should contain one more configuration", configurations.size() + 1, configurationsAfter.size());
        configurationsAfter.removeAll(configurations);

        boolean found = false;
        for (ComponentConfiguration cc : configurationsAfter) {
            if (servicePid.compareTo(cc.getPid()) == 0) {
                found = true;
                break;
            }
        }

        assertTrue("Should have found new configuration", found);
    }

    @Test
    public void testGetComponentConfigurationNull() throws KuraException {
        ComponentConfiguration configuration = configurationService.getComponentConfiguration(null);

        assertNull("Null should produce null", configuration);
    }

    @Test
    public void testGetComponentConfiguration() throws KuraException {
        String factoryPid = "fpid_gcc_" + System.currentTimeMillis();
        final String servicePid = "spid_gcc_" + System.currentTimeMillis();
        final boolean takeSnapshot = false;

        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, takeSnapshot);

        ComponentConfiguration configuration = configurationService.getComponentConfiguration(servicePid);

        assertNotNull("Configuration should be returned", configuration);
        assertEquals("Correct PID should be returned", servicePid, configuration.getPid());
    }

    @Test
    public void testGetDefaultComponentConfigurationNull() throws KuraException {
        // default configuration of null is empty, with no PID set

        ComponentConfiguration configuration = configurationService.getDefaultComponentConfiguration(null);

        assertNotNull("Configuration should not be null", configuration);
        assertNull("PID should be null", configuration.getPid());
        assertTrue("Properties should be empty", configuration.getConfigurationProperties().isEmpty());
        assertNull("Should not be any definition", configuration.getDefinition());
    }

    @Test
    public void testGetDefaultComponentConfigurationNonExisting() throws KuraException {
        // default configuration of a non-existing service is empty

        final String servicePid = "spid_gdccne_" + System.currentTimeMillis();

        ComponentConfiguration configuration = configurationService.getDefaultComponentConfiguration(servicePid);

        assertNotNull("Configuration should not be null", configuration);
        assertEquals("PID should be set", servicePid, configuration.getPid());
        assertTrue("Properties should be empty", configuration.getConfigurationProperties().isEmpty());
    }

    @Test
    public void testGetDefaultComponentConfiguration() throws KuraException {
        // default configuration of a new service is empty

        final String factoryPid = "fpid_gdcc_" + System.currentTimeMillis();
        final String servicePid = "spid_gdcc_" + System.currentTimeMillis();

        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, false);

        ComponentConfiguration configuration = configurationService.getDefaultComponentConfiguration(servicePid);

        assertNotNull("Configuration should not be null", configuration);
        assertEquals("PID should be set", servicePid, configuration.getPid());
        assertTrue("Properties should be empty", configuration.getConfigurationProperties().isEmpty());
        assertNull("Should not be any definition", configuration.getDefinition());
    }

    @Test
    public void testGetDefaultComponentConfigurationExisting() throws KuraException {
        // default configuration of an existing service is ...

        ComponentConfiguration configuration = configurationService
                .getDefaultComponentConfiguration(TEST_COMPONENT_PID);

        assertNotNull("Configuration should not be null", configuration);
        assertEquals("PID should be set", TEST_COMPONENT_PID, configuration.getPid());
        assertNotNull("Property should exist", configuration.getConfigurationProperties());
        assertEquals("1 property should exist", 1, configuration.getConfigurationProperties().size());
        assertEquals("Test property shoult exist", 1, configuration.getConfigurationProperties().get("field.test"));
        assertNotNull("Definition should exist", configuration.getDefinition());
        assertNotNull("ID should exist", configuration.getDefinition().getId());
        assertNotNull("Name should exist", configuration.getDefinition().getName());
        assertNotNull("Description should exist", configuration.getDefinition().getDescription());
        assertNotNull("Icon should exist", configuration.getDefinition().getIcon());
        assertNotNull("AD should exist", configuration.getDefinition().getAD());
        assertFalse("AD is not empty", configuration.getDefinition().getAD().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateConfigurationPidPropertiesNull() throws KuraException {
        String pid = null;
        Map<String, Object> properties = null;

        configurationService.updateConfiguration(pid, properties);
    }

    /*
     * FIXME? what is strange is that method's JavaDOC states: "If the component to be updated is not yet registered
     * with the ConfigurationService, it is first registered and then it is updated with the specified properties."
     * Well, that obviously doesn't happen...
     */
    @Test
    public void testUpdateConfigurationPidPropertiesNullProps() throws KuraException {
        String pid = "ucppnp_pid_" + System.currentTimeMillis();
        Map<String, Object> properties = null;

        configurationService.updateConfiguration(pid, properties);

        ComponentConfiguration configuration = configurationService.getComponentConfiguration(pid);

        assertNull("Configuration should be null", configuration);
    }

    @Test
    public void testUpdateConfigurationPidPropertiesEmptyProps() throws KuraException {
        // try it with a registered component and an existing PID with empty properties

        String pid = TEST_COMPONENT_PID;

        ComponentConfiguration config = configurationService.getComponentConfiguration(pid);
        assertNotNull(config);

        Map<String, Object> configurationProperties = config.getConfigurationProperties();
        Object val = configurationProperties.get(TEST_COMPONENT_PROPERTY_TEST);

        Map<String, Object> properties = new HashMap<String, Object>();

        configurationService.updateConfiguration(pid, properties);

        config = configurationService.getComponentConfiguration(pid);
        assertEquals("Should be no change", val, configurationProperties.get(TEST_COMPONENT_PROPERTY_TEST));
    }

    @Test
    public void testUpdateConfigurationPidPropertiesValid() throws KuraException {
        // try it with a registered component and an existing PID with invalid properties
        String pid = TEST_COMPONENT_PID;

        Set<Long> snapshots = configurationService.getSnapshots();
        int size1 = snapshots.size();

        Map<String, Object> properties = new HashMap<String, Object>();
        String prop = "some unknown property";
        properties.put(prop, 123);
        properties.put(TEST_COMPONENT_PROPERTY_TEST, 10);

        configurationService.updateConfiguration(pid, properties);

        ComponentConfiguration config = configurationService.getComponentConfiguration(pid);
        assertEquals(10, config.getConfigurationProperties().get(TEST_COMPONENT_PROPERTY_TEST));
        assertEquals(123, config.getConfigurationProperties().get(prop));

        snapshots = configurationService.getSnapshots();
        int size2 = snapshots.size();
        assertEquals("Should contain one more snapshot", size1 + 1, size2);
    }

    static String loadConfigsXml() throws Exception, IOException {
        return loadConfigsXml("123");
    }

    private static String loadConfigsXml(String pid) throws Exception, IOException {
        XmlComponentConfigurations cfgs = new XmlComponentConfigurations();

        List<ComponentConfigurationImpl> cfglist = new ArrayList<ComponentConfigurationImpl>();
        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        cfg.setPid(pid);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("pass", "pass");
        cfg.setProperties(props);
        Tocd definition = new Tocd();
        definition.setDescription("description");
        cfg.setDefinition(definition);
        cfglist.add(cfg);
        cfgs.setConfigurations(cfglist);

        StringWriter w = new StringWriter();
        XmlUtil.marshal(cfgs, w);
        String cfgxml = w.toString();
        w.close();

        return cfgxml;
    }
    @Test
    public void testUpdateConfigurationPidPropertiesInvalid() throws KuraException {
        // try it with a registered component and an existing PID with invalid properties
        String pid = TEST_COMPONENT_PID;

        Map<String, Object> properties = new HashMap<String, Object>();
        String prop = "some unknown property";
        properties.put(prop, 123);
        properties.put(TEST_COMPONENT_PROPERTY_TEST, 1234);

        try {
            configurationService.updateConfiguration(pid, properties);
        } catch (KuraPartialSuccessException e) {
            assertEquals("Should contain one cause", 1, e.getCauses().size());
            KuraException e1 = (KuraException) e.getCauses().get(0);
            assertEquals("Should specify invalid value", KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e1.getCode());
        }
    }

    @Test
    public void testUpdateConfigurationPidPropertiesNoSnapshot() throws KuraException {
        // existing component PID and takeSnapshot == false
        String pid = TEST_COMPONENT_PID;
        boolean takeSnapshot = false;

        Set<Long> snapshots = configurationService.getSnapshots();
        int size1 = snapshots.size();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(TEST_COMPONENT_PROPERTY_TEST, 10);

        configurationService.updateConfiguration(pid, properties, takeSnapshot);

        ComponentConfiguration config = configurationService.getComponentConfiguration(pid);
        assertEquals(10, config.getConfigurationProperties().get(TEST_COMPONENT_PROPERTY_TEST));

        snapshots = configurationService.getSnapshots();
        int size2 = snapshots.size();
        assertEquals("There should be no more snapshots", size1, size2);
    }

    @Test
    public void testUpdateConfigurationsConfigs() throws KuraException {
        String pid = TEST_COMPONENT_PID;

        Set<Long> snapshots = configurationService.getSnapshots();
        int size1 = snapshots.size();

        Map<String, Object> properties = new HashMap<String, Object>();
        String prop = "some property";
        properties.put(prop, 123);
        properties.put(TEST_COMPONENT_PROPERTY_TEST, 5);

        List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();
        ComponentConfiguration config = new ComponentConfigurationImpl(pid, null, properties);
        configs.add(config);

        configurationService.updateConfigurations(configs);

        config = configurationService.getComponentConfiguration(pid);
        assertEquals(5, config.getConfigurationProperties().get(TEST_COMPONENT_PROPERTY_TEST));
        assertEquals(123, config.getConfigurationProperties().get(prop));

        snapshots = configurationService.getSnapshots();
        int size2 = snapshots.size();
        assertEquals("There should be one more snapshot", size1 + 1, size2);
    }

    @Test
    public void testSnapshot() throws KuraException {
        // create a new snapshot - make sure that there are no unsaved changes
        long maxId = configurationService.snapshot();

        // create another new snapshot
        long id = configurationService.snapshot();

        assertTrue("Bigger ID", id > maxId);

        Set<Long> snapshots = configurationService.getSnapshots();

        assertTrue("New ID 1 should be in the new snapshots list", snapshots.contains(maxId));
        assertTrue("New ID 2 should be in the new snapshots list", snapshots.contains(id));

        // no change between previous and this snapshot
        List<ComponentConfiguration> oldS = configurationService.getSnapshot(maxId);
        List<ComponentConfiguration> newS = configurationService.getSnapshot(id);

        assertEquals(oldS.toString(), newS.toString());
    }

    @Test
    public void testRollbackEmpty() throws KuraException {
        // remove all other snapshots
        File dir = new File(kuraSnapshotsDir);
        File[] snapshots = dir.listFiles();
        for (File f : snapshots) {
            f.delete();
        }

        // update configuration, but don't make a new snapshot
        configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID,
                "pid_rollback_dontsave_" + System.currentTimeMillis(), null, false);

        // check that rollback reverted to the recently created snapshot
        try {
            configurationService.rollback();
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, e.getCode());
        }
    }

    // FIXME: this test proves it doesn't work as the API describes: previous to last snapshot is restored, not the last
    // one
    @Test
    public void testRollbackNotSaved() throws KuraException {
        Set<Long> snapshots = configurationService.getSnapshots();
        if (snapshots.size() < 2) {
            return;
        }
        long previousToLastID = snapshots.toArray(new Long[1])[snapshots.size() - 1];

        // create a new snapshot - make sure that there are no unsaved changes
        long maxId = configurationService.snapshot();

        // update configuration, but don't make a new snapshot
        configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID,
                "pid_rollback_dontsave_" + System.currentTimeMillis(), null, false);

        // check that rollback reverted to the recently created snapshot
        long rollbackId = configurationService.rollback();

        assertTrue(maxId > rollbackId);
        assertEquals(previousToLastID, rollbackId);
    }

    // FIXME: this test proves it doesn't work as the API describes: previous to last snapshot is restored, not the last
    // one
    @Test
    public void testRollback() throws KuraException {
        // create a new snapshot - make sure that there are no unsaved changes
        long maxId = configurationService.snapshot();

        // update configuration and make a new snapshot
        configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID,
                "pid_rollback_" + System.currentTimeMillis(), null, true);

        // check that rollback reverted to the recently created snapshot
        long rollbackId = configurationService.rollback();

        assertEquals("Should have reverted to one-before-last snapshot", maxId, rollbackId);
    }

    public void testRollbackId() throws KuraException {
        long id = configurationService.snapshot();

        List<ComponentConfiguration> snapshot1 = configurationService.getSnapshot(id);

        configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID,
                "pid_rollback_" + System.currentTimeMillis(), null, true);

        configurationService.rollback(id);

        List<ComponentConfiguration> snapshot2 = configurationService.getComponentConfigurations();

        // this doesn't seem to be OK...
        assertEquals(snapshot1.toString(), snapshot2.toString());
    }

    @Test
    public void testEncryptSnapshots() throws Exception {
        XmlComponentConfigurations cfgs = new XmlComponentConfigurations();

        List<ComponentConfigurationImpl> cfglist = new ArrayList<ComponentConfigurationImpl>();
        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        cfg.setPid("123");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("pass", "pass");
        cfg.setProperties(props);
        Tocd definition = new Tocd();
        definition.setDescription("description");
        cfg.setDefinition(definition);
        cfglist.add(cfg);
        cfgs.setConfigurations(cfglist);

        StringWriter w = new StringWriter();
        XmlUtil.marshal(cfgs, w);
        String cfgxml = w.toString();
        w.close();

        File f1 = new File(kuraSnapshotsDir, "snapshot_123456.xml");
        f1.createNewFile();
        f1.deleteOnExit();

        FileWriter fw = new FileWriter(f1);
        fw.append(cfgxml);
        fw.close();

        long snapshotId = configurationService.snapshot();

        Set<Long> snapshots = configurationService.getSnapshots();
        assertEquals(2, snapshots.size());
        assertTrue(snapshots.contains(123456L));
        assertTrue(snapshots.contains(snapshotId));

        FileReader fr = new FileReader(f1);
        char[] chars = new char[100];
        fr.read(chars);
        fr.close();

        String s = new String(chars);

        assertTrue("Snapshot should be encrypted", s.startsWith("encrypted"));
    }

    // a unit test, just to see it working
    @Test
    public void testUpdateConfigurationsListOfComponentConfigurationBoolean()
            throws KuraException, NoSuchFieldException {
        // test that password encryption is attempted (but decrypt doesn't fail, which is OK) and some other calls are
        // made - stop with usage of m_allActivatedPids in getComponentConfigurationsInternal

        boolean takeSnapshot = false;
        final List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();
        configs.add(null);
        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<String, Object>();
        cfg.setProperties(props);
        props.put("pass", new Password("pass"));
        configs.add(cfg);

        ConfigurationServiceImpl cs = new ConfigurationServiceImpl();

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        cs.setCryptoService(cryptoServiceMock);

        when(cryptoServiceMock.decryptAes((char[]) anyObject())).thenReturn("dec".toCharArray());

        // make updateConfigurationsInternal fail with NPE
        TestUtil.setFieldValue(cs, "m_allActivatedPids", null);

        try {
            cs.updateConfigurations(configs, takeSnapshot);
            fail("Exception expected");
        } catch (NullPointerException e) {
            // OK
        }

        verify(cryptoServiceMock, times(1)).decryptAes((char[]) anyObject());
    }
}

class MultiStepCSValidator implements CSValidator {

    private XmlComponentConfigurations configs;
    private String factoryPid;
    private String pid;

    public MultiStepCSValidator() {
    }

    public MultiStepCSValidator(String factoryPid, String pid) {
        this.factoryPid = factoryPid;
        this.pid = pid;
    }

    @Override
    public boolean validateEncryptArgs(Object[] args) {
        String arg0 = new String(((char[]) args[0]));
        XmlComponentConfigurations configurations = null;
        try {
            configurations = XmlUtil.unmarshal(arg0, XmlComponentConfigurations.class);
        } catch (Exception e) {
        }

        if (configs == null) { // first pass
            assertTrue("At least one configuration expected", configurations.getConfigurations().size() >= 1);
            boolean found = false;
            for (ComponentConfigurationImpl cfg : configurations.getConfigurations()) {
                if (pid.compareTo(cfg.getPid()) == 0) {
                    assertEquals(factoryPid, cfg.getConfigurationProperties().get("service.factoryPid"));
                    found = true;
                }
            }
            assertTrue("Our configuration should be found.", found);
        }
        configs = configurations;

        return true;
    }

    @Override
    public boolean validateDecryptArgs(Object[] args) throws KuraException {
        String arg0 = new String((char[]) args[0]);
        if (arg0.startsWith("<?xml")) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        }

        return true;
    }

    @Override
    public String getEncrypted() {
        return "encrypted";
    }

    @Override
    public String getDecrypted() {
        if (configs != null) {
            try {
                StringWriter w = new StringWriter();
                XmlUtil.marshal(configs, w);
                String cfgxml = w.toString();
                w.close();

                return cfgxml;
            } catch (Exception e) {
            }
        }

        try {
            return ConfigurationServiceTest.loadConfigsXml();
        } catch (Exception e) {
            // so be it...
        }
        return "";
    }
}