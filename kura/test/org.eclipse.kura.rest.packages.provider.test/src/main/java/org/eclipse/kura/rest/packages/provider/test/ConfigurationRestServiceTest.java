/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.packages.provider.test;

import static java.util.Collections.singletonMap;
import static org.eclipse.kura.core.testutil.json.JsonProjection.self;
import static org.eclipse.kura.rest.configuration.provider.test.ConfigurationUtil.adBuilder;
import static org.eclipse.kura.rest.configuration.provider.test.ConfigurationUtil.configurationBuilder;
import static org.eclipse.kura.rest.configuration.provider.test.ConfigurationUtil.ocdBuilder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.testutil.json.JsonProjection;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

@RunWith(Parameterized.class)
public class ConfigurationRestServiceTest extends AbstractRequestHandlerTest {

    private char[] EncryptedPassword;

    @Test
    public void shouldSupportGetSnapshots() throws KuraException {
        givenMockGetSnapshotsReturnEmpty();

        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("[]");
    }

    private static DeploymentAgentService deploymentAgentService = Mockito.mock(DeploymentAgentService.class);
    private static ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final CryptoService cryptoService;

    private final Map<String, Map<String, Object>> receivedConfigsByPid = new HashMap<>();

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport("deploy/v2"));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

        final ConfigurationAdmin configurationAdmin = WireTestUtil
                .trackService(ConfigurationAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);

        final Configuration config = configurationAdmin
                .getConfiguration("org.eclipse.kura.internal.rest.configuration.ConfigurationRestService", "?");
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("ConfigurationService.target", "(kura.service.pid=mockConfigurationService)");

        config.update(properties);

        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "mockConfigurationService");

        FrameworkUtil.getBundle(ConfigurationRestServiceTest.class).getBundleContext()
                .registerService(ConfigurationService.class, configurationService, configurationServiceProperties);

        FrameworkUtil.getBundle(ConfigurationRestServiceTest.class).getBundleContext()
                .registerService(DeploymentAgentService.class, deploymentAgentService, configurationServiceProperties);

    }

    @SuppressWarnings("unchecked")
    public ConfigurationRestServiceTest(final Transport transport) throws InterruptedException, ExecutionException,
            TimeoutException, KuraException, InvalidSyntaxException, IOException {
        super(transport);
        this.cryptoService = WireTestUtil.trackService(CryptoService.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        Mockito.reset(configurationService);
        Mockito.doAnswer(i -> {
            Optional.of(i.getArgument(0, List.class));
            return (Void) null;
        }).when(configurationService).updateConfigurations(ArgumentMatchers.any());
        final Answer<?> configurationUpdateAnswer = i -> {
            this.receivedConfigsByPid.put(i.getArgument(0, String.class), i.getArgument(1, Map.class));
            return (Void) null;
        };
        Mockito.doAnswer(configurationUpdateAnswer).when(configurationService)
                .updateConfiguration(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.doAnswer(configurationUpdateAnswer).when(configurationService)
                .updateConfiguration(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    private void thenResponseElementIs(final JsonValue expected, final JsonProjection projection) {
        final JsonValue root = Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected body")));
        final JsonValue actual;

        try {
            actual = projection.apply(root);
        } catch (final Exception e) {
            fail("failed to apply " + projection + " to " + root);
            throw new IllegalStateException("unreachable");
        }

        assertEquals("after applying " + projection + " to " + root, expected, actual);
    }

    private void thenResponseElementExists(final JsonProjection projection) {
        final JsonValue root = Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected body")));

        try {
            assertNotNull("response element " + projection + " is null", projection.apply(root));
        } catch (final Exception e) {
            fail("failed to apply " + projection + " to " + root);
            throw new IllegalStateException("unreachable");
        }
    }

    private void givenMockGetSnapshotsReturnEmpty() throws KuraException {
        when(configurationService.getSnapshots()).thenReturn(new TreeSet<Long>());
    }

    private void givenMockGetSnapshotsReturnSome(int howManySnapshots) throws KuraException {
        Set<Long> snapshots = new TreeSet<>();
        for (int i = 0; i < howManySnapshots; i++) {
            snapshots.add((long) i + 10000);
        }
        when(configurationService.getSnapshots()).thenReturn(snapshots);
    }

    private void givenMockGetSnapshotsReturnException() throws KuraException {
        when(configurationService.getSnapshots()).thenThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR));
    }

    private void givenMockGetFactoryComponentPidsReturnEmpty() throws KuraException {
        when(configurationService.getFactoryComponentPids()).thenReturn(Collections.emptySet());
    }

    private void givenMockGetFactoryComponentPidsReturnSome(int howManyComponents) throws KuraException {
        Set<String> components = new HashSet<>();
        for (int i = 0; i < howManyComponents; i++) {
            components.add("pid" + i);
        }
        when(configurationService.getFactoryComponentPids()).thenReturn(components);
    }

    private void givenMockGetConfigurableComponentPidsReturnEmpty() throws KuraException {
        when(configurationService.getConfigurableComponentPids()).thenReturn(new HashSet<String>());
    }

    private void givenMockGetConfigurableComponentPidsReturnSome(int howManyComponents) throws KuraException {
        Set<String> components = new HashSet<>();
        for (int i = 0; i < howManyComponents; i++) {
            components.add("pid" + i);
        }
        when(configurationService.getConfigurableComponentPids()).thenReturn(components);
    }

    private void givenMockGetComponentConfigurationsReturnException() throws KuraException {
        when(configurationService.getComponentConfigurations()).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetComponentConfigurationsReturnEmpty() throws KuraException {
        when(configurationService.getComponentConfigurations()).thenReturn(Collections.emptyList());
    }

    private void givenMockGetComponentConfigurationPidReturnException() throws KuraException {
        when(configurationService.getComponentConfiguration(anyString()))
                .thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetComponentConfigurationPidReturnEmpty() throws KuraException {
        when(configurationService.getComponentConfiguration(anyString())).thenReturn(null);
    }

    private void givenMockGetComponentConfigurationsReturnSome(int howManyConfigurations) throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < howManyConfigurations; i++) {
            final String generatedPid = "pid" + i;
            configs.add(new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return generatedPid;
                }

                @Override
                public OCD getDefinition() {
                    return null;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    return null;
                }
            });
        }
        when(configurationService.getComponentConfigurations()).thenReturn(configs);
    }

    private void givenMockGetComponentConfigurationPidReturnSome(int howManyConfigurations) throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < howManyConfigurations; i++) {
            final String generatedPid = "pid" + i;
            configs.add(new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return generatedPid;
                }

                @Override
                public OCD getDefinition() {
                    return null;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    return null;
                }
            });
        }
        when(configurationService.getComponentConfiguration("pid1")).thenReturn(configs.get(1));
        when(configurationService.getComponentConfiguration("pid3")).thenReturn(configs.get(3));
    }

    private void givenMockGetDefaultComponentConfigurationReturnException() throws KuraException {
        when(configurationService.getDefaultComponentConfiguration(ArgumentMatchers.any()))
                .thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetDefaultComponentConfigurationReturnOne(String pid) throws KuraException {
        ComponentConfiguration config = new ComponentConfiguration() {

            @Override
            public String getPid() {
                return pid;
            }

            @Override
            public OCD getDefinition() {
                return new OCD() {

                    @Override
                    public List<AD> getAD() {
                        return null;
                    }

                    @Override
                    public List<Icon> getIcon() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return pid;
                    }

                    @Override
                    public String getDescription() {
                        return null;
                    }

                    @Override
                    public String getId() {
                        return pid;
                    }
                };
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return null;
            }
        };

        when(configurationService.getDefaultComponentConfiguration(ArgumentMatchers.any())).thenReturn(config);
    }

    private void givenMockGetSnapshotReturnException() throws KuraException {
        when(configurationService.getSnapshot(12345)).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetSnapshotReturnSome(long snapshotId, int howManyConfigurations) throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < howManyConfigurations; i++) {
            final String generatedPid = "pid" + i;
            configs.add(new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return generatedPid;
                }

                @Override
                public OCD getDefinition() {
                    return null;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    return null;
                }
            });
        }
        when(configurationService.getSnapshot(snapshotId)).thenReturn(configs);
    }

    private void givenMockSnapshotReturnException() throws KuraException {
        when(configurationService.snapshot()).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockSnapshotReturnOne(long snapshotId) throws KuraException {
        when(configurationService.snapshot()).thenReturn(snapshotId);
    }

    private void givenMockRollbackReturnException() throws KuraException {
        when(configurationService.rollback()).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockRollbackReturnException(long snapshotId) throws KuraException {
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(configurationService).rollback(snapshotId);
    }

    private void givenMockRollbackReturnOne(long snapshotId) throws KuraException {
        when(configurationService.rollback()).thenReturn(snapshotId);
    }

    private void givenMockRollbackReturnNothing(long snapshotId) throws KuraException {
        doNothing().when(configurationService).rollback(snapshotId);
    }

    private void givenConfigurations(final ComponentConfiguration... configurations) throws KuraException {
        final Map<String, ComponentConfiguration> byPid = Arrays.stream(configurations)
                .collect(Collectors.toMap(c -> c.getPid(), c -> c));

        Mockito.when(configurationService.getComponentConfigurations())
                .thenReturn(byPid.values().stream().collect(Collectors.toList()));

        Mockito.when(configurationService.getComponentConfiguration(ArgumentMatchers.any())).thenAnswer(i -> {
            final String pid = i.getArgument(0, String.class);
            return byPid.get(pid);
        });

        Mockito.when(configurationService.getDefaultComponentConfiguration(ArgumentMatchers.any())).thenAnswer(i -> {
            final String pid = i.getArgument(0, String.class);
            return byPid.get(pid);
        });
    }

    private void givenATestConfigurationPropertyWithAdTypeAndValue(final Scalar type, final Object value)
            throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("testProp", type) //
                                        .build()) //
                                .build()) //
                .withConfigurationProperties(singletonMap("testProp", value)).build());
    }

    private void thenTestPropertyTypeIs(final JsonValue type) {
        thenResponseElementIs(type,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("type"));
        thenResponseElementIs(type,
                self().field("configs").arrayItem(0).field("properties").field("testProp").field("type"));
    }

    private void thenTestPropertyIsMissing() {
        thenResponseElementIs(null, self().field("configs").arrayItem(0).field("properties").field("testProp"));
    }

    private void thenTestPropertyValueIs(final JsonValue value) {
        thenResponseElementIs(value,
                self().field("configs").arrayItem(0).field("properties").field("testProp").field("value"));
    }

    private void thenReceivedPropertiesForPidContains(final String pid, final String expectedKey,
            final Object expectedValue) {
        assertEquals(expectedValue, this.receivedConfigsByPid.get(pid).get(expectedKey));
    }

    private void thenReceivedPropertiesForPidContainsArray(final String pid, final String expectedKey,
            final Object[] expectedValue) {
        assertArrayEquals(expectedValue, (Object[]) this.receivedConfigsByPid.get(pid).get(expectedKey));
    }

    private void thenReceivedPropertiesForPidContainsPassword(final String pid, final String expectedKey,
            final String expectedValue) {
        assertEquals(expectedValue,
                new String(((Password) this.receivedConfigsByPid.get(pid).get(expectedKey)).getPassword()));
    }

    private void thenReceivedPropertiesForPidContainsPasswords(final String pid, final String expectedKey,
            final String... expectedValues) {
        final Password[] passwords = (Password[]) this.receivedConfigsByPid.get(pid).get(expectedKey);

        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals(expectedValues[i], new String(passwords[i].getPassword()));
        }

    }
}
