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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
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

@RunWith(Parameterized.class)
public class SecurityEndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "SEC-V1";

    private static final String EXPECTED_DEBUG_ENABLE_TRUE_RESPONSE = "{\"enabled\":true}";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String REST_APP_ID = "security/v1";

    private static SecurityService securityServiceMock = mock(SecurityService.class);

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

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/security-policy-fingerprint/reload");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldInvokeReloadCommandLineFingerprintSuccessfully() {
        givenSecurityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/command-line-fingerprint/reload");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldReturnExpectedDebugStatus() {
        givenDebugEnabledStatus(true);
        givenSecurityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/debug-enabled");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_DEBUG_ENABLE_TRUE_RESPONSE);
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnReloadSecurityPolicyFingerprint() throws KuraException {
        givenFailingSecurityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/security-policy-fingerprint/reload");

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnReloadCommandLineFingerprint() throws KuraException {
        givenFailingSecurityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/command-line-fingerprint/reload");

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnGetDebugStatus() throws KuraException {
        givenFailingSecurityService();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/debug-enabled");

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private void givenDebugEnabledStatus(boolean debugStatus) {
        debugEnabled = debugStatus;
    }

    private static void givenSecurityService() {
        reset(securityServiceMock);

        when(securityServiceMock.isDebugEnabled()).thenReturn(debugEnabled);
    }

    private static void givenFailingSecurityService() throws KuraException {
        reset(securityServiceMock);

        when(securityServiceMock.isDebugEnabled()).thenThrow(RuntimeException.class);
        doThrow(RuntimeException.class).when(securityServiceMock).reloadCommandLineFingerprint();
        doThrow(RuntimeException.class).when(securityServiceMock).reloadSecurityPolicyFingerprint();
    }

    /*
     * Utilities
     */

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
        FrameworkUtil.getBundle(SecurityEndpointsTest.class).getBundleContext().registerService(SecurityService.class,
                securityServiceMock, configurationServiceProperties);
    }

    private static void registerSecurityServiceMock() throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("SecurityService.target", "(kura.service.pid=mockSecurityService)");

        final ConfigurationAdmin configurationAdmin = WireTestUtil
                .trackService(ConfigurationAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        final Configuration config = configurationAdmin.getConfiguration(SecurityRestService.class.getName(), "?");
        config.update(properties);
    }

}
