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
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.internal.rest.identity.provider.IdentityRestServiceV2;
import org.junit.Test;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class IdentityRestServiceV2DependenciesTest {

    private static final String MQTT_APP_ID = "IDN-V2";
    private static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final IdentityRestServiceV2 service = new IdentityRestServiceV2();

    private UserAdmin userAdminMock;
    private IdentityService identityServiceMock;

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
        givenRequestHandlerRegistryMock();

        whenBindRequestHandlerRegistry();

        thenRequestHandlerIsRegistered(MQTT_APP_ID);
    }

    @Test
    public void shouldUnregisterRequestHandler() throws KuraException {
        givenRequestHandlerRegistryMock();
        givenBoundRequestHandlerRegistry();

        whenUnbindRequestHandlerRegistry();

        thenRequestHandlerIsUnregistered(MQTT_APP_ID);
    }

    @Test
    public void shouldCatchExceptionsOnRequestHandlerBind() throws KuraException {
        givenFailingRequestHandlerRegistryMock();

        whenBindRequestHandlerRegistry();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldCatchExceptionsOnRequestHandlerUnbind() throws KuraException {
        givenFailingRequestHandlerRegistryMock();

        whenUnbindRequestHandlerRegistry();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldBindRestIdentityServiceDependencies() throws KuraException {
        givenUserAdminMock();
        givenIdentityServiceMock();

        whenBindDependencies();

        thenNoExceptionOccurred();
    }

    /*
     * Given
     */

    private void givenUserAdminMock() {
        this.userAdminMock = mock(UserAdmin.class);
    }

    private void givenRequestHandlerRegistryMock() {
        this.requestHandlerRegistryMock = mock(RequestHandlerRegistry.class);
    }

    private void givenBoundRequestHandlerRegistry() {
        bindRequestHandlerRegistry();
    }

    private void givenIdentityServiceMock() {
        this.identityServiceMock = mock(IdentityService.class);
    }

    private void givenFailingRequestHandlerRegistryMock() throws KuraException {
        this.requestHandlerRegistryMock = mock(RequestHandlerRegistry.class);
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistryMock)
                .registerRequestHandler(any(), any());
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistryMock).unregister(any());
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

    private void whenBindDependencies() {
        try {
            this.service.bindIdentityService(this.identityServiceMock);
            this.service.bindRequestHandlerRegistry(this.requestHandlerRegistryMock);
            this.service.bindUserAdmin(this.userAdminMock);
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
