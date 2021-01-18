/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Priority;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.jaxrs.provider.security.AuthenticationHandler;
import com.eclipsesource.jaxrs.provider.security.AuthorizationHandler;

@Provider
public class RestService
        implements AuthenticationHandler, AuthorizationHandler, ConfigurableComponent, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    private static final String KURA_PASSWORD_CREDENTIAL = "kura.password";

    private static final String KURA_PERMISSION_PREFIX = "kura.permission.";
    private static final String KURA_PERMISSION_REST_PREFIX = KURA_PERMISSION_PREFIX + "rest.";
    private static final String KURA_USER_PREFIX = "kura.user.";

    private static final String REST_FAILURE_RECEIVED_UNAUTHORIZED_REQUEST = "Rest - Failure - "
            + "Received unauthorized REST request. Method: {}, path: {}, request IP: {}";
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final Decoder BASE64_DECODER = Base64.getDecoder();

    private static final Response UNAUTHORIZED_RESPONSE = Response.status(Response.Status.UNAUTHORIZED)
            .header("WWW-Authenticate", "Basic realm=\"kura-rest-api\"").build();
    private static final Response NOT_FOUND_RESPONSE = Response.status(Response.Status.NOT_FOUND).build();

    private CryptoService cryptoService;
    private UserAdmin userAdmin;

    RestServiceOptions options;

    private final List<ServiceRegistration<?>> registeredServices = new ArrayList<>();

    @Context
    private HttpServletRequest sr;

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        final BundleContext bundleContext = FrameworkUtil.getBundle(RestService.class).getBundleContext();

        registeredServices
                .add(bundleContext.registerService(ContainerRequestFilter.class, new IncomingPortCheckFilter(), null));

        options = new RestServiceOptions(properties);

        logger.info("activating...done");
    }

    public void update(final Map<String, Object> properties) {
        logger.info("updating...");

        options = new RestServiceOptions(properties);

        logger.info("updating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        for (final ServiceRegistration<?> reg : registeredServices) {
            reg.unregister();
        }

        logger.info("deactivating...done");
    }

    @Override
    public boolean isUserInRole(Principal requestUser, String role) {

        try {
            final User user = (User) this.userAdmin.getRole(KURA_USER_PREFIX + requestUser.getName());

            return containsBasicMember(this.userAdmin.getRole(KURA_PERMISSION_REST_PREFIX + role), user)
                    || containsBasicMember(this.userAdmin.getRole(KURA_PERMISSION_PREFIX + "kura.admin"), user);

        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public Principal authenticate(ContainerRequestContext request) {

        String path = getRequestPath(request);

        String requestIp = request.getHeaderString("X-FORWARDED-FOR");
        if (requestIp == null) {
            requestIp = this.sr.getRemoteAddr();
        }

        try {

            final Optional<Principal> fromCertificateAuth = performCertificateAuthentication(request);

            if (fromCertificateAuth.isPresent()) {
                return fromCertificateAuth.get();
            }

            return performBasicAuthentication(request).orElseThrow(IllegalStateException::new);

        } catch (final Exception e) {
            auditLogger.warn(REST_FAILURE_RECEIVED_UNAUTHORIZED_REQUEST, request.getMethod(), path, requestIp);
            request.abortWith(UNAUTHORIZED_RESPONSE);
            return null;
        }

    }

    private String getRequestPath(ContainerRequestContext request) {
        List<PathSegment> pathSegments = request.getUriInfo().getPathSegments();
        Iterator<PathSegment> iterator = pathSegments.iterator();
        StringBuilder pathBuilder = new StringBuilder();

        while (iterator.hasNext()) {
            pathBuilder.append(iterator.next().getPath());
            if (iterator.hasNext()) {
                pathBuilder.append("/");
            }
        }

        return pathBuilder.toString();
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }

    private Optional<Principal> performBasicAuthentication(final ContainerRequestContext request)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        String authHeader = request.getHeaderString("Authorization");
        if (authHeader == null) {
            return Optional.empty();
        }

        StringTokenizer tokens = new StringTokenizer(authHeader);
        String authScheme = tokens.nextToken();
        if (!"Basic".equals(authScheme)) {
            return Optional.empty();
        }

        final String credentials = new String(BASE64_DECODER.decode(tokens.nextToken()), StandardCharsets.UTF_8);

        int colon = credentials.indexOf(':');
        String userName = credentials.substring(0, colon);
        String requestPassword = credentials.substring(colon + 1);

        final User user = (User) userAdmin.getRole(KURA_USER_PREFIX + userName);

        final String passwordHash = (String) user.getCredentials().get(KURA_PASSWORD_CREDENTIAL);

        if (cryptoService.sha256Hash(requestPassword).equals(passwordHash)) {
            return Optional.of(() -> userName);
        } else {
            return Optional.empty();
        }

    }

    private Optional<Principal> performCertificateAuthentication(final ContainerRequestContext context) {
        try {

            final Object clientCertificatesRaw = context.getProperty("javax.servlet.request.X509Certificate");

            if (!(clientCertificatesRaw instanceof X509Certificate[])) {
                return Optional.empty();
            }

            final X509Certificate[] clientCertificates = (X509Certificate[]) clientCertificatesRaw;

            if (clientCertificates.length == 0) {
                throw new IllegalArgumentException("Certificate chain is empty");
            }

            final LdapName ldapName = new LdapName(clientCertificates[0].getSubjectX500Principal().getName());

            final Optional<Rdn> commonNameRdn = ldapName.getRdns().stream()
                    .filter(r -> "cn".equalsIgnoreCase(r.getType())).findAny();

            if (!commonNameRdn.isPresent()) {
                throw new IllegalArgumentException("Certificate common name is not present");
            }

            final String commonName = (String) commonNameRdn.get().getValue();

            if (this.userAdmin.getRole(KURA_USER_PREFIX + commonName) instanceof User) {
                return Optional.of(() -> commonName);
            }

            return Optional.empty();

        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    private static boolean containsBasicMember(final Role group, final User user) {
        if (!(group instanceof Group)) {
            return false;
        }

        final Group asGroup = (Group) group;

        final Role[] members = asGroup.getMembers();

        if (members == null) {
            return false;
        }

        for (final Role member : members) {
            if (member.getName().equals(user.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String path = getRequestPath(requestContext);
        int responseStatus = responseContext.getStatus();
        final SecurityContext context = requestContext.getSecurityContext();

        if (context == null) {
            auditLogger.warn(
                    "Rest - Warning - No security context method: {}, path: {}, response code: {}, message: {}",
                    requestContext.getMethod(), path, responseStatus, responseContext.getEntity());
            return;
        }

        final Principal principal = context.getUserPrincipal();

        if (principal == null) {
            auditLogger.warn(
                    "Rest - Warning - User not authenticated method: {}, path: {}, response code: {}, message: {}",
                    requestContext.getMethod(), path, responseStatus, responseContext.getEntity());
            return;
        }

        final String user = principal.getName();

        if (responseStatus == Response.Status.OK.getStatusCode()) {
            auditLogger.info("Rest - Success - Request succeeded for user: {}, method: {}, path: {}", user,
                    requestContext.getMethod(), path);
        } else {
            auditLogger.warn(
                    "Rest - Failure - Request failed for user: {}, method: {}, path: {}, response code: {}, message: {}",
                    user, requestContext.getMethod(), path, responseStatus, responseContext.getEntity());
        }

    }

    @Provider
    @Priority(Priorities.AUTHENTICATION - 100)
    private class IncomingPortCheckFilter implements ContainerRequestFilter {

        @Context
        private HttpServletRequest sr;

        @Override
        public void filter(final ContainerRequestContext request) throws IOException {
            final Set<Integer> allowedPorts = options.getAllowedPorts();

            if (allowedPorts.isEmpty()) {
                return;
            }

            final int port = sr.getLocalPort();

            if (!allowedPorts.contains(port)) {
                request.abortWith(NOT_FOUND_RESPONSE);
            }
        }

    }

}
