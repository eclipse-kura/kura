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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.util.XmlUtil;
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

    private static void resetMocks() throws KuraException {
        reset(csMock);

        when(csMock.encryptAes((char[]) Mockito.anyObject())).thenAnswer(new Answer<char[]>() {

            @Override
            public char[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                logger.debug(new String((char[]) args[0]));
                return "encrypted".toCharArray();
            }

        });
        when(csMock.decryptAes((char[]) Mockito.anyObject())).thenAnswer(new Answer<char[]>() {

            @Override
            public char[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                logger.debug(new String((char[]) args[0]));
                return loadConfigsXml().toCharArray();
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
        logger.info(sysService.getClass().getName());
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
        assertNotNull("Configuration service not null.", configurationService);
        assertNotNull("System service not null.", systemService);
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

    // @Test
    public void testCreateFactoryConfigurationMergePropertiesAndSnapshot() throws KuraException, IOException {
        // a positive test, take passed properties into account, without snapshot creation

        final String factoryPid = DATA_SERVICE_FACTORY_PID;
        final String pid = "cfcmp_pid_" + System.currentTimeMillis();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("key", "value");
        final boolean takeSnapshot = true;

        Set<Long> snapshots = configurationService.getSnapshots();

        configurationService.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot);

        Set<Long> snapshotsAfter = configurationService.getSnapshots();

        int expectedSize = Math.min(kuraSnapshotsCount, snapshots.size() + 1);
        assertEquals("One more snapshot", expectedSize, snapshotsAfter.size());

        // verify that the new snapshot contains our new property
        snapshotsAfter.removeAll(snapshots);

        List<ComponentConfiguration> snapshot = configurationService
                .getSnapshot(snapshotsAfter.iterator().next().longValue());
        boolean found = false;
        for (ComponentConfiguration cc : snapshot) {
            if (pid.compareTo(cc.getPid()) == 0) {
                Map<String, Object> props = cc.getConfigurationProperties();
                assertTrue("Contains our key", props.containsKey("key"));
                assertEquals("Contains the right value", "value", props.get("key"));

                found = true;

                break;
            }
        }
        assertTrue("Configuration verified", found);
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
    public void testGetConfigurableComponentPids() {
        // positive test: get pids, assert they are not modifiable outside
        Set<String> configurableComponentPids = configurationService.getConfigurableComponentPids();

        assertFalse("Not empty", configurableComponentPids.isEmpty());

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

        assertFalse("Not empty", configurableComponentPids.isEmpty());
        assertFalse("Does not contain our service PID", configurableComponentPids.contains(servicePid));

        int size1 = configurableComponentPids.size();

        // create new configuration
        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, takeSnapshot);

        // check results
        Set<String> configurableComponentPids2 = configurationService.getConfigurableComponentPids();

        int size2 = configurableComponentPids2.size();

        assertEquals("Additional PID", size1 + 1, size2);
        assertTrue("Contains our service PID", configurableComponentPids2.contains(servicePid));
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

        assertEquals("One more configuration", configurations.size() + 1, configurationsAfter.size());
        configurationsAfter.removeAll(configurations);

        boolean found = false;
        for (ComponentConfiguration cc : configurationsAfter) {
            if (servicePid.compareTo(cc.getPid()) == 0) {
                found = true;
                break;
            }
        }

        assertTrue("Found new configuration", found);
    }

    @Test
    public void testGetComponentConfigurationNull() throws KuraException {
        ComponentConfiguration configuration = configurationService.getComponentConfiguration(null);

        assertNull("Null produces null", configuration);
    }

    @Test
    public void testGetComponentConfiguration() throws KuraException {
        String factoryPid = "fpid_gcc_" + System.currentTimeMillis();
        final String servicePid = "spid_gcc_" + System.currentTimeMillis();
        final boolean takeSnapshot = false;

        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, takeSnapshot);

        ComponentConfiguration configuration = configurationService.getComponentConfiguration(servicePid);

        assertNotNull("Configuration is returned", configuration);
        assertEquals("Correct PID is returned", servicePid, configuration.getPid());
    }

    @Test
    public void testGetDefaultComponentConfigurationNull() throws KuraException {
        // default configuration of null is empty, with no PID set

        ComponentConfiguration configuration = configurationService.getDefaultComponentConfiguration(null);

        assertNotNull("Configuration is not null", configuration);
        assertNull("PID is null", configuration.getPid());
        assertTrue("Empty properties", configuration.getConfigurationProperties().isEmpty());
        assertNull("No definition", configuration.getDefinition());
    }

    @Test
    public void testGetDefaultComponentConfigurationNonExisting() throws KuraException {
        // default configuration of a non-existing service is empty

        final String servicePid = "spid_gdccne_" + System.currentTimeMillis();

        ComponentConfiguration configuration = configurationService.getDefaultComponentConfiguration(servicePid);

        assertNotNull("Configuration is not null", configuration);
        assertEquals("PID is set", servicePid, configuration.getPid());
        assertTrue("Empty properties", configuration.getConfigurationProperties().isEmpty());
    }

    @Test
    public void testGetDefaultComponentConfiguration() throws KuraException {
        // default configuration of a new service is empty

        final String factoryPid = "fpid_gdcc_" + System.currentTimeMillis();
        final String servicePid = "spid_gdcc_" + System.currentTimeMillis();

        configurationService.createFactoryConfiguration(factoryPid, servicePid, null, false);

        ComponentConfiguration configuration = configurationService.getDefaultComponentConfiguration(servicePid);

        assertNotNull("Configuration is not null", configuration);
        assertEquals("PID is set", servicePid, configuration.getPid());
        assertTrue("Empty properties", configuration.getConfigurationProperties().isEmpty());
        assertNull("No definition", configuration.getDefinition());
    }

    @Test
    public void testGetDefaultComponentConfigurationExisting() throws KuraException {
        // default configuration of an existing service is ...

        ComponentConfiguration configuration = configurationService
                .getDefaultComponentConfiguration(TEST_COMPONENT_PID);

        assertNotNull("Configuration is not null", configuration);
        assertEquals("PID is set", TEST_COMPONENT_PID, configuration.getPid());
        assertNotNull("Property exists", configuration.getConfigurationProperties());
        assertEquals("1 property exists", 1, configuration.getConfigurationProperties().size());
        assertEquals("Test property exists", 1, configuration.getConfigurationProperties().get("field.test"));
        assertNotNull("Definition exists", configuration.getDefinition());
        assertNotNull("ID exists", configuration.getDefinition().getId());
        assertNotNull("Name exists", configuration.getDefinition().getName());
        assertNotNull("Description exists", configuration.getDefinition().getDescription());
        assertNotNull("Icon exists", configuration.getDefinition().getIcon());
        assertNotNull("AD exists", configuration.getDefinition().getAD());
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

        assertNull("Configuration is null", configuration);
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
        assertEquals("No change", val, configurationProperties.get(TEST_COMPONENT_PROPERTY_TEST));
    }

    private static String loadConfigsXml() throws Exception, IOException {
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

}
