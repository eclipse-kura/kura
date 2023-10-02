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
package org.eclipse.kura.internal.rest.service.listing.provider.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.EventHandler;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.service.listing.provider.test.constants.ServiceListeningTestConstants;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

public class ServiceListingEndpointsTest extends AbstractRequestHandlerTest {

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String REST_APP_ID = "serviceListing/v1";

    public ServiceListingEndpointsTest() {
        super(new RestTransport(REST_APP_ID));
    }

    @Test
    public void shouldReturnNotFoundIfNoServiceIsRegistered() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/nothing");

        thenResponseCodeIs(404);
    }

    @Test
    public void shouldReturnUnauthorizedStatusWhenNoRestPermissionIsGiven() {

        givenTestServicesRegitered();
        givenIdentity("noAuthUser", Optional.of("pass1"), Collections.emptyList());
        givenBasicCredentials(Optional.empty());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), ServiceListeningTestConstants.GET_ENDPOINT);

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldReturnListOfAllServices() {

        givenTestServicesRegitered();
        givenIdentity("authUser", Optional.of("pass2"), Collections.emptyList());
        givenBasicCredentials(Optional.of("authUser:pass2"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), ServiceListeningTestConstants.GET_ENDPOINT);

        thenRequestSucceeds();
        thenResponseContainsTestServiceKuraServicePid();
    }

    @Test
    public void shouldReturnListOfFilteredServices() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.COMPLETE_POST_BODY);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.FILTERED_SERVICES_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyIsNull() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.NULL_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.NULL_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyIsEmpty() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.EMPTY_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.EMPTY_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyHasNullField() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.NULL_FIELD_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.NULL_FIELD_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyHasEmptyField() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.EMPTY_FIELD_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.EMPTY_FIELD_BODY_RESPONSE);
    }

    /*
     * GIVEN
     */

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

    private static void givenTestServicesRegitered() {

        EventHandler testService = Mockito.mock(EventHandler.class);
        BundleContext context = FrameworkUtil.getBundle(ServiceListingEndpointsTest.class).getBundleContext();
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("kura.service.pid", "TestService");
        serviceRegistration = context.registerService(EventHandler.class, testService, properties);

    }

    /*
     * THEN
     */

    private void thenResponseContainsTestServiceKuraServicePid() {
        JsonArray responseArray = expectJsonResponse().get("servicesList").asArray();
        assertTrue(responseArray.values().contains(Json.value("TestService")));
    }

    /*
     * Utils
     */

    private static ServiceRegistration<EventHandler> serviceRegistration;

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

    @AfterClass
    public static void unregisterTestService() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }
}
