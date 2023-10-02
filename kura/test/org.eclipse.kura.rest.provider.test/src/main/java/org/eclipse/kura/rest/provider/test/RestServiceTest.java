/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CertificateCreationOptions;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.requesthandler.Transport.Response;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

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

    @Test
    public void shouldSupportCertificateAuthenticationOnResourcePath() {
        givenRestServiceConfiguration(Collections.singletonMap("allowed.ports", new Integer[] { 8080, 9999 }));
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.empty(), Arrays.asList("rest.assets"));
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "foo");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        whenRequestIsPerformed("https", 9999, new MethodSpec("GET"), "/testservice/requireAssets", null);

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldRejectRequestIfCertCommonNameDoesNotMatchIdentity() {
        givenRestServiceConfiguration(Collections.singletonMap("allowed.ports", new Integer[] { 8080, 9999 }));
        givenNoBasicCredentials();
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "doesNotExist");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        whenRequestIsPerformed("https", 9999, new MethodSpec("GET"), "/testservice/requireAssets", null);

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldCreateSessionWithUsernameAndPassword() {
        givenService(new RequiresAssetsRole());
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenNoBasicCredentials();

        whenRequestIsPerformed("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");

        thenResponseCodeIs(200);
        thenResponseBodyEqualsJson("{\"passwordChangeNeeded\":false}");
        thenResponseHasCookie("JSESSIONID");
    }

    @Test
    public void shouldNotCreateSessionWithWrongPassword() {
        givenService(new RequiresAssetsRole());
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenNoBasicCredentials();

        whenRequestIsPerformed("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"baz\"}");

        thenResponseCodeIs(401);
        thenResponseDoesNotHaveCookie("JSESSIONID");
    }

    @Test
    public void shouldNotCreateSessionWithWrongUsername() {
        givenService(new RequiresAssetsRole());
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenNoBasicCredentials();

        whenRequestIsPerformed("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"bar\",\"password\":\"bar\"}");

        thenResponseCodeIs(401);
        thenResponseDoesNotHaveCookie("JSESSIONID");
    }

    @Test
    public void shouldCreateSessionWithCertificate() {
        givenRestServiceConfiguration(Collections.singletonMap("allowed.ports", new Integer[] { 8080, 9999 }));
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.empty(), Arrays.asList("rest.assets"));
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "foo");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        whenRequestIsPerformed("https", 9999, new MethodSpec("POST"), "/session/v1/login/certificate", null);

        thenRequestSucceeds();
        thenResponseHasCookie("JSESSIONID");
    }

    @Test
    public void shouldNotCreateSessionWithCertificateIfCNDoesNotMatchIdentity() {
        givenRestServiceConfiguration(Collections.singletonMap("allowed.ports", new Integer[] { 8080, 9999 }));
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.empty(), Arrays.asList("rest.assets"));
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "bar");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        whenRequestIsPerformed("https", 9999, new MethodSpec("POST"), "/session/v1/login/certificate", null);

        thenResponseCodeIs(401);
        thenResponseDoesNotHaveCookie("JSESSIONID");
    }

    @Test
    public void shouldReturnXSRFTokenIfSessionIsOpen() {
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenNoBasicCredentials();

        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        whenRequestIsPerformed("http", 8080, new MethodSpec("GET"), "/session/v1/xsrfToken",
                null);

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldNotReturnXSRFTokenIfSessionIsNotOpen() {
        givenIdentity("foo", Optional.of("bar"), Collections.emptyList());
        givenNoBasicCredentials();

        whenRequestIsPerformed("http", 8080, new MethodSpec("GET"), "/session/v1/xsrfToken",
                null);

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldAllowAccessingResourceThroughSessionAuth() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldNotAllowAccessingResourceThroughSessionAuthIfXsrfTokenIsMissing() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldSupportLogout() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/logout",
                null);

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldRequireXsrfTokenForLogout() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        whenRequestIsPerformed("http", 8080, new MethodSpec("POST"), "/session/v1/logout",
                null);

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldDenyResourceAccessIfPasswordChangeIsNeeded() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"), true);
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldRejectPassordChangeWithSamePassword() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"), true);
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();

        whenRequestIsPerformed("http", 8080, new MethodSpec("POST"), "/session/v1/changePassword",
                "{\"currentPassword\":\"bar\",\"newPassword\":\"bar\"}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldAllowResourceAccessAfterPasswordChange() {
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"), true);
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/changePassword",
                "{\"currentPassword\":\"bar\",\"newPassword\":\"baz\"}");
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"baz\"}");
        givenXsrfToken();

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldSupportSessionExpiration() {
        givenRestServiceConfiguration(Collections.singletonMap("session.inactivity.interval", 1));
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenDelay(2, TimeUnit.SECONDS);

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldNotAllowGettingXsrfTokenIfSessionIsExpired() {
        givenRestServiceConfiguration(Collections.singletonMap("session.inactivity.interval", 1));
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenDelay(2, TimeUnit.SECONDS);

        whenRequestIsPerformed("http", 8080, new MethodSpec("GET"), "/session/v1/xsrfToken", null);

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldExtendSessionLifetimeOnActivity() {
        givenRestServiceConfiguration(Collections.singletonMap("session.inactivity.interval", 1));
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenDelay(500, TimeUnit.MILLISECONDS);
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenDelay(500, TimeUnit.MILLISECONDS);
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenDelay(500, TimeUnit.MILLISECONDS);
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");
        givenDelay(2, TimeUnit.SECONDS);

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldIgnorePasswordChangeNeededWithCertificateAuth() {
        givenRestServiceConfiguration(Collections.singletonMap("allowed.ports", new Integer[] { 8080, 9999 }));
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"), true);
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "foo");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        givenSuccessfulRequest("https", 9999, new MethodSpec("POST"), "/session/v1/login/certificate", null);
        givenXsrfToken("https", 9999);

        whenRequestIsPerformed("https", 9999, new MethodSpec("GET"), "/testservice/requireAssets", null);

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldSupportDisablingBuiltInBasicAuthentication() {
        givenRestServiceConfiguration(Collections.singletonMap("auth.basic.enabled", false));
        givenService(new RequiresAssetsRole());
        givenIdentity("foo1", Optional.of("bar1"), Collections.singletonList("rest.assets"));
        givenBasicCredentials(Optional.of("foo1:bar1"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldSupportDisablingCertificateAuthenticationOnResourcePath() {
        givenRestServiceConfiguration(map("allowed.ports", new Integer[] { 8080, 9999 }, //
                "auth.certificate.stateless.enabled", false));
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.empty(), Arrays.asList("rest.assets"));
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "foo");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        whenRequestIsPerformed("https", 9999, new MethodSpec("GET"), "/testservice/requireAssets", null);

        thenResponseCodeIs(401);
    }

    @Test
    public void shouldAllowAccessingResourceThroughSessionAuthEvenIfBasicAuthIsDisabled() {
        givenRestServiceConfiguration(Collections.singletonMap("auth.basic.enabled", false));
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();

        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldAllowAccessingResourceThroughSessionAuthEvenIfLegacyCertificateAuthIsDisabled() {
        givenRestServiceConfiguration(map("allowed.ports", new Integer[] { 8080, 9999 }, //
                "auth.certificate.stateless.enabled", false));
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"), true);
        givenService(new RequiresAssetsRole());
        givenCA("clientCA");
        givenCA("serverCA");
        givenKeystoreService("clientKeystore");
        givenCACertificateInKeystore("clientKeystore", "serverCA");
        givenKeystoreService("serverKeystore");
        givenCACertificateInKeystore("serverKeystore", "clientCA");
        givenKeyPairInKeystore("clientKeystore", "clientCA", "foo");
        givenKeyPairInKeystore("serverKeystore", "serverCA", "serverCert");
        givenHttpServiceClientCertAuthEnabled("serverKeystore", 9999);
        givenClientKeystore("clientKeystore");

        givenSuccessfulRequest("https", 9999, new MethodSpec("POST"), "/session/v1/login/certificate", null);
        givenXsrfToken("https", 9999);

        whenRequestIsPerformed("https", 9999, new MethodSpec("GET"), "/testservice/requireAssets", null);

        thenResponseCodeIs(200);
    }

    @Test
    public void shouldTerminateSessionIfUserCredentialsChange() {
        givenRestServiceConfiguration(Collections.singletonMap("auth.basic.enabled", false));
        givenService(new RequiresAssetsRole());
        givenNoBasicCredentials();
        givenIdentity("foo", Optional.of("bar"), Arrays.asList("rest.assets"));
        givenSuccessfulRequest("http", 8080, new MethodSpec("POST"), "/session/v1/login/password",
                "{\"username\":\"foo\",\"password\":\"bar\"}");
        givenXsrfToken();
        givenSuccessfulRequest(new MethodSpec("GET"), "/requireAssets");

        whenIdentityIsUpdated("foo", Optional.of("baz"), Arrays.asList("rest.assets"));
        whenRequestIsPerformed(new MethodSpec("GET"), "/requireAssets");

        thenResponseCodeIs(401);
    }

    private List<ServiceRegistration<?>> registeredServices = new ArrayList<>();
    private CompletableFuture<Void> providerEnabled = new CompletableFuture<>();
    private CompletableFuture<Void> providerDisabled = new CompletableFuture<>();
    private CompletableFuture<Void> providerAuthenticated = new CompletableFuture<>();
    private boolean customizedConfig = false;
    private final Map<String, KeystoreService> keystoreServices = new HashMap<>();
    private final Map<String, TestCA> testCAs = new HashMap<>();
    private final Set<String> createdFactoryPids = new HashSet<>();

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

    private void givenKeystoreService(final String pid) {
        try {
            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            final java.nio.file.Path directoryPath = Files.createTempDirectory(null);

            final Map<String, Object> properties = Collections.singletonMap("keystore.path",
                    new File(directoryPath.toFile(), System.nanoTime() + ".ks").getAbsolutePath());

            final KeystoreService keystoreService = ServiceUtil
                    .createFactoryConfiguration(configurationService, KeystoreService.class,
                            pid, "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl", properties)
                    .get(30, TimeUnit.SECONDS);

            this.keystoreServices.put(pid, keystoreService);
            this.createdFactoryPids.add(pid);
        } catch (final Exception e) {
            fail("failed to create KeystoreService");
            return;
        }
    }

    private void givenCA(final String cn) {
        try {
            final TestCA testCA = new TestCA(
                    CertificateCreationOptions.builder(new X500Name("cn=" + cn + ", dc=bar.com")).build());
            this.testCAs.put(cn, testCA);
        } catch (Exception e) {
            fail("cannot cerate test CA");
        }
    }

    private void givenCACertificateInKeystore(final String keystorePid, final String caCN) {
        try {
            final TestCA testCA = this.testCAs.get(caCN);
            final KeystoreService keystoreService = this.keystoreServices.get(keystorePid);

            keystoreService.setEntry(caCN, new TrustedCertificateEntry(testCA.getCertificate()));
        } catch (Exception e) {
            fail("cannot store trusted certificate");
        }
    }

    private void givenKeyPairInKeystore(final String keystorePid, final String caCN, final String certCN) {
        try {
            final TestCA testCA = this.testCAs.get(caCN);
            final KeystoreService keystoreService = this.keystoreServices.get(keystorePid);

            final KeyPair keyPair = TestCA.generateKeyPair();
            final X509Certificate clientCertificate = testCA.createAndSignCertificate(CertificateCreationOptions
                    .builder(new X500Name("cn=" + certCN + ", dc=bar.com"))
                    .build(), keyPair);

            keystoreService.setEntry(certCN, new PrivateKeyEntry(keyPair.getPrivate(),
                    new Certificate[] { clientCertificate, testCA.getCertificate() }));
        } catch (Exception e) {
            fail("cannot create client key pair");
        }
    }

    private void givenHttpServiceClientCertAuthEnabled(final String keystorePid, final int port) {
        try {
            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            final Map<String, Object> properties = new HashMap<>();
            properties.put("KeystoreService.target",
                    "(kura.service.pid=" + keystorePid + ")");
            properties.put("https.client.auth.ports", new Integer[] { port });

            configurationService.updateConfiguration("org.eclipse.kura.http.server.manager.HttpService", properties);

            RestTransport.waitPortOpen("localhost", port, 1, TimeUnit.MINUTES);

        } catch (Exception e) {
            fail("cannot set httpservice keystore pid");
        }
    }

    private void givenClientKeystore(final String keystorePid) {
        try {
            final KeystoreService keystoreService = this.keystoreServices.get(keystorePid);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystoreService.getKeyStore());
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

            sslContext.init(
                    keystoreService.getKeyManagers(KeyManagerFactory.getDefaultAlgorithm()).toArray(new KeyManager[0]),
                    trustManagerFactory.getTrustManagers(), new SecureRandom());

            ((RestTransport) this.transport).setSslContext(sslContext);
        } catch (Exception e) {
            fail("cannot set httpservice keystore pid");
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

    private void givenIdentity(final String username, final Optional<String> password, final List<String> roles) {
        givenIdentity(username, password, roles, false);
    }

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

    private void givenAuthenticationProvider(final AuthenticationProvider provider) {
        final BundleContext bundleContext = FrameworkUtil.getBundle(RestServiceTest.class).getBundleContext();

        this.registeredServices.add(bundleContext.registerService(AuthenticationProvider.class, provider, null));

        try {
            this.providerEnabled.get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("provider was not enabled");
        }
    }

    private void givenSuccessfulRequest(final String proto, final int port, final MethodSpec method,
            final String resource,
            final String body) {
        whenRequestIsPerformed(proto, port, method, resource, body);
        thenRequestSucceeds();
    }

    private void givenSuccessfulRequest(final MethodSpec method,
            final String resource) {
        whenRequestIsPerformed(method, resource);
        thenRequestSucceeds();
    }

    private void givenXsrfToken() {
        whenXsrfTokenIsObtained();
    }

    private void givenXsrfToken(final String protocol, final int port) {
        whenXsrfTokenIsObtained(protocol, port);
    }

    private void givenDelay(final long amount, final TimeUnit timeUnit) {
        whenDelayPasses(amount, timeUnit);
    }

    private void whenRequestIsPerformed(final String proto, final int port, final MethodSpec method,
            final String resource,
            final String body) {
        final RestTransport restTransport = (RestTransport) this.transport;

        this.response = Optional
                .of(restTransport.runRequest(proto + "://localhost:" + port + "/services", resource, method, body));
    }

    private void whenXsrfTokenIsObtained() {
        whenXsrfTokenIsObtained("http", 8080);
    }

    private void whenXsrfTokenIsObtained(final String protocol, final int port) {
        final RestTransport restTransport = (RestTransport) this.transport;

        final Response response = restTransport.runRequest(protocol + "://localhost:" + port + "/services",
                "/session/v1/xsrfToken",
                new MethodSpec("GET"), null);

        final JsonObject object = Json
                .parse(response.getBody().orElseThrow(() -> new IllegalStateException("no response body"))).asObject();

        final String token = object.get("xsrfToken").asString();

        restTransport.setHeader("X-XSRF-Token", token);
    }

    private void whenDelayPasses(final long amount, final TimeUnit timeUnit) {
        synchronized (this) {
            try {
                this.wait(timeUnit.toMillis(amount));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void whenAuthenticationProviderIsRegistered(final AuthenticationProvider provider) {
        givenAuthenticationProvider(provider);
    }

    private void whenLastRegisteredServiceIsUnregistered() {
        this.registeredServices.remove(this.registeredServices.size() - 1).unregister();
    }

    private void whenIdentityIsUpdated(final String username, final Optional<String> password,
            final List<String> roles) {
        givenIdentity(username, password, roles, false);
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
            defaultConfig.put("auth.certificate.stateless.enabled", true);
            defaultConfig.put("auth.basic.enabled", true);
            defaultConfig.put("session.inactivity.interval", 900);
            defaultConfig.put("allowed.ports", new Integer[] {});

            givenRestServiceConfiguration(defaultConfig);
        }

        try {
            if (createdFactoryPids.isEmpty()) {
                return;
            }

            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            for (final String pid : createdFactoryPids) {
                ServiceUtil.deleteFactoryConfiguration(configurationService, pid).get(30, TimeUnit.SECONDS);
            }
        } catch (final Exception e) {
            fail("failed to cleanup registered factory components");
        }
    }

    private static Map<String, Object> map(final Object... objects) {
        final Iterator<Object> iter = Arrays.asList(objects).iterator();

        final Map<String, Object> result = new HashMap<>();

        while (iter.hasNext()) {
            final Object key = iter.next();
            final Object value = iter.next();

            result.put((String) key, value);
        }

        return result;
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
