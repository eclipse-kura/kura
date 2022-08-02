/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.configuration;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraPartialSuccessException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.system.SystemService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

public class ConfigurationServiceTest {

    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String TEST_COMPONENT_PID = "org.eclipse.kura.core.configuration.CfgSvcTestComponent";
    private static final String TEST_SELF_COMPONENT_PID = "org.eclipse.kura.core.configuration.CfgSvcTestSelfComponent";
    private static final String TEST_COMPONENT_FPID = "org.eclipse.kura.core.configuration.TestFactoryComponent";
    private static final String TEST_COMPONENT_PROPERTY_KEY = "field.test";
    private static final int TEST_COMPONENT_PROPERTY_VALUE = 1;
    private static final String TEST_SELF_COMPONENT_PROPERTY_KEY = "TestADId";
    private static final String TEST_SELF_COMPONENT_PROPERTY_VALUE = "TestADDefaultValue";
    private static final String KURA_SNAPSHOTS_DIR = "/tmp/kura/snapshots";

    /*
     * OSGi dependencies
     */

    private static CountDownLatch dependencyLatch = new CountDownLatch(3);
    private static ConfigurationService configurationService;
    private static OCDService ocdService;
    private static SystemService systemService;

    protected void bindConfigService(final ConfigurationService configService) {
        if (ConfigurationServiceTest.configurationService == null) {
            ConfigurationServiceTest.configurationService = configService;
            dependencyLatch.countDown();
        }
    }

    protected void unbindConfigService(final ConfigurationService configService) {
        if (ConfigurationServiceTest.configurationService == configService) {
            ConfigurationServiceTest.configurationService = null;
        }
    }

    protected void bindOcdService(final OCDService ocdService) {
        if (ConfigurationServiceTest.ocdService == null) {
            ConfigurationServiceTest.ocdService = ocdService;
            dependencyLatch.countDown();
        }
    }

    protected void unbindOcdService(final OCDService ocdService) {
        if (ConfigurationServiceTest.ocdService == ocdService) {
            ConfigurationServiceTest.ocdService = null;
        }
    }

    protected void bindSystemService(final SystemService sysService) {
        if (ConfigurationServiceTest.systemService == null) {
            ConfigurationServiceTest.systemService = sysService;
            dependencyLatch.countDown();
        }
    }

    protected void unbindSystemService(final SystemService sysService) {
        if (ConfigurationServiceTest.systemService == sysService) {
            ConfigurationServiceTest.systemService = null;
        }
    }

    @BeforeClass
    public static void awaitDependencies() throws Exception {
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);

