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

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.identity.provider.IdentityRestService;
import org.junit.Test;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class IdentityRestServiceDependenciesTest {

    private static final String MQTT_APP_ID = "IDN-V1";
    private static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final IdentityRestService service = new IdentityRestService();
    private UserAdmin userAdmin;
    private CryptoService cryptoService;
    private ConfigurationService configurationService;

    private RequestHandlerRegistry requestHandlerRegistry;
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void shouldCreateRoleOnUserAdminBinding() {
        givenMockUserAdmin();

        whenBindUserAdmin();

        thenRoleIsCreated(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    @Test
    public void shouldRegisterRequestHandler() throws KuraException {
        givenMockRequestHandlerRegistry();

        whenBindRequestHandlerRegistry();

        thenRequestHandlerIsRegistered(MQTT_APP_ID);
    }

    @Test
    public void shouldUnregisterRequestHandler() throws KuraException {
        givenMockRequestHandlerRegistry();
        givenBoundRequestHandlerRegistry();

        whenUnbindRequestHandlerRegistry();

        thenRequestHandlerIsUnregistered(MQTT_APP_ID);
    }

    @Test
    public void shouldCatchExceptionsOnRequestHandlerBind() throws KuraException {
        givenFailingMockRequestHandlerRegistry();

        whenBindRequestHandlerRegistry();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldCatchExceptionsOnRequestHandlerUnbind() throws KuraException {
        givenFailingMockRequestHandlerRegistry();

        whenUnbindRequestHandlerRegistry();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldCreateIdentityService() throws KuraException {
        givenMockUserAdmin();
        givenMockCryptoService();
        givenMockConfigurationService();
        givenBoundCryptoService();
        givensBoundConfigurationService();
        givenBoundUserAdmin();

        whenActivate();

        thenNoExceptionOccurred();
    }

    /*
     * Given
     */

    private void givenMockCryptoService() {
        this.cryptoService = mock(CryptoService.class);
    }

    private void givenMockConfigurationService() {
        this.configurationService = mock(ConfigurationService.class);
    }

    private void givenMockUserAdmin() {
        this.userAdmin = mock(UserAdmin.class);
    }

    private void givenBoundCryptoService() {
        this.service.bindCryptoService(this.cryptoService);
    }

    private void givensBoundConfigurationService() {
        this.service.bindConfigurationService(this.configurationService);
    }

    private void givenBoundUserAdmin() {
        this.service.bindUserAdmin(this.userAdmin);
    }

    private void givenMockRequestHandlerRegistry() {
        this.requestHandlerRegistry = mock(RequestHandlerRegistry.class);
    }

    private void givenBoundRequestHandlerRegistry() {
        bindRequestHandlerRegistry();
    }

    private void givenFailingMockRequestHandlerRegistry() throws KuraException {
        this.requestHandlerRegistry = mock(RequestHandlerRegistry.class);
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistry)
                .registerRequestHandler(any(), any());
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistry).unregister(any());
    }

    /*
     * When
     */

    private void whenBindUserAdmin() {
        this.service.bindUserAdmin(this.userAdmin);
    }

    private void whenBindRequestHandlerRegistry() {
        bindRequestHandlerRegistry();
    }

    private void whenUnbindRequestHandlerRegistry() {
        try {
            this.service.unbindRequestHandlerRegistry(this.requestHandlerRegistry);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenActivate() {
        this.service.activate();
    }

    /*
     * Then
     */

    private void thenRoleIsCreated(String expectedKuraPermission, int expectedRole) {
        verify(this.userAdmin, times(1)).createRole(expectedKuraPermission, expectedRole);
    }

    private void thenRequestHandlerIsRegistered(String expectedMqttAppId) throws KuraException {
        verify(this.requestHandlerRegistry, times(1)).registerRequestHandler(eq(expectedMqttAppId),
                any(RequestHandler.class));
    }

    private void thenRequestHandlerIsUnregistered(String expectedMqttAppId) throws KuraException {
        verify(this.requestHandlerRegistry, times(1)).unregister(expectedMqttAppId);
    }

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private void bindRequestHandlerRegistry() {
        try {
            this.service.bindRequestHandlerRegistry(this.requestHandlerRegistry);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

}
