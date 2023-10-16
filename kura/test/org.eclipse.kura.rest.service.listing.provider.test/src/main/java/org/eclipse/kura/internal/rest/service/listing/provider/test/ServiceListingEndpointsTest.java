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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.service.listing.provider.test.constants.ServiceListeningTestConstants;
import org.junit.After;
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

        givenIdentity("noAuthUser", Optional.of("pass1"), Collections.emptyList());
        givenBasicCredentials(Optional.empty());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/servicePids");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldReturnListOfAllServices() {

        givenRegisteredService(TestInterface.class, "TestService", Collections.emptyMap());
        givenIdentity("authUser", Optional.of("pass2"), Collections.emptyList());
        givenBasicCredentials(Optional.of("authUser:pass2"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/servicePids");

        thenRequestSucceeds();
        thenResponseContainsPid("TestService");
    }

    @Test
    public void shouldReturnListOfFilteredServices() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byInterface",
                ServiceListeningTestConstants.COMPLETE_POST_BODY);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.FILTERED_SERVICES_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyIsNull() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byInterface",
                ServiceListeningTestConstants.NULL_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.NULL_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyIsEmpty() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byInterface",
                ServiceListeningTestConstants.EMPTY_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.EMPTY_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyHasNullField() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byInterface",
                ServiceListeningTestConstants.NULL_FIELD_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.NULL_FIELD_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyHasEmptyField() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byInterface",
                ServiceListeningTestConstants.EMPTY_FIELD_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.EMPTY_FIELD_BODY_RESPONSE);
    }

    @Test
    public void shouldSupportPropertyMatchFilter() {
        givenRegisteredService(TestInterface.class, "foo", Collections.singletonMap("foo", "bar"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"name\":\"foo\",\"value\":\"bar\"}");

        thenRequestSucceeds();
        thenResponseContainsPid("foo");
    }

    @Test
    public void shouldSupportPropertyMatchFilterWithArray() {
        givenRegisteredService(TestInterface.class, "foo",
                Collections.singletonMap("foo", new String[] { "bar", "baz" }));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"name\":\"foo\",\"value\":\"bar\"}");

        thenRequestSucceeds();
        thenResponseContainsPid("foo");
    }

    @Test
    public void shouldSupportPropertyPresenceFilter() {
        givenRegisteredService(TestInterface.class, "foo", Collections.singletonMap("foo", "bar"));
        givenRegisteredService(TestInterface.class, "bar", Collections.singletonMap("foo", "baz"));
        givenRegisteredService(TestInterface.class, "baz", Collections.singletonMap("fooo", "bar"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter", "{\"name\":\"foo\"}");

        thenRequestSucceeds();
        thenResponseContainsPid("foo");
        thenResponseContainsPid("bar");
        thenResponseDoesNotContainPid("baz");
    }

    @Test
    public void shouldSupportNotFilter() {
        givenRegisteredService(TestInterface.class, "foo", Collections.singletonMap("foo", "bar"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"not\": {\"name\":\"foo\",\"value\":\"bar\"} }");

        thenRequestSucceeds();
        thenResponseDoesNotContainPid("foo");
    }

    @Test
    public void shouldSupportAndFilter() {
        givenRegisteredService(TestInterface.class, "foo", Collections.singletonMap("foo", "bar"));
        givenRegisteredService(TestInterface.class, "bar", Collections.singletonMap("foo", "bar"));
        givenRegisteredService(TestInterface.class, "baz", Collections.singletonMap("fooo", "bar"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"and\": [ {\"name\":\"foo\",\"value\":\"bar\"}, {\"name\":\"kura.service.pid\",\"value\":\"foo\"} ] }");

        thenRequestSucceeds();
        thenResponseContainsPid("foo");
        thenResponseDoesNotContainPid("bar");
        thenResponseDoesNotContainPid("baz");
    }

    @Test
    public void shouldSupportOrFilter() {
        givenRegisteredService(TestInterface.class, "foo", Collections.singletonMap("foo", "baz"));
        givenRegisteredService(TestInterface.class, "bar", Collections.singletonMap("foo", "bar"));
        givenRegisteredService(TestInterface.class, "baz", Collections.singletonMap("fooo", "bar"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"or\": [ {\"name\":\"fooo\" }, {\"name\":\"foo\",\"value\":\"bar\"} ] }");

        thenRequestSucceeds();
        thenResponseContainsPid("baz");
        thenResponseContainsPid("bar");
        thenResponseDoesNotContainPid("foo");
    }

    @Test
    public void shouldSupportServicesSatisfyingReference() {
        givenRegisteredService(TestInterface.class, "foo", Collections.singletonMap("foo", "baz"));
        givenRegisteredService(TestInterface.class, "bar", Collections.singletonMap("foo", "bar"));
        givenRegisteredService(OtherTestInterface.class, "baz", Collections.singletonMap("fooo", "bar"));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/satisfyingReference",
                "{\"pid\": \"org.eclipse.kura.internal.rest.service.listing.provider.test.TargetFilterTestService\","
                        + " \"referenceName\": \"TestInterface\" }");

        thenRequestSucceeds();
        thenResponseContainsPid("foo");
        thenResponseContainsPid("bar");
        thenResponseDoesNotContainPid("baz");
    }

    @Test
    public void shouldSupportListingFactoryPids() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/factoryPids");

        thenRequestSucceeds();
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory1");
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory2");
    }

    @Test
    public void shouldSupportListingFactorysPidByInterface() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryPids/byInterface",
                "{\"interfaceNames\":[\"org.eclipse.kura.internal.rest.service.listing.provider.test.TestInterface\"]}");

        thenRequestSucceeds();
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory1");
        thenResponseDoesNotContainPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory2");
    }

    @Test
    public void shouldSupportListingFactoryPidsSpecifyingMultipleInterfaces() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryPids/byInterface",
                "{\"interfaceNames\":[\"org.eclipse.kura.configuration.ConfigurableComponent\", \"org.eclipse.kura.internal.rest.service.listing.provider.test.OtherTestInterface\"]}");

        thenRequestSucceeds();
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory2");
        thenResponseDoesNotContainPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory1");
    }

    @Test
    public void shouldSupportListingFactoryPidsByProperty() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryPids/byFilter",
                "{\"name\": \"testProperty\", \"value\": \"testValue\"}");

        thenRequestSucceeds();
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory1");
        thenResponseDoesNotContainPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory2");
    }

    @Test
    public void shouldSupportListingFactoryPidsByPropertyOfArrayType() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryPids/byFilter",
                "{\"name\": \"testProperty\", \"value\": \"value2\"}");

        thenRequestSucceeds();
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory2");
        thenResponseDoesNotContainPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory1");
    }

    @Test
    public void shouldSupportListingFactoryPidsByPropertyAndObjectClass() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/factoryPids/byFilter",
                "{\"and\": [ {\"name\":\"objectClass\", \"value\":\"org.eclipse.kura.internal.rest.service.listing.provider.test.TestInterface\"}, {\"name\":\"testProperty\",\"value\": \"testValue\"} ] }");

        thenRequestSucceeds();
        thenResponseContainsPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory1");
        thenResponseDoesNotContainPid("org.eclipse.kura.internal.rest.service.listing.provider.test.TestFactory2");
    }

    @Test
    public void shouldRejectEmptyFilter() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter", "{}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectFilterWithSpacesInPropertyName() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"name\": \" testProperty \", \"value\": \"value2\"}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectFilterWithMultipleTypes() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/byFilter",
                "{\"and\": [], \"name\": \" testProperty \", \"value\": \"value2\"}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectReferenceWithoutPid() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/satisfyingReference",
                "{ \"referenceName\": \"TestInterface\" }");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectReferenceWithEmptyPid() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/satisfyingReference",
                "{ \"pid\": \"\", \"referenceName\": \"TestInterface\" }");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectReferenceWithoutReferenceName() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/satisfyingReference",
                "{ \"pid\": \"foo\" }");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectReferenceWithEmptyReferenceName() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/servicePids/satisfyingReference",
                "{ \"pid\": \"foo\", \"referenceName\": \"\" }");

        thenResponseCodeIs(400);
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

    private <T> void givenRegisteredService(final Class<T> clazz, final String pid,
            final Map<String, Object> properties) {

        final T testService = Mockito.mock(clazz);
        BundleContext context = FrameworkUtil.getBundle(ServiceListingEndpointsTest.class).getBundleContext();
        final Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("kura.service.pid", pid);

        for (final Entry<String, Object> e : properties.entrySet()) {
            dict.put(e.getKey(), e.getValue());
        }

        registeredServices.add(context.registerService(clazz, testService, dict));
    }

    /*
     * THEN
     */

    private void thenResponseContainsPid(final String pid) {
        JsonArray responseArray = expectJsonResponse().get("pids").asArray();
        assertTrue(responseArray.values().contains(Json.value(pid)));
    }

    private void thenResponseDoesNotContainPid(final String pid) {
        JsonArray responseArray = expectJsonResponse().get("pids").asArray();
        assertFalse(responseArray.values().contains(Json.value(pid)));
    }

    /*
     * Utils
     */

    private final List<ServiceRegistration<?>> registeredServices = new ArrayList<>();

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

    @After
    public void unregisterTestService() {
        for (final ServiceRegistration<?> reg : registeredServices) {
            reg.unregister();
        }
    }
}
