/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.network.configuration.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.rest.network.configuration.provider.test.responses.MockComponentConfiguration;
import org.eclipse.kura.rest.network.configuration.provider.test.responses.RestNetworkConfigurationJson;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

@Ignore
public class NetworkConfigurationRestServiceTest extends AbstractRequestHandlerTest {

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_PUT = "PUT";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String REST_APP_ID = "networkConfiguration/v1";

    private static final String NETWORK_CONF_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
    private static final String IP4_FIREWALL_CONF_SERVICE_PID = "org.eclipse.kura.net.admin.FirewallConfigurationService";
    private static final String IP6_FIREWALL_CONF_SERVICE_PID = "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6";

    private final Map<String, Map<String, Object>> receivedConfigsByPid = new HashMap<>();

    private static ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);

    public NetworkConfigurationRestServiceTest() {
        super(new RestTransport(REST_APP_ID));
    }

    @Test
    public void shouldReturnNotFoundIfNoServiceIsRegistered() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/nothing");

        thenResponseCodeIs(404);
    }

    @Test
    public void shouldReturnUnauthorizedStatusWhenNoRestPermissionIsGiven() {
        givenIdentity("noAuthUser", Optional.of("pass1"), Collections.emptyList());
        givenBasicCredentials(Optional.empty());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/configurableComponents");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldReturnListOfNetworkConfigurationPids() {
        givenMockGetNetworkConfigurationPids();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/configurableComponents");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"pids\":[\"org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6\",\"org.eclipse.kura.net.admin.NetworkConfigurationService\",\"org.eclipse.kura.net.admin.FirewallConfigurationService\"]}");
    }

    @Test
    public void shouldReturnMockedConfigurationList() throws KuraException {
        givenMockGetNetworkConfigurationsList();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.ALL_CONFIGURATIONS_RESPONSE);
    }

    @Test
    public void shouldReturnAMockedNetworkConfiguration() throws KuraException {
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));
        givenMockGetNetworkConfiguration();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/configurableComponents/configurations/byPid",
                RestNetworkConfigurationJson.FIREWALL_IP6_BYPID_REQUEST);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.SINGLE_CONFIG_RESPONSE);
    }

    @Test
    public void shouldReturnAMockedDefaultNetworkConfiguration() throws KuraException {
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));
        givenMockGetDefaultNetworkConfiguration();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST),
                "/configurableComponents/configurations/byPid/_default",
                RestNetworkConfigurationJson.FIREWALL_IP6_BYPID_REQUEST);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.SINGLE_CONFIG_RESPONSE);
    }

    @Test
    public void shouldUpdateWithoutErrors() throws KuraException {
        givenMockUpdateConfiguration();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_PUT), "/configurableComponents/configurations/_update",
                RestNetworkConfigurationJson.FIREWALL_IP6_UPDATE_REQUEST);

        thenRequestSucceeds();
        thenValueIsUpdated(IP6_FIREWALL_CONF_SERVICE_PID, "firewall.ipv6.open.ports",
                "1234,tcp,0:0:0:0:0:0:0:0/0,,,,,#");
    }

    @Test
    public void shouldReturnEmptyArray() throws KuraException {
        givenMockUpdateConfiguration();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/factoryComponents");

        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.EMPTY_PIDS_RESPONSE);
    }

    @Test
    public void shouldReturnNothing() throws KuraException {
        givenMockUpdateConfiguration();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryComponents",
                RestNetworkConfigurationJson.EMPTY_CONFIGS_REQUEST);

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldReturnInvalidPidResponse() throws KuraException {
        givenMockUpdateConfiguration();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, "DEL"), "/factoryComponents/byPid",
                RestNetworkConfigurationJson.INVALID_PID_DELETE_REQUEST);

        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.INVALID_PID_DELETE_RESPONSE);
    }

    @Test
    public void shouldReturnEmptyConfigsOnGet() throws KuraException {
        givenMockUpdateConfiguration();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/factoryComponents/ocd");

        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.EMPTY_CONFIGS_RESPONSE);
    }

    @Test
    public void shouldReturnEmptyConfigsOnPost() throws KuraException {
        givenMockUpdateConfiguration();
        givenIdentity("admin", Optional.of("password"), Collections.emptyList());
        givenBasicCredentials(Optional.of("admin:password"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryComponents/ocd/byFactoryPid",
                RestNetworkConfigurationJson.EMPTY_PIDS_REQUEST);

        thenResponseBodyEqualsJson(RestNetworkConfigurationJson.EMPTY_CONFIGS_RESPONSE);
    }

    /*
     * Given
     */

    private void givenMockGetNetworkConfigurationPids() {
        Set<String> pids = new HashSet<>();
        pids.add(IP6_FIREWALL_CONF_SERVICE_PID);
        pids.add(NETWORK_CONF_SERVICE_PID);
        pids.add(IP4_FIREWALL_CONF_SERVICE_PID);
        when(configurationService.getConfigurableComponentPids()).thenReturn(pids);
    }

    private void givenMockGetNetworkConfigurationsList() throws KuraException {
        MockComponentConfiguration mockNetworkConfig = new MockComponentConfiguration(0);
        MockComponentConfiguration mockIp4Config = new MockComponentConfiguration(1);
        MockComponentConfiguration mockIp6Config = new MockComponentConfiguration(2);
        when(configurationService.getComponentConfiguration(NETWORK_CONF_SERVICE_PID))
                .thenReturn(mockNetworkConfig.getComponentConfiguration());
        when(configurationService.getComponentConfiguration(IP4_FIREWALL_CONF_SERVICE_PID))
                .thenReturn(mockIp4Config.getComponentConfiguration());
        when(configurationService.getComponentConfiguration(IP6_FIREWALL_CONF_SERVICE_PID))
                .thenReturn(mockIp6Config.getComponentConfiguration());
    }

    private void givenMockGetNetworkConfiguration() throws KuraException {
        MockComponentConfiguration mockIp6Config = new MockComponentConfiguration(0);
        when(configurationService.getComponentConfiguration(IP6_FIREWALL_CONF_SERVICE_PID))
                .thenReturn(mockIp6Config.getComponentConfiguration());
    }

    private void givenMockGetDefaultNetworkConfiguration() throws KuraException {
        MockComponentConfiguration mockIp6Config = new MockComponentConfiguration(0);
        when(configurationService.getDefaultComponentConfiguration(IP6_FIREWALL_CONF_SERVICE_PID))
                .thenReturn(mockIp6Config.getComponentConfiguration());
    }

    @SuppressWarnings("unchecked")
    private void givenMockUpdateConfiguration() throws KuraException {
        final Answer<?> configurationUpdateAnswer = i -> {
            this.receivedConfigsByPid.put(i.getArgument(0, String.class), i.getArgument(1, Map.class));
            return (Void) null;
        };
        Mockito.doAnswer(configurationUpdateAnswer).when(configurationService)
                .updateConfiguration(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    /*
     * Then
     */

    private void thenValueIsUpdated(final String pid, final String expectedKey, final Object expectedValue) {
        assertEquals(expectedValue, this.receivedConfigsByPid.get(pid).get(expectedKey));
    }

    /*
     * Utils
     */

    @BeforeClass
    public static void setUp() throws Exception {
        WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        final ConfigurationAdmin configurationAdmin = WireTestUtil
                .trackService(ConfigurationAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        final Configuration config = configurationAdmin.getConfiguration(
                "org.eclipse.kura.internal.rest.network.configuration.NetworkConfigurationRestService", "?");
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("ConfigurationService.target", "(kura.service.pid=mockConfigurationService)");
        config.update(properties);
        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "mockConfigurationService");
        FrameworkUtil.getBundle(NetworkConfigurationRestServiceTest.class).getBundleContext()
                .registerService(ConfigurationService.class, configurationService, configurationServiceProperties);
    }

    @SuppressWarnings("unchecked")
    private void givenIdentity(final String username, final Optional<String> password, final List<String> roles) {
        final UserAdmin userAdmin;
        try {
            userAdmin = ServiceUtil.trackService(UserAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("failed to track UserAdmin");
            return;
        }
        final User user = getRoleOrCreateOne(userAdmin, "kura.user." + username, User.class);
        if (password.isPresent()) {
            try {
                final CryptoService cryptoService = ServiceUtil.trackService(CryptoService.class, Optional.empty())
                        .get(30, TimeUnit.SECONDS);

                user.getCredentials().put("kura.password", cryptoService.sha256Hash(password.get()));
            } catch (Exception e) {
                fail("failed to compute password hash");
            }
        }
        for (final String role : roles) {
            getRoleOrCreateOne(userAdmin, "kura.permission." + role, Group.class).addMember(user);
        }
    }

    public void givenBasicCredentials(final Optional<String> basicCredentials) {
        ((RestTransport) this.transport).setBasicCredentials(basicCredentials);
    }

    @SuppressWarnings("unchecked")
    private <S extends Role> S getRoleOrCreateOne(final UserAdmin userAdmin, final String name, final Class<S> classz) {
        final Role role = userAdmin.getRole(name);
        if (classz.isInstance(role)) {
            return (S) role;
        }
        final int type;
        if (classz == User.class) {
            type = Role.USER;
        } else if (classz == Group.class) {
            type = Role.GROUP;
        } else {
            fail("Unsupported role type");
            return null;
        }
        return (S) userAdmin.createRole(name, type);
    }
}
