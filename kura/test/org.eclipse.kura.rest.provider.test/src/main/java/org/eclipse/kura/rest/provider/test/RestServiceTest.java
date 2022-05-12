/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.provider.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.requesthandler.Transport.Response;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class RestServiceTest extends AbstractRequestHandlerTest {

    public RestServiceTest() {
        super(new RestTransport("testservice"));
    }

    @Test
    public void shouldReturnNotFoundIfNoServiceIsRegistered() {

        whenRequestIsPerformed(new MethodSpec("GET"), "/foo");

        thenResponseCodeIs(404);
    }

    @Test
    public void shouldNotRequireAuth() {
        givenService(new NoAuth());
        givenNoBasicCredentials();

        whenRequestIsPerformed(new MethodSpec("GET"), "/noAuth");

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldRerturn403IfUserIsNotInRole() {
        givenService(new RequiresAssetsRole());
        givenIdentity("fooo", Optional.of("barr"), Collections.emptyList());
        givenBasicCredentials(Optional.of("fooo:barr"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(403);
    }

    @Test
    public void shouldRerturn401IfPasswordIsWrong() {
        givenService(new RequiresAssetsRole());
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenBasicCredentials(Optional.of("foo:baz"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldAllowAccessIfUserIsInRole() {
        givenService(new RequiresAssetsRole());
        givenIdentity("foo1", Optional.of("bar1"), Collections.singletonList("rest.assets"));
        givenBasicCredentials(Optional.of("foo1:bar1"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldRerturn401IfNoCredentialsAreProvided() {
        givenService(new RequiresAssetsRole());
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenNoBasicCredentials();

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldEnableAuthenticationProvider() {

        whenAuthenticationProviderIsRegistered(new DummyAuthenticationProvider("foo", "bar", "admin"));

        thenProviderIsEnabled();
    }

    @Test
    public void shouldDisableAuthenticationProvider() {

        givenAuthenticationProvider(new DummyAuthenticationProvider("foo", "bar", "admin"));

        whenLastRegisteredServiceIsUnregistered();

        thenProviderIsDisabled();
    }

    @Test
    public void shouldSupportCustomAuthenticationProvider() {
        givenService(new RequiresAssetsRole());
        givenAuthenticationProvider(new DummyAuthenticationProvider("bar", "baz", "admin"));
        givenBasicCredentials(Optional.of("bar:baz"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenProviderAuthenticatedRequest();
        thenResponseCodeIs(200);
    }

    @Test
    public void shouldSupportLowPriorityAuthenticationHandler() {
        givenService(new RequiresAssetsRole());
        givenAuthenticationProvider(new LowPriorityAuthenticationProvider("foo", "bar", "admin"));
        givenBasicCredentials(Optional.of("foo:bar"));
        givenIdentity("foo", Optional.of("bar"), Collections.singletonList("rest.assets"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenProviderDidNotAuthenticateRequest();
        thenResponseCodeIs(200);
    }

    @Test
    public void shouldSupportHighPriorityAuthenticationHandler() {
        givenService(new RequiresAssetsRole());
        givenAuthenticationProvider(new HighPriorityAuthenticationProvider("foo", "bar", "admin"));
        givenBasicCredentials(Optional.of("foo:bar"));
        givenIdentity("foo", Optional.of("bar"), Collections.singletonList("rest.assets"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenProviderAuthenticatedRequest();
        thenResponseCodeIs(200);
    }

    @Test
    public void shouldSupportDisablingBuiltInPasswordAuthentication() {
        givenRestServiceConfiguration(Collections.singletonMap("auth.password.enabled", false));
        givenService(new RequiresAssetsRole());
        givenIdentity("foo1", Optional.of("bar1"), Collections.singletonList("rest.assets"));
        givenBasicCredentials(Optional.of("foo1:bar1"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    private List<ServiceRegistration<?>> registeredServices = new ArrayList<>();
    private CompletableFuture<Void> providerEnabled = new CompletableFuture<>();
    private CompletableFuture<Void> providerDisabled = new CompletableFuture<>();
    private CompletableFuture<Void> providerAuthenticated = new CompletableFuture<>();
    private boolean customizedConfig = false;

    public void givenNoBasicCredentials() {
        givenBasicCredentials(Optional.empty());
    }

    public void givenBasicCredentials(final Optional<String> basicCredentials) {
        ((RestTransport) this.transport).setBasicCredentials(basicCredentials);
    }

    private void givenRestServiceConfiguration(final Map<String, Object> properties) {
        try {
            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            ServiceUtil
                    .updateComponentConfiguration(configurationService,
                            "org.eclipse.kura.internal.rest.provider.RestService", properties)
                    .get(30, TimeUnit.SECONDS);

            customizedConfig = true;
        } catch (final Exception e) {
            fail("failed to update rest service configuration");
            return;
        }

    }

    @SuppressWarnings("unchecked")
    private <T extends TestService> void givenService(final T service) {
        final BundleContext bundleContext = FrameworkUtil.getBundle(RestServiceTest.class).getBundleContext();

        registeredServices.add(bundleContext.registerService((Class<T>) service.getClass(), service, null));

        final RestTransport restTransport = (RestTransport) this.transport;

        for (int i = 0; i < 100; i++) {
            final Response response = restTransport.runRequest("/ping", new MethodSpec("GET"));

            if (response.getStatus() != 404 && response.getStatus() != 503) {
                break;
            }

            try {
                synchronized (this) {
                    this.wait(100);
                }
            } catch (InterruptedException e) {
                fail("Interrupted wile waiting for service startup");
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void givenIdentity(final String username, final Optional<String> password, final List<String> roles) {
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

        for (final String role : roles) {
            getOrCreateRole(userAdmin, "kura.permission." + role, Group.class).addMember(user);
        }
    }

    private void givenAuthenticationProvider(final AuthenticationProvider provider) {
        final BundleContext bundleContext = FrameworkUtil.getBundle(RestServiceTest.class).getBundleContext();

        this.registeredServices.add(bundleContext.registerService(AuthenticationProvider.class, provider, null));

        try {
            this.providerEnabled.get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("provider was not enabled");
        }
    }

    private void whenAuthenticationProviderIsRegistered(final AuthenticationProvider provider) {
        givenAuthenticationProvider(provider);
    }

    private void whenLastRegisteredServiceIsUnregistered() {
        this.registeredServices.remove(this.registeredServices.size() - 1).unregister();
    }

    private void thenProviderIsEnabled() {
        try {
            this.providerEnabled.get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("provider was not enabled");
        }
    }

    private void thenProviderIsDisabled() {
        try {
            this.providerDisabled.get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("provider was not enabled");
        }
    }

    private void thenProviderAuthenticatedRequest() {
        try {
            this.providerAuthenticated.get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("provider didn't authenticate request");
        }
    }

    private void thenProviderDidNotAuthenticateRequest() {
        assertFalse("provider authenticated request", this.providerAuthenticated.isDone());
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

    @After
    public void cleanUp() {
        this.registeredServices.forEach(ServiceRegistration::unregister);

        if (customizedConfig) {
            final Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("auth.password.enabled", true);
            defaultConfig.put("auth.certificate.enabled", true);
            defaultConfig.put("allowed.ports", new Integer[] {});

            givenRestServiceConfiguration(defaultConfig);
        }
    }

    public static abstract class TestService {

        @GET
        @Path("/ping")
        public String ping() {
            return "ok";
        }

    }

    @Path("testservice")
    public static class NoAuth extends TestService {

        @GET
        @Path("/noAuth")
        public String noAuth() {
            return "Hello";
        }
    }

    @Path("testservice")
    public static class RequiresAssetsRole extends TestService {

        @GET
        @Path("requireAssets")
        @RolesAllowed("assets")
        public String requireAssets() {
            return "Hello";
        }
    }

    public class DummyAuthenticationProvider implements AuthenticationProvider {

        final String expectedCredentials;
        final String targetIdentity;

        public DummyAuthenticationProvider(final String expectedUsername, final String expectedPassword,
                final String targetIdentity) {
            this.expectedCredentials = expectedUsername + ":" + expectedPassword;
            this.targetIdentity = targetIdentity;
        }

        @Override
        public void onEnabled() {
            providerEnabled.complete(null);
        }

        @Override
        public void onDisabled() {
            providerDisabled.complete(null);
        }

        @Override
        public Optional<Principal> authenticate(HttpServletRequest request, ContainerRequestContext requestContext) {

            String authHeader = requestContext.getHeaderString("Authorization");
            if (authHeader == null) {
                return Optional.empty();
            }

            StringTokenizer tokens = new StringTokenizer(authHeader);
            String authScheme = tokens.nextToken();
            if (!"Basic".equals(authScheme)) {
                return Optional.empty();
            }

            final String credentials = new String(Base64.getDecoder().decode(tokens.nextToken()),
                    StandardCharsets.UTF_8);

            if (expectedCredentials.equals(credentials)) {
                providerAuthenticated.complete(null);
                return Optional.of(() -> targetIdentity);
            } else {
                return Optional.empty();
            }
        }
    }

    @Priority(1000)
    private class LowPriorityAuthenticationProvider extends DummyAuthenticationProvider {

        public LowPriorityAuthenticationProvider(String expectedUsername, String expectedPassword,
                String targetIdentity) {
            super(expectedUsername, expectedPassword, targetIdentity);
        }
    }

    @Priority(1)
    private class HighPriorityAuthenticationProvider extends DummyAuthenticationProvider {

        public HighPriorityAuthenticationProvider(String expectedUsername, String expectedPassword,
                String targetIdentity) {
            super(expectedUsername, expectedPassword, targetIdentity);
        }
    }
}
