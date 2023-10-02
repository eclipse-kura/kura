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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

@RunWith(Parameterized.class)
public class IdentityEndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "ID-V1";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String REST_APP_ID = "identity/v1";

    private UserDTO user;

    private Gson gson = new Gson();

    private static final String EXPECTED_GET_USER_RESPONSE = new Scanner(
            IdentityEndpointsTest.class.getResourceAsStream("/getUserConfigResponse.json"), "UTF-8").useDelimiter("\\A")
                    .next().replace(" ", "");

    private String username;

    private Set<UserDTO> userConfigs;

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(REST_APP_ID), new MqttTransport(MQTT_APP_ID));
    }

    public IdentityEndpointsTest(Transport transport) {
        super(transport);
    }

    @Test
    public void shouldInvokeCreateUserSuccessfully() {
        givenUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/users", gson.toJson(this.user));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldInvokeDeleteUserSuccessfully() {

        givenUsername("testuser");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE), "/users/" + this.username);

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldReturnDefinedPermissions() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/defined-permissions");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "[\"kura.cloud.connection.admin\",\"kura.packages.admin\",\"kura.device\",\"kura.admin\",\"rest.keystores\",\"kura.network.admin\",\"kura.wires.admin\",\"rest.identity\"]");
    }

    @Test
    public void shouldReturnUserConfig() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/users/configs");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_USER_RESPONSE);
    }

    @Test
    public void shouldInvokeSetUserConfigSuccessfully() throws KuraException {
        givenUserConfigs(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"), new UserDTO(
                "testuser2", new HashSet<String>(Arrays.asList("perm1", "perm2")), false, true, "testpassw2"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/users/configs", gson.toJson(this.userConfigs));

        thenRequestSucceeds();
    }

    // @Test
    // public void shouldRethrowWebApplicationExceptionOnReloadSecurityPolicyFingerprint() throws KuraException {
    // givenFailingIdentityService();
    //
    // whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/security-policy-fingerprint/reload");
    //
    // thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    // }
    //
    // @Test
    // public void shouldRethrowWebApplicationExceptionOnReloadCommandLineFingerprint() throws KuraException {
    // givenFailingIdentityService();
    //
    // whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/command-line-fingerprint/reload");
    //
    // thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    // }
    //
    // @Test
    // public void shouldRethrowWebApplicationExceptionOnGetDebugStatus() throws KuraException {
    // givenFailingIdentityService();
    //
    // whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/debug-enabled");
    //
    // thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    // }

    private void givenUser(UserDTO user) {
        this.user = user;
    }

    private void givenUsername(String username) {
        this.username = username;
    }

    private void givenUserConfigs(UserDTO... userConfigs) {
        this.userConfigs = new HashSet<>(Arrays.asList(userConfigs));
    }

}