            assertTrue("Dependencies OK.", ok);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled.");
        }
    }

    /*
     * Gherkin-style tests
     */

    private int kuraSnapshotsCount = 10;
    private String kuraSnapshotsDir = KURA_SNAPSHOTS_DIR;

    // private BundleContext bundleContext;

    private Optional<Exception> exceptionOccurred;

    private Set<String> factoryComponentPids;
    private Map<String, Object> exampleProperties = new HashMap<String, Object>() {

        private static final long serialVersionUID = 1L;

        {
            put("key1", "value1");
            put("key2", "value2");
        }
    };
    private Set<Long> snapshotsBefore;
    private Set<Long> snapshotsAfter;
    private Set<String> configurableComponentPids;
    private List<ComponentConfiguration> configurations;
    private List<ComponentConfiguration> inputConfigurations;
    private ComponentConfiguration configuration;
    private ComponentConfiguration defaultConfiguration;
    private ComponentConfiguration updatedConfiguration;
    private long rollbackID;
    private List<ComponentConfiguration> factoryComponentOCDs;
    private List<ComponentConfiguration> serviceProviderOCDs;

    /*
     * Scenarios
     */

    @Test
    public void testGetFactoryComponentPids() {
        // positive test; test component pid should be contained in the factory pids

        whenGetFactoryComponentPids();

        thenFactoryComponentPidsAreMoreThan(1);
        thenFactoryComponentPidsContain(TEST_COMPONENT_FPID);
        thenNoExceptionOccurred();
    }

    @Test
    public void testCreateFactoryConfigurationNulls() {
        // negative test; cannot create a component with a null factory

        whenCreateFactoryConfiguration(null, "testPid", null, false);

        thenIllegalArgumentExceptionOccurred();
    }

    @Test
    public void testCreateFactoryExistingPid() {
        // negative test; cannot create a factory comp. with an already existing pid

        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "existingPid", null, false);

        whenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "existingPid", null, false);

        thenKuraExceptionOccurred();
    }

    @Test
    public void testCreateFactoryConfigurationMergePropertiesAndSnapshot() {
        // positive test; the created snapshot contains the properties

        whenCreateFactoryConfiguration(TEST_COMPONENT_FPID, "cfcmp_pid_1", this.exampleProperties, true);

        thenSnapshotsIncreasedBy(1);
        thenLastSnapshotContainsProperties("cfcmp_pid_1", this.exampleProperties);
    }

    @Test
    public void testDeleteFactoryConfigurationNulls() throws KuraException {
        // negative test; null is given as pid

        whenDeleteFactoryConfiguration(null, false);

        thenKuraExceptionOccurred();
    }

    @Test
    public void testDeleteFactoryConfigurationNonExistingFactoryPid() {
        // negative test; pid not registered

        whenDeleteFactoryConfiguration("pid_non_existent", false);

        thenNoExceptionOccurred();
    }

    @Test
    public void testDeleteFactoryConfigurationWithSnapshot() throws KuraException, IOException, NoSuchFieldException {
        // positive test; pid registered in factory and service pids, configuration delete is expected, with snapshots

        givenCreateFactoryConfiguration("fpid_" + System.currentTimeMillis(), "spid-test1", this.exampleProperties,
                true);

        whenDeleteFactoryConfiguration("spid-test1", true);

        thenSnapshotsIncreasedBy(1);
        thenLastSnapshotNotContainsProperties("spid-test1", this.exampleProperties);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetConfigurableComponentPids() {
        // positive test: get pids, assert they are not modifiable outside

        whenGetConfigurableComponentPids();

        thenConfigurableComponentPidsNotAssignable();
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetConfigurableComponentPidsAdd() {
        // positive test: add a new configuration and find it later

        givenGetConfigurableComponentPids();

        whenCreateFactoryConfiguration("fpid_test", "spid_ccpa_1234", null, true);

        thenConfigurableComponentPidsContainPid("spid_ccpa_1234");
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetComponentConfigurations() {
        // positive test; new pid registered => new configuration

        whenCreateFactoryConfiguration("fpid_gcc_11" + System.currentTimeMillis(), "spid_gcc_111", null, true);

        thenSnapshotsIncreasedBy(1);
        thenLastSnapshotContainsProperties("spid_gcc_111", null);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetComponentConfigurationsFilterNonExistentPid() {

        whenGetComponentConfigurations("foo");

        thenConfigurationsSizeIs(0);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetComponentConfigurationsFilterCofServicePid() {

        whenGetComponentConfigurations("org.eclipse.kura.configuration.ConfigurationService");

        thenConfigurationsSizeIs(0);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetComponentConfigurationsFilterExistentPid() {

        whenGetComponentConfigurations(TEST_COMPONENT_PID);

        thenConfigurationsContains(TEST_COMPONENT_PID);
        thenConfigurationsSizeIs(1);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetComponentConfigurationNull() {

        whenGetComponentConfiguration(null);

        thenConfigurationIsNull();
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetComponentConfiguration() {
        givenCreateFactoryConfiguration("test-factory", "cc-test-pid", null, false);

        whenGetComponentConfiguration("cc-test-pid");

        thenConfigurationHasPid("cc-test-pid");
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetDefaultComponentConfigurationNull() {
        // default configuration of null is empty, with no PID set

        whenGetDefaultComponentConfiguration(null);

        thenDefaultConfigurationIs(null, true, true);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetDefaultComponentConfigurationNonExisting() {
        // default configuration of a non-existing service is empty

        whenGetDefaultComponentConfiguration("spid_gdccne_90");

        thenDefaultConfigurationIs("spid_gdccne_90", true, true);
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetDefaultComponentConfiguration() {
        // default configuration of a new service is empty

        givenCreateFactoryConfiguration("example-factory", "example-pid", null, false);

        whenGetDefaultComponentConfiguration("example-pid");

        thenDefaultConfigurationIs("example-pid", true, true);
    }

    @Test
    public void testGetDefaultComponentConfigurationExisting() {
        // default configuration of an existing service is ...

        whenGetDefaultComponentConfiguration(TEST_COMPONENT_PID);

        thenDefaultConfigurationIs(TEST_COMPONENT_PID, false, false);
        thenDefaultConfigurationPropertiesHas(1, TEST_COMPONENT_PROPERTY_KEY, TEST_COMPONENT_PROPERTY_VALUE);
        thenDefaultConfigurationDefinitionIsPopulated();
        thenNoExceptionOccurred();
    }

    @Test
    public void testGetDefaultSelfConfiguringComponentConfigurationExisting() {
        givenSelfConfiguringComponentConfiguration(TEST_SELF_COMPONENT_PID, TEST_SELF_COMPONENT_PID);

        whenGetDefaultComponentConfiguration(TEST_SELF_COMPONENT_PID);

        thenDefaultConfigurationIs(TEST_SELF_COMPONENT_PID, false, false);
        thenDefaultConfigurationPropertiesHas(1, TEST_SELF_COMPONENT_PROPERTY_KEY, TEST_SELF_COMPONENT_PROPERTY_VALUE);
        thenDefaultConfigurationDefinitionIsPopulated();
        thenNoExceptionOccurred();
    }

    @Test
    public void testUpdateConfigurationPidPropertiesNull() {
        whenUpdateConfiguration(null, null);

        thenNPExceptionOccurred();
    }

    @Test
    public void testUpdateConfigurationPidPropertiesNullProps() {
        givenUpdateConfiguration("abcdefg", null);

        whenGetComponentConfiguration("abcdefg");

        thenConfigurationIsNull();
        thenNoExceptionOccurred();
    }

    @Test
    public void testUpdateConfigurationPidPropertiesEmptyProps() {
        // try it with a registered component and an existing PID with empty properties
        givenGetComponentConfiguration(TEST_COMPONENT_PID);

        whenUpdateConfiguration(TEST_COMPONENT_PID, new HashMap<String, Object>());

        thenConfigurationPropertiesHaveNotChanged();
    }

    @Test
    public void testUpdateConfigurationPidPropertiesValid() {
        // try it with a registered component and an existing PID with invalid properties
        whenUpdateConfiguration(TEST_COMPONENT_PID, new HashMap<String, Object>() {

            private static final long serialVersionUID = 7567583973175478794L;

            {
                put("unknownProperty", 123);
                put(TEST_COMPONENT_PROPERTY_KEY, 2);
            }
        });

        thenUpdatedConfigurationPropertiesContains(new HashMap<String, Object>() {

            private static final long serialVersionUID = -7139379355635306015L;

            {
                put("unknownProperty", 123);
                put(TEST_COMPONENT_PROPERTY_KEY, 2);
            }
        });
        thenSnapshotsIncreasedBy(1);
        thenNoExceptionOccurred();
        thenLastSnapshotContainsProperties(TEST_COMPONENT_PID, new HashMap<String, Object>() {

            private static final long serialVersionUID = -4728655312644177496L;

            {
                put("unknownProperty", 123);
                put(TEST_COMPONENT_PROPERTY_KEY, 2);
            }
        });
    }

    @Test
    public void testUpdateConfigurationPidPropertiesInvalid() {
        // try it with a registered component and an existing PID with invalid properties
        whenUpdateConfiguration(TEST_COMPONENT_PID, new HashMap<String, Object>() {

            private static final long serialVersionUID = 7567583973175478794L;

            {
                put("unknown property", 123);
                put(TEST_COMPONENT_PROPERTY_KEY, 9999);
            }
        });

        thenKuraPartialSuccessExceptionOccurred();
    }

    @Test
    public void testUpdateConfigurationPidPropertiesNoSnapshot() {
        // existing component PID and takeSnapshot == false

        whenUpdateConfiguration(TEST_COMPONENT_PID, new HashMap<String, Object>() {

            private static final long serialVersionUID = 7567583973175478794L;

            {
                put("unknown property", 123);
                put(TEST_COMPONENT_PROPERTY_KEY, 10);
            }
        }, false);

        thenSnapshotsIncreasedBy(0);
        thenUpdatedConfigurationPropertiesContains(new HashMap<String, Object>() {

            private static final long serialVersionUID = 7567583973175478794L;

            {
                put("unknown property", 123);
                put(TEST_COMPONENT_PROPERTY_KEY, 10);
            }
        });
        thenNoExceptionOccurred();
    }

    @Test
    public void testUpdateConfigurationsConfigs() {
        givenInputConfigurationsWithExampleProperties("ex1", "ex2", "ex3");

        whenUpdateConfigurations();

        thenComponentConfigurationsContainPidsWithExampleProperties("ex1", "ex2", "ex3");
        thenSnapshotsIncreasedBy(1);
        thenLastSnapshotNotContainsProperties("ex1", this.exampleProperties);
        thenLastSnapshotNotContainsProperties("ex2", this.exampleProperties);
        thenLastSnapshotNotContainsProperties("ex3", this.exampleProperties);
        thenNoExceptionOccurred();
    }

    @Test
    public void testSnapshot() {
        givenSnapshotBefore();

        whenSnapshot();

        thenLastSnapshotHasNotChanged();
        thenNoExceptionOccurred();
    }

    @Test
    public void testRollbackEmpty() {
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_dontsave_", null, false);
        givenNoSnapshotsInKuraDir();

        whenRollback();

        thenKuraExceptionOccurred(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND);
    }

    @Test
    public void testRollbackNotSaved() {
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_1", null, true);
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_2", null, true);
        givenSnapshotBefore();
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_dontsave_2", null, false);

        whenRollback();

        thenRollbackIdIsLessThanPrevious();
        thenLastSnapshotNotContainsProperties("pid_rollback_dontsave_2", null);
    }

    @Test
    public void testRollback() {
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_1", null, true);
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_2", null, true);
        givenSnapshotBefore();
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_3", null, true);

        whenRollback();

        thenRollbackIdIsGreaterThanPrevious();
    }

    @Test
    public void testRollbackId() {
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_", this.exampleProperties, true);

        whenRollback();

        thenLastSnapshotContainsProperties("pid_rollback_", this.exampleProperties);
        thenSnapshotsIncreasedBy(-1);
    }

    @Test
    public void testEncryptSnapshots() {
        givenCreateFactoryConfiguration(DATA_SERVICE_FACTORY_PID, "pid_rollback_1", null, true);

        whenSnapshot();

        thenLastSnapshotIsEncrypted();
        thenNoExceptionOccurred();
    }

    @Test
    public void testShouldGetFactoryComponentDefinitions() {
        whenGetFactoryComponentOCDs();

        thenContainsWireComponentsDefinitions(this.factoryComponentOCDs, false);
    }

    @Test
    public void testShouldGetServiceProviderDefinitions() {
        whenGetServiceProviderOCDs();

        thenContainsWireComponentsDefinitions(this.serviceProviderOCDs, true);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenCreateFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties,
            boolean takeSnapshot) {
        try {
            ConfigurationServiceTest.configurationService.createFactoryConfiguration(factoryPid, pid, properties,
                    takeSnapshot);

            if (takeSnapshot) {
                this.snapshotsBefore = ConfigurationServiceTest.configurationService.getSnapshots();
            }
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void givenSelfConfiguringComponentConfiguration(String pid, String servicePid) {
        try {
            Dictionary<String, Object> componentConfigurationProperties = new Hashtable<>();
            componentConfigurationProperties.put(KURA_SERVICE_PID, pid);
            componentConfigurationProperties.put(SERVICE_PID, servicePid);
            BundleContext bundleContext = FrameworkUtil.getBundle(ConfigurationServiceTest.class).getBundleContext();
            bundleContext.registerService(SelfConfiguringComponent.class, new CfgSvcTestSelfComponent(),
                    componentConfigurationProperties);

        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void givenGetConfigurableComponentPids() {
        this.configurableComponentPids = ConfigurationServiceTest.configurationService.getConfigurableComponentPids();
    }

    private void givenUpdateConfiguration(String pid, Map<String, Object> properties) {
        try {
            ConfigurationServiceTest.configurationService.updateConfiguration(pid, properties);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void givenGetComponentConfiguration(String pid) {
        try {
            this.configuration = ConfigurationServiceTest.configurationService.getComponentConfiguration(pid);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void givenInputConfigurationsWithExampleProperties(String... pids) {
        this.inputConfigurations = new ArrayList<ComponentConfiguration>();

        for (String pid : pids) {
            ComponentConfiguration conf = new ComponentConfigurationImpl(pid, null, this.exampleProperties);
            this.inputConfigurations.add(conf);
        }
    }

    private void givenSnapshotBefore() {
        try {
            this.snapshotsBefore.clear();
            this.snapshotsBefore.add(configurationService.snapshot());
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void givenNoSnapshotsInKuraDir() {
        try {
            cleanSnapshots();
        } catch (KuraException e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    /*
     * When
     */

    private void whenGetFactoryComponentPids() {
        this.factoryComponentPids = ConfigurationServiceTest.configurationService.getFactoryComponentPids();
    }

    private void whenCreateFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties,
            boolean takeSnapshot) {
        try {
            ConfigurationServiceTest.configurationService.createFactoryConfiguration(factoryPid, pid, properties,
                    takeSnapshot);

            if (takeSnapshot) {
                this.snapshotsAfter = ConfigurationServiceTest.configurationService.getSnapshots();
            }
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenDeleteFactoryConfiguration(String pid, boolean takeSnapshot) {
        try {
            ConfigurationServiceTest.configurationService.deleteFactoryConfiguration(pid, takeSnapshot);

            if (takeSnapshot) {
                this.snapshotsAfter = ConfigurationServiceTest.configurationService.getSnapshots();
            }
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenGetConfigurableComponentPids() {
        this.configurableComponentPids = ConfigurationServiceTest.configurationService.getConfigurableComponentPids();
    }

    private void whenGetComponentConfigurations(String searchedPid) {
        try {
            this.configurations = configurationService
                    .getComponentConfigurations(FrameworkUtil.createFilter("(kura.service.pid=" + searchedPid + ")"));
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenGetComponentConfiguration(String searchedPid) {
        try {
            this.configuration = configurationService.getComponentConfiguration(searchedPid);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenGetDefaultComponentConfiguration(String searchedPid) {
        try {
            this.defaultConfiguration = configurationService.getDefaultComponentConfiguration(searchedPid);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenUpdateConfiguration(String pid, Map<String, Object> properties) {
        try {
            ConfigurationServiceTest.configurationService.updateConfiguration(pid, properties);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }

        try {
            this.snapshotsAfter = ConfigurationServiceTest.configurationService.getSnapshots();
            this.updatedConfiguration = ConfigurationServiceTest.configurationService.getComponentConfiguration(pid);
        } catch (Exception e) {
            // ignore
        }
    }

    private void whenUpdateConfiguration(String pid, Map<String, Object> properties, boolean takeSnapshot) {
        try {
            ConfigurationServiceTest.configurationService.updateConfiguration(pid, properties, takeSnapshot);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }

        try {
            this.snapshotsAfter = ConfigurationServiceTest.configurationService.getSnapshots();
            this.updatedConfiguration = ConfigurationServiceTest.configurationService.getComponentConfiguration(pid);
        } catch (Exception e) {
            // ignore
        }
    }

    private void whenUpdateConfigurations() {
        try {
            ConfigurationServiceTest.configurationService.updateConfigurations(this.inputConfigurations);
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }

        try {
            this.snapshotsAfter = ConfigurationServiceTest.configurationService.getSnapshots();
        } catch (Exception e) {
            // ignore
        }
    }

    private void whenSnapshot() {
        try {
            ConfigurationServiceTest.configurationService.snapshot();
            this.snapshotsAfter = ConfigurationServiceTest.configurationService.getSnapshots();
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenRollback() {
        try {
            this.rollbackID = ConfigurationServiceTest.configurationService.rollback();
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenGetFactoryComponentOCDs() {
        try {
            this.factoryComponentOCDs = ocdService.getFactoryComponentOCDs();
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void whenGetServiceProviderOCDs() {
        this.serviceProviderOCDs = ocdService.getServiceProviderOCDs("org.eclipse.kura.wire.WireEmitter",
                "org.eclipse.kura.wire.WireReceiver", "org.eclipse.kura.wire.WireComponent");
    }

    /*
     * Then
     */

    private void thenFactoryComponentPidsAreMoreThan(int minPidsAmount) {
        assertTrue(this.factoryComponentPids.size() >= minPidsAmount);
    }

    private void thenFactoryComponentPidsContain(String pid) {
        assertTrue(this.factoryComponentPids.contains(pid));
    }

    private void thenKuraExceptionOccurred() {
        assertTrue(this.exceptionOccurred.isPresent());
        assertTrue(this.exceptionOccurred.get() instanceof KuraException);
    }

    private void thenKuraExceptionOccurred(KuraErrorCode code) {
        assertTrue(this.exceptionOccurred.isPresent());
        assertTrue(this.exceptionOccurred.get() instanceof KuraException);
        assertEquals(code, ((KuraException) this.exceptionOccurred.get()).getCode());
    }

    private void thenIllegalArgumentExceptionOccurred() {
        assertTrue(this.exceptionOccurred.isPresent());
        assertTrue(this.exceptionOccurred.get() instanceof IllegalArgumentException);
    }

    private void thenNPExceptionOccurred() {
        assertTrue(this.exceptionOccurred.isPresent());
        assertTrue(this.exceptionOccurred.get() instanceof NullPointerException);
    }

    private void thenKuraPartialSuccessExceptionOccurred() {
        assertTrue(this.exceptionOccurred.isPresent());
        assertTrue(this.exceptionOccurred.get() instanceof KuraPartialSuccessException);
    }

    private void thenNoExceptionOccurred() {
        assertFalse(this.exceptionOccurred.isPresent());
    }

    private void thenSnapshotsIncreasedBy(int amount) {
        assertEquals(Math.min(this.kuraSnapshotsCount, this.snapshotsBefore.size() + amount),
                this.snapshotsAfter.size());
    }

    private void thenLastSnapshotContainsProperties(String pid, Map<String, Object> expectedProperties) {
        try {
            if (this.snapshotsAfter == null) {
                this.snapshotsAfter = configurationService.getSnapshots();
            }
        } catch (KuraException e) {
            // ignore
        }

        Set<Long> before = this.snapshotsBefore;
        Set<Long> after = this.snapshotsAfter;

        after.removeAll(before);

        try {
            List<ComponentConfiguration> lastSnapshot = ConfigurationServiceTest.configurationService
                    .getSnapshot(after.iterator().next().longValue());
            assertTrue(snapshotContains(lastSnapshot, pid, expectedProperties));
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void thenLastSnapshotNotContainsProperties(String pid, Map<String, Object> expectedProperties) {
        try {
            if (this.snapshotsAfter == null) {
                this.snapshotsAfter = configurationService.getSnapshots();
            }
        } catch (KuraException e) {
            // ignore
        }

        Set<Long> before = this.snapshotsBefore;
        Set<Long> after = this.snapshotsAfter;

        after.removeAll(before);

        try {
            List<ComponentConfiguration> lastSnapshot = ConfigurationServiceTest.configurationService
                    .getSnapshot(after.iterator().next().longValue());
            assertFalse(snapshotContains(lastSnapshot, pid, expectedProperties));
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void thenLastSnapshotHasNotChanged() {
        Set<Long> before = this.snapshotsBefore;
        Set<Long> after = this.snapshotsAfter;

        after.removeAll(before);

        try {
            List<ComponentConfiguration> previousSnapshot = ConfigurationServiceTest.configurationService
                    .getSnapshot(before.iterator().next().longValue());

            List<ComponentConfiguration> lastSnapshot = ConfigurationServiceTest.configurationService
                    .getSnapshot(after.iterator().next().longValue());

            assertTrue(snapshotsEqual(previousSnapshot, lastSnapshot));
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void thenConfigurableComponentPidsNotAssignable() {
        try {
            this.configurableComponentPids.add("should-be-unsupported");
            fail("Updating PIDs should not be possible.");
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
    }

    private void thenConfigurableComponentPidsContainPid(String... pids) {
        Set<String> ccPids = configurationService.getConfigurableComponentPids();

        for (String pid : pids) {
            assertTrue(ccPids.contains(pid));
        }
    }

    private void thenConfigurationsContains(String pid) {
        assertTrue(this.configurations.stream().anyMatch(conf -> conf.getPid().equals(pid)));
    }

    private void thenConfigurationsSizeIs(int expectedSize) {
        assertEquals(expectedSize, this.configurations.size());
    }

    private void thenConfigurationIsNull() {
        assertNull(this.configuration);
    }

    private void thenConfigurationHasPid(String expectedPid) {
        assertEquals(expectedPid, this.configuration.getPid());
    }

    private void thenDefaultConfigurationIs(String expectedPid, boolean emptyProperties, boolean emptyDefinition) {
        assertNotNull("Configuration should not be null", this.defaultConfiguration);
        assertEquals(expectedPid, this.defaultConfiguration.getPid());
        assertEquals(this.defaultConfiguration.getConfigurationProperties().isEmpty(), emptyProperties);
        if (emptyDefinition) {
            assertNull("Should not be any definition", this.defaultConfiguration.getDefinition());
        } else {
            assertNotNull("Definition should not be empty", this.defaultConfiguration.getDefinition());
        }
    }

    private void thenDefaultConfigurationPropertiesHas(int size, String expectedKey, Object expectedValue) {
        assertNotNull(this.defaultConfiguration.getConfigurationProperties());
        assertEquals(size, this.defaultConfiguration.getConfigurationProperties().size());
        assertEquals(expectedValue, this.defaultConfiguration.getConfigurationProperties().get(expectedKey));
    }

    private void thenDefaultConfigurationDefinitionIsPopulated() {
        assertNotNull("Definition should exist", this.defaultConfiguration.getDefinition());
        assertNotNull("ID should exist", this.defaultConfiguration.getDefinition().getId());
        assertNotNull("Name should exist", this.defaultConfiguration.getDefinition().getName());
        assertNotNull("Description should exist", this.defaultConfiguration.getDefinition().getDescription());
        assertNotNull("Icon should exist", this.defaultConfiguration.getDefinition().getIcon());
        assertNotNull("AD should exist", this.defaultConfiguration.getDefinition().getAD());
        assertFalse("AD is not empty", this.defaultConfiguration.getDefinition().getAD().isEmpty());
    }

    private void thenConfigurationPropertiesHaveNotChanged() {
        assertTrue(propertiesEquals(this.configuration.getConfigurationProperties(),
                this.updatedConfiguration.getConfigurationProperties()));
    }

    private void thenUpdatedConfigurationPropertiesContains(Map<String, Object> expectedProps) {
        assertTrue(propertiesEquals(expectedProps, this.updatedConfiguration.getConfigurationProperties()));
    }

    private void thenComponentConfigurationsContainPidsWithExampleProperties(String... pids) {
        for (String pid : pids) {
            try {

                ComponentConfiguration cc = ConfigurationServiceTest.configurationService
                        .getComponentConfiguration(pid);

                assertTrue(propertiesEquals(this.exampleProperties, cc.getConfigurationProperties()));

            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void thenRollbackIdIsLessThanPrevious() {
        assertTrue(this.snapshotsBefore.iterator().next().longValue() > this.rollbackID);
    }

    private void thenRollbackIdIsGreaterThanPrevious() {
        assertTrue(this.snapshotsBefore.iterator().next().longValue() < this.rollbackID);
    }

    private void thenLastSnapshotIsEncrypted() {
        Set<Long> before = this.snapshotsBefore;
        Set<Long> after = this.snapshotsAfter;

        after.removeAll(before);

        long snapshotID = after.iterator().next().longValue();
        File file = new File(this.kuraSnapshotsDir, "snapshot_" + snapshotID + ".xml");

        try (FileReader fr = new FileReader(file)) {
            char[] chars = new char[100];
            fr.read(chars);

            String s = new String(chars);
            assertFalse("Snapshot should be encrypted", s.contains("kura.service.pid="));
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private void thenContainsWireComponentsDefinitions(List<ComponentConfiguration> configs, boolean includesAsset) {
        final String[] PIDS = { "org.eclipse.kura.wire.CloudPublisher", "org.eclipse.kura.wire.CloudSubscriber",
                "org.eclipse.kura.wire.Fifo", "org.eclipse.kura.wire.Logger", "org.eclipse.kura.wire.RegexFilter",
                "org.eclipse.kura.wire.Timer" };
        for (final String pid : PIDS) {
            assertTrue(configs.stream()
                    .filter(config -> config.getPid().equals(pid) && config.getDefinition().getId().equals(pid))
                    .findAny().isPresent());
        }
        boolean wireAssetFound = configs.stream().filter(
                config -> config.getPid().equals("org.eclipse.kura.wire.WireAsset") && config.getDefinition() == null)
                .findAny().isPresent();
        assertEquals(includesAsset, wireAssetFound);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        // this.bundleContext = null;
        // this.bundleContext = FrameworkUtil.getBundle(ConfigurationServiceTest.class).getBundleContext();

        this.exceptionOccurred = Optional.empty();

        try {
            cleanSnapshots();

            this.snapshotsBefore = ConfigurationServiceTest.configurationService.getSnapshots();
        } catch (Exception e) {
            this.exceptionOccurred = Optional.of(e);
        }
    }

    private boolean snapshotContains(List<ComponentConfiguration> snapshot, String pid,
            Map<String, Object> expectedProperties) throws KuraException {
        boolean found = false;

        for (ComponentConfiguration cc : snapshot) {
            if (pid.equals(cc.getPid())) {
                if (expectedProperties == null) {
                    return true;
                }

                for (Entry<String, Object> e : expectedProperties.entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();

                    if (cc.getConfigurationProperties().containsKey(key)
                            && value.equals(cc.getConfigurationProperties().get(key))) {
                        found = true;
                    }
                }
            }
        }

        return found;
    }

    private void cleanSnapshots() throws KuraException {
        if (systemService != null) {
            this.kuraSnapshotsCount = systemService.getKuraSnapshotsCount();
            this.kuraSnapshotsDir = systemService.getKuraSnapshotsDirectory();
        }

        // remove all other snapshots
        File dir = new File(this.kuraSnapshotsDir);
        dir.mkdirs();
        File[] snapshots = dir.listFiles();
        if (snapshots != null) {
            for (File f : snapshots) {
                f.delete();
            }
        }
    }

    private boolean snapshotsEqual(List<ComponentConfiguration> snap1, List<ComponentConfiguration> snap2) {
        List<String> pids1 = snap1.stream().flatMap(cc -> Stream.of(cc.getPid())).collect(Collectors.toList());
        List<String> pids2 = snap2.stream().flatMap(cc -> Stream.of(cc.getPid())).collect(Collectors.toList());

        return pids1.containsAll(pids2) && pids2.containsAll(pids1);
    }

    private boolean propertiesEquals(Map<String, Object> p1, Map<String, Object> p2) {
        for (Entry<String, Object> e1 : p1.entrySet()) {
            if (!p2.entrySet().stream()
                    .anyMatch(e2 -> e2.getKey().equals(e1.getKey()) && e2.getValue().equals(e1.getValue()))) {
                return false;
            }
        }

        return true;
    }
}