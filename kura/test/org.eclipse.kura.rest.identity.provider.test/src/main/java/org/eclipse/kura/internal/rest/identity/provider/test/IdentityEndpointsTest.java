/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.rest.identity.provider.test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.internal.rest.identity.provider.IdentityRestService;
import org.eclipse.kura.internal.rest.identity.provider.IdentityService;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserDTO;
import org.eclipse.kura.util.validation.ValidatorOptions;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.google.gson.Gson;

@SuppressWarnings("restriction")
@RunWith(Parameterized.class)
public class IdentityEndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "IDN-V1";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String MQTT_METHOD_SPEC_DEL = "DEL";
    private static final String METHOD_SPEC_PUT = "PUT";
    private static final String REST_APP_ID = "identity/v1";

    private static IdentityService identityServiceMock = mock(IdentityService.class);

    private static UserDTO user;

    private Gson gson = new Gson();

    private static final String EXPECTED_GET_USER_CONFIG_RESPONSE = new Scanner(
            IdentityEndpointsTest.class.getResourceAsStream("/getUserConfigResponse.json"), "UTF-8").useDelimiter("\\A")
                    .next().replace(" ", "");

    private static final String EXPECTED_GET_USER_RESPONSE = new Scanner(
            IdentityEndpointsTest.class.getResourceAsStream("/getUserResponse.json"), "UTF-8").useDelimiter("\\A")
                    .next().replace(" ", "");

    private static final String EXPECTED_GET_PASSWORD_REQUIREMENTS_RESPONSE = new Scanner(
            IdentityEndpointsTest.class.getResourceAsStream("/getPasswordRequirementsResponse.json"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");

    private static final String EXPECTED_NON_EXISTING_USER_RESPONSE = new Scanner(
            IdentityEndpointsTest.class.getResourceAsStream("/getNonExistingUserResponse.json"), "UTF-8")
                    .useDelimiter("\\A").next();

    private static Set<UserDTO> userConfigs;

    private static ServiceRegistration<IdentityService> identityServiceRegistration;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Transport> transports() {
        return Arrays.asList(new MqttTransport(MQTT_APP_ID), new RestTransport(REST_APP_ID));
    }

    public IdentityEndpointsTest(Transport transport) {
        super(transport);
    }

    @Test
    public void shouldInvokeCreateUserSuccessfully() throws KuraException {
        givenUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities", gson.toJson(user));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldInvokeUpdateUserSuccessfully() throws KuraException {
        givenUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_PUT), "/identities", gson.toJson(user));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldInvokeDeleteUserSuccessfully() throws KuraException {
        givenUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/identities",
                gson.toJson(user));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldReturnUserSuccessfully() throws KuraException {
        givenUser(new UserDTO("testuser3", Collections.emptySet(), true, false));

        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities/byName", gson.toJson(user));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_USER_RESPONSE);
    }

    @Test
    public void shouldReturnUserConfig() throws KuraException {
        givenUserConfigs(new UserDTO("testuser2", //
                new HashSet<String>(Arrays.asList("perm1", "perm2")), //
                false, //
                true));

        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/identities");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_USER_CONFIG_RESPONSE);
    }

    @Test
    public void shouldReturnDefinedPermissions() throws KuraException {
        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/definedPermissions");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"permissions\": [\"perm1\",\"perm2\"]}");
    }

    @Test
    public void shouldReturnPasswordRequirements() throws KuraException {
        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/passwordRequirements");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_PASSWORD_REQUIREMENTS_RESPONSE);
    }

    @Test
    public void shouldReturnNonExistingUserDeleteResponse() throws KuraException {
        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/identities",
                gson.toJson(new UserDTO("nonExistingUser", null, false, false)));

        thenResponseCodeIs(404);
        thenResponseBodyEqualsJson(EXPECTED_NON_EXISTING_USER_RESPONSE);
    }

    @Test
    public void shouldReturnNonExistingUserPostResponse() throws KuraException {
        givenIdentityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities/byName",
                gson.toJson(new UserDTO("nonExistingUser", null, false, false)));

        thenResponseCodeIs(404);
        thenResponseBodyEqualsJson(EXPECTED_NON_EXISTING_USER_RESPONSE);
    }

    private void givenUser(UserDTO userParam) {
        user = userParam;
    }

    private void givenUserConfigs(UserDTO... userConfigurations) {
        userConfigs = new HashSet<>(Arrays.asList(userConfigurations));
    }

    private static void givenIdentityService() throws KuraException {
        reset(identityServiceMock);

        when(identityServiceMock.getDefinedPermissions())
                .thenReturn(new HashSet<String>(Arrays.asList("perm1", "perm2")));

        when(identityServiceMock.getUserConfig()).thenReturn(userConfigs);

        if (user != null) {
            when(identityServiceMock.getUser("testuser3"))
                    .thenReturn(new UserDTO("testuser3", Collections.emptySet(), true, false));

            when(identityServiceMock.getUser("testuser"))
                    .thenReturn(new UserDTO("testuser", Collections.emptySet(), true, false));

        }

        when(identityServiceMock.getUser("nonExistingUser"))
                .thenThrow(new KuraException(KuraErrorCode.NOT_FOUND, "Identity does not exist"));

        doThrow(new KuraException(KuraErrorCode.NOT_FOUND, "Identity does not exist")).when(identityServiceMock)
                .deleteUser("nonExistingUser");

        when(identityServiceMock.getValidatorOptions()).thenReturn(new ValidatorOptions(8, false, false, false));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        createIdentityServiceMock();
        registerIdentityServiceMock();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        unregisterIdentityServiceMock();
    }

    private static void createIdentityServiceMock() throws KuraException {
        givenIdentityService();

        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "identityServiceMock");
        identityServiceRegistration = FrameworkUtil.getBundle(IdentityEndpointsTest.class).getBundleContext()
                .registerService(IdentityService.class, identityServiceMock, configurationServiceProperties);
    }

    private static void registerIdentityServiceMock() throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("IdentityService.target", "(kura.service.pid=identityServiceMock)");

        final ConfigurationAdmin configurationAdmin = WireTestUtil
                .trackService(ConfigurationAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        final Configuration config = configurationAdmin.getConfiguration(IdentityRestService.class.getName(), "?");
        config.update(properties);
    }

    private static void unregisterIdentityServiceMock() {
        identityServiceRegistration.unregister();

    }

}
