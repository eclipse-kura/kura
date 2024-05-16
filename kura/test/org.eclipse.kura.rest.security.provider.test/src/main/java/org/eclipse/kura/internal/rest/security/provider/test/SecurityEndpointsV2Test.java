/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
import org.eclipse.kura.internal.rest.security.provider.SecurityRestServiceV2;
import org.eclipse.kura.security.SecurityService;
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

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class SecurityEndpointsV2Test extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "SEC-V2";

    private static final String METHOD_SPEC_POST = "POST";
    private static final String REST_APP_ID = "security/v2";

    private static final SecurityService securityServiceMock = mock(SecurityService.class);
    private static ServiceRegistration<SecurityService> securityServiceRegistration;
    public static final String ADMIN_ADMIN = "admin:admin";
    public static final String SECURITY_POLICY_APPLY_DEFAULT_PRODUCTION = "/security-policy/apply-default-production";
    public static final String SECURITY_POLICY_APPLY = "/security-policy/apply";
    private static String securityPolicy;

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(REST_APP_ID), new MqttTransport(MQTT_APP_ID));
    }

    public SecurityEndpointsV2Test(Transport transport) {
        super(transport);
    }

    @Test
    public void shouldCopyDefaultSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY_DEFAULT_PRODUCTION);

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
        thenApplyDefaultProductionSecurityPolicyIsCalled();
    }

    @Test
    public void shouldReloadFingerprintsWhenDefaultSecurityPolicyIsApplied() throws IOException, KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY_DEFAULT_PRODUCTION);

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
        thenFingerprintsAreReloaded();
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnApplyDefaultProductionSecurityPolicy() throws KuraException {
        givenFailingSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY_DEFAULT_PRODUCTION);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnApplyNullSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY, null);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson("{\"message\":\"Security Policy cannot be null or empty\"}");
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnApplyEmptySecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY, "");

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson("{\"message\":\"Security Policy cannot be null or empty\"}");
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnApplyTooBigSecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);
        givenTooLargeSecurityPolicy();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY, securityPolicy);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson("{\"message\":\"Security policy too large\"}");
    }

    @Test
    public void shouldApplySecurityPolicy() throws KuraException {
        givenSecurityService();
        givenRestBasicCredentials(ADMIN_ADMIN);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), SECURITY_POLICY_APPLY,
                "This is a very cool security policy!");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
        thenApplySecurityPolicyIsCalled("This is a very cool security policy!");
    }

    private static void givenSecurityService() {
        reset(securityServiceMock);
    }

    private static void givenFailingSecurityService() throws KuraException {
        reset(securityServiceMock);

        doThrow(RuntimeException.class).when(securityServiceMock).applyDefaultProductionSecurityPolicy();
        doThrow(RuntimeException.class).when(securityServiceMock).reloadCommandLineFingerprint();
        doThrow(RuntimeException.class).when(securityServiceMock).reloadSecurityPolicyFingerprint();
    }

    /*
     * Utilities
     */

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
        securityPolicy = sb.toString();
    }

    private void thenApplyDefaultProductionSecurityPolicyIsCalled() throws KuraException {
        verify(securityServiceMock, times(1)).applyDefaultProductionSecurityPolicy();
    }

    private void thenFingerprintsAreReloaded() throws KuraException {
        verify(securityServiceMock, times(1)).reloadSecurityPolicyFingerprint();
        verify(securityServiceMock, times(1)).reloadCommandLineFingerprint();
    }

    private void thenApplySecurityPolicyIsCalled(final String securityPolicy) throws KuraException {
        verify(securityServiceMock, times(1)).applySecurityPolicy(securityPolicy);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        createSecurityServiceMock();
        registerSecurityServiceMock();
        configureSecurityServiceMock();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        unregisterSecurityServiceMock();
    }

    private static void createSecurityServiceMock() {
        givenSecurityService();
    }

    private static void registerSecurityServiceMock() {
        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "mockSecurityService");
        securityServiceRegistration = FrameworkUtil.getBundle(SecurityEndpointsV2Test.class).getBundleContext()
                .registerService(SecurityService.class, securityServiceMock, configurationServiceProperties);
    }

    private static void configureSecurityServiceMock() throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("SecurityService.target", "(kura.service.pid=mockSecurityService)");

        final ConfigurationAdmin configurationAdmin = WireTestUtil.trackService(ConfigurationAdmin.class,
                Optional.empty()).get(30, TimeUnit.SECONDS);
        final Configuration config = configurationAdmin.getConfiguration(SecurityRestServiceV2.class.getName(), "?");
        config.update(properties);
    }

    private static void unregisterSecurityServiceMock() throws Exception {
        securityServiceRegistration.unregister();
    }

}
