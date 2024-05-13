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
package org.eclipse.kura.internal.rest.security.provider.test;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.security.provider.SecurityRestService;
import org.eclipse.kura.security.SecurityService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class SecurityEndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "SEC-V1";

    private static final String EXPECTED_DEBUG_ENABLE_TRUE_RESPONSE = "{\"enabled\":true}";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String REST_APP_ID = "security/v1";

    private static final SecurityService securityServiceMock = mock(SecurityService.class);
    public static final String DEBUG_ENABLED = "/debug-enabled";
    public static final String ADMIN_ADMIN = "admin:admin";
    public static final String SECURITY_POLICY_LOAD_DEFAULT_PRODUCTION = "/security-policy/load-default-production";
    public static final String SECURITY_POLICY_LOAD = "/security-policy/load";
    private static String tooLargeSecurityPolicy;

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(REST_APP_ID), new MqttTransport(MQTT_APP_ID));
    }

    private static boolean debugEnabled;

    public SecurityEndpointsTest(Transport transport) {
        super(transport);
    }

    @Test
    public void shouldInvokeReloadSecurityPolicyFingerprintSuccessfully() {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/security-policy-fingerprint/reload");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldInvokeReloadCommandLineFingerprintSuccessfully() {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/command-line-fingerprint/reload");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldReturnExpectedDebugStatus() {
        givenDebugEnabledStatus(true);
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), DEBUG_ENABLED);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_DEBUG_ENABLE_TRUE_RESPONSE);
    }

    @Test
    public void shouldNotReturnDebugStatusOverRestIfNotLoggedIn() {
        givenDebugEnabledStatus(true);
        givenSecurityService();
        givenNoRestBasicCredentials();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), DEBUG_ENABLED);

        thenRestResponseCodeIs(401);
        thenMqttResponseCodeIs(200);
    }

    @Test
    public void shouldReturnDebugStatusEvenIfIdentityHasNoPermissions() {
        givenDebugEnabledStatus(true);
        givenSecurityService();
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList(), false);
        givenRestBasicCredentials("foo:bar");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), DEBUG_ENABLED);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_DEBUG_ENABLE_TRUE_RESPONSE);
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnReloadSecurityPolicyFingerprint() throws KuraException {
        givenFailingSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/security-policy-fingerprint/reload");

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnReloadCommandLineFingerprint() throws KuraException {
        givenFailingSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/command-line-fingerprint/reload");

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnGetDebugStatus() throws KuraException {
        givenFailingSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), DEBUG_ENABLED);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldCopyDefaultSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD_DEFAULT_PRODUCTION);

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
        thenLoadDefaultProductionSecurityPolicyIsCalled();
    }

    @Test
    public void shouldReloadFingerprintsWhenDefaultSecurityPolicyIsLoaded() throws IOException, KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD_DEFAULT_PRODUCTION);

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
        thenFingerprintsAreReloaded();
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnLoadDefaultProductionSecurityPolicy() throws KuraException {
        givenFailingSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD_DEFAULT_PRODUCTION);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnLoadNullSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD, null);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        thenResponseBodyEqualsJson("{\"message\":\"Security Policy cannot be null or empty\"}");
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnLoadEmptySecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD, "");

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        thenResponseBodyEqualsJson("{\"message\":\"Security Policy cannot be null or empty\"}");
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnLoadTooBigSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);
        givenTooLargeSecurityPolicy();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD, tooLargeSecurityPolicy);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        thenResponseBodyEqualsJson("{\"message\":\"Security policy too large\"}");
    }

    @Test
    public void shouldLoadSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_LOAD,
                "This is a very cool security policy!");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
        thenLoadSecurityPolicyIsCalled("This is a very cool security policy!");
    }

    private static void givenDebugEnabledStatus(boolean debugStatus) {
        debugEnabled = debugStatus;
    }

    private static void givenSecurityService() {
        reset(securityServiceMock);

        when(securityServiceMock.isDebugEnabled()).thenReturn(debugEnabled);
    }

    private static void givenFailingSecurityService() throws KuraException {
        reset(securityServiceMock);

        when(securityServiceMock.isDebugEnabled()).thenThrow(RuntimeException.class);
        doThrow(RuntimeException.class).when(securityServiceMock).loadDefaultProductionSecurityPolicy();
        doThrow(RuntimeException.class).when(securityServiceMock).reloadCommandLineFingerprint();
        doThrow(RuntimeException.class).when(securityServiceMock).reloadSecurityPolicyFingerprint();
    }

    /*
     * Utilities
     */

    @SuppressWarnings("unchecked")
    private void givenIdentity(final String username, final Optional<String> password, final List<String> roles,
            final boolean needsPasswordChange) {
        final UserAdmin userAdmin;

        try {
            userAdmin = ServiceUtil.trackService(UserAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("failed to track UserAdmin");
            return;
        }

        final User user = getOrCreateRole(userAdmin, "kura.user." + username, User.class);

        if (password.isPresent()) {
            try {
                final CryptoService cryptoService = ServiceUtil.trackService(CryptoService.class, Optional.empty())
                        .get(30, TimeUnit.SECONDS);

                user.getCredentials().put("kura.password", cryptoService.sha256Hash(password.get()));

            } catch (Exception e) {
                fail("failed to compute password hash");
            }
        }

        if (needsPasswordChange) {
            user.getProperties().put("kura.need.password.change", "true");
        } else {
            user.getProperties().remove("kura.need.password.change");
        }

        for (final String role : roles) {
            getOrCreateRole(userAdmin, "kura.permission." + role, Group.class).addMember(user);
        }
    }

    private void givenNoRestBasicCredentials() {
        if (this.transport instanceof RestTransport) {
            ((RestTransport) this.transport).setBasicCredentials(Optional.empty());
        }
    }

    private void givenRestBasicCredentials(final String credentials) {
        if (this.transport instanceof RestTransport) {
            ((RestTransport) this.transport).setBasicCredentials(Optional.of(credentials));
        }
    }

    private static void givenTooLargeSecurityPolicy() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append("This is a very large security policy!");
        }
        tooLargeSecurityPolicy = sb.toString();
    }

    private void thenRestResponseCodeIs(final int expectedStatusCode) {
        if (this.transport instanceof RestTransport) {
            thenResponseCodeIs(expectedStatusCode);
        }
    }

    private void thenMqttResponseCodeIs(final int expectedStatusCode) {
        if (this.transport instanceof MqttTransport) {
            thenResponseCodeIs(expectedStatusCode);
        }
    }

    private void thenLoadDefaultProductionSecurityPolicyIsCalled() throws KuraException {
        verify(securityServiceMock, times(1)).loadDefaultProductionSecurityPolicy();
    }

    private void thenFingerprintsAreReloaded() throws KuraException {
        verify(securityServiceMock, times(1)).reloadSecurityPolicyFingerprint();
        verify(securityServiceMock, times(1)).reloadCommandLineFingerprint();
    }

    private void thenLoadSecurityPolicyIsCalled(final String securityPolicy) throws KuraException {
        verify(securityServiceMock, times(1)).loadSecurityPolicy(securityPolicy);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        createSecurityServiceMock();
        registerSecurityServiceMock();
    }

    private static void createSecurityServiceMock() {
        givenSecurityService();

        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "mockSecurityService");
        FrameworkUtil.getBundle(SecurityEndpointsTest.class).getBundleContext()
                .registerService(SecurityService.class, securityServiceMock, configurationServiceProperties);
    }

    private static void registerSecurityServiceMock() throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("SecurityService.target", "(kura.service.pid=mockSecurityService)");

        final ConfigurationAdmin configurationAdmin = WireTestUtil.trackService(ConfigurationAdmin.class,
                Optional.empty()).get(30, TimeUnit.SECONDS);
        final Configuration config = configurationAdmin.getConfiguration(SecurityRestService.class.getName(), "?");
        config.update(properties);
    }

    @SuppressWarnings("unchecked")
    private <T extends Role> T getOrCreateRole(final UserAdmin userAdmin, final String name, final Class<T> classz) {

        final Role existing = userAdmin.getRole(name);

        if (classz.isInstance(existing)) {
            return (T) existing;
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

        return (T) userAdmin.createRole(name, type);
    }

}
