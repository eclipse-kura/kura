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
 *******************************************************************************/
package org.eclipse.kura.internal.rest.auth;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
@Priority(200)
public class BasicAuthenticationProvider implements AuthenticationProvider {

    private static final String PASSWORD_AUTH_FAILED_MSG = "{} Rest - Failure - Authentication failed as username or password not matching";

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationProvider.class);

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final Decoder BASE64_DECODER = Base64.getDecoder();

    private final UserAdminHelper userAdminHelper;
    private final BundleContext bundleContext;

    private Optional<ServiceRegistration<ContainerResponseFilter>> registration = Optional.empty();

    public BasicAuthenticationProvider(final BundleContext bundleContext, final UserAdminHelper userAdminHelper) {
        this.userAdminHelper = userAdminHelper;
        this.bundleContext = bundleContext;
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        final String authHeader = requestContext.getHeaderString("Authorization");
        if (isNull(authHeader)) {
            return Optional.empty();
        }

        StringTokenizer tokens = new StringTokenizer(authHeader);
        String authScheme = tokens.nextToken();
        if (!"Basic".equals(authScheme)) {
            return Optional.empty();
        }

        final RequestCredentials credentials;

        try {
            credentials = RequestCredentials.fromBasicAuthorizationHeader(tokens.nextToken());
        } catch (final Exception e) {
            logger.debug("failed to parse basic credentials", e);
            auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
            return Optional.empty();
        }

        try {
            auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), credentials.username);

            if (this.userAdminHelper.isPasswordChangeRequired(credentials.username)) {
                auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
                return Optional.empty();
            }

            this.userAdminHelper.verifyUsernamePassword(credentials.username, credentials.password);

            auditLogger.info("{} Rest - Success - Authentication succeeded via password provider", auditContext);

            return Optional.of(() -> credentials.username);
        } catch (final Exception e) {
            auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
            return Optional.empty();
        }
    }

    @Override
    public void onEnabled() {
        if (this.registration.isPresent()) {
            return;
        }

        this.registration = Optional.of(
                bundleContext.registerService(ContainerResponseFilter.class, new AuthenticateResponseFilter(), null));
    }

    @Override
    public void onDisabled() {
        registration.ifPresent(ServiceRegistration::unregister);
        registration = Optional.empty();
    }

    @Provider
    private static class AuthenticateResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext request, final ContainerResponseContext response)
                throws IOException {
            final int status = response.getStatus();

            if (status == 401 || status == 403) {
                response.getHeaders().add("WWW-Authenticate", "Basic realm=\"kura-rest-api\"");
            }
        }

    }

    private static class RequestCredentials {

        final String username;
        final String password;

        RequestCredentials(final String username, final String password) {
            this.username = username;
            this.password = password;
        }

        static RequestCredentials fromBasicAuthorizationHeader(final String authHeaderToken) {

            final String credentials = new String(BASE64_DECODER.decode(authHeaderToken), StandardCharsets.UTF_8);

            final int colonIndex = credentials.indexOf(':');

            String username = credentials.substring(0, colonIndex);
            String password = credentials.substring(colonIndex + 1);

            return new RequestCredentials(username, password);

        }
    }

}
