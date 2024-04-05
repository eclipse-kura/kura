/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
import org.eclipse.kura.internal.rest.identity.provider.IdentityRestServiceV1;
import org.eclipse.kura.internal.rest.identity.provider.LegacyIdentityService;
import org.junit.Test;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class IdentityRestServiceV1DependenciesTest {

    private static final String MQTT_APP_ID = "IDN-V1";
    private static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final IdentityRestServiceV1 service = new IdentityRestServiceV1();

    private UserAdmin userAdminMock;
    private CryptoService cryptoServiceMock;
    private ConfigurationService configurationServiceMock;

    private LegacyIdentityService legacyIdentityServiceMock;
    private RequestHandlerRegistry requestHandlerRegistryMock;
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void shouldCreateRoleOnUserAdminBinding() {
        givenUserAdminMock();

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
    public void shouldActivateRestIdentityService() throws KuraException {
        givenUserAdminMock();
        givenCryptoServiceMock();
        givenMockConfigurationService();
        givenLegacyIdentityServiceMock();

        whenActivateWithDependencies();

        thenNoExceptionOccurred();
    }

    /*
     * Given
     */

    private void givenCryptoServiceMock() {
        this.cryptoServiceMock = mock(CryptoService.class);
    }

    private void givenMockConfigurationService() {
        this.configurationServiceMock = mock(ConfigurationService.class);
    }

    private void givenUserAdminMock() {
        this.userAdminMock = mock(UserAdmin.class);
    }

    private void givenMockRequestHandlerRegistry() {
        this.requestHandlerRegistryMock = mock(RequestHandlerRegistry.class);
    }

    private void givenBoundRequestHandlerRegistry() {
        bindRequestHandlerRegistry();
    }

    private void givenFailingMockRequestHandlerRegistry() throws KuraException {
        this.requestHandlerRegistryMock = mock(RequestHandlerRegistry.class);
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistryMock)
                .registerRequestHandler(any(), any());
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistryMock).unregister(any());
    }

    private void givenLegacyIdentityServiceMock() {
        this.legacyIdentityServiceMock = mock(LegacyIdentityService.class);
    }

    /*
     * When
     */

    private void whenBindUserAdmin() {
        this.service.bindUserAdmin(this.userAdminMock);
    }

    private void whenBindRequestHandlerRegistry() {
        bindRequestHandlerRegistry();
    }

    private void whenUnbindRequestHandlerRegistry() {
        try {
            this.service.unbindRequestHandlerRegistry(this.requestHandlerRegistryMock);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenActivateWithDependencies() {
        try {
            this.service.bindConfigurationService(this.configurationServiceMock);
            this.service.bindCryptoService(this.cryptoServiceMock);
            this.service.bindLegacyIdentityService(this.legacyIdentityServiceMock);
            this.service.bindRequestHandlerRegistry(this.requestHandlerRegistryMock);
            this.service.bindUserAdmin(this.userAdminMock);

            this.service.activate();
        } catch (Exception e) {
            this.occurredException = e;
        }

    }

    /*
     * Then
     */

    private void thenRoleIsCreated(String expectedKuraPermission, int expectedRole) {
        verify(this.userAdminMock, times(1)).createRole(expectedKuraPermission, expectedRole);
    }

    private void thenRequestHandlerIsRegistered(String expectedMqttAppId) throws KuraException {
        verify(this.requestHandlerRegistryMock, times(1)).registerRequestHandler(eq(expectedMqttAppId),
                any(RequestHandler.class));
    }

    private void thenRequestHandlerIsUnregistered(String expectedMqttAppId) throws KuraException {
        verify(this.requestHandlerRegistryMock, times(1)).unregister(expectedMqttAppId);
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
            this.service.bindRequestHandlerRegistry(this.requestHandlerRegistryMock);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

}
