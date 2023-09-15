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
package org.eclipse.kura.internal.rest.auth;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.PathSegment;

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.internal.rest.provider.RestServiceOptions;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(300)
public class SessionAuthProvider implements AuthenticationProvider {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final RestSessionHelper sessionHelper;
    private RestServiceOptions restServiceOptions;
    private final Set<String> lockedSessionAllowedPaths;
    private final Set<String> allowNoXsrfTokenPaths;

    public SessionAuthProvider(final RestSessionHelper sessionHelper, final Set<String> allowSessionLocked,
            final Set<String> allowNoXsrfToken) {
        this.sessionHelper = sessionHelper;
        this.lockedSessionAllowedPaths = allowSessionLocked;
        this.allowNoXsrfTokenPaths = allowNoXsrfToken;
    }

    @Override
    public void onEnabled() {
        // no need
    }

    @Override
    public void onDisabled() {
        // no need
    }

    public void setOptions(final RestServiceOptions restServiceOptions) {
        this.restServiceOptions = restServiceOptions;
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {

        final AuditContext auditContext = AuditContext.currentOrInternal();

        final Optional<HttpSession> session = this.sessionHelper.getExistingSession(request);

        if (!session.isPresent()) {
            return Optional.empty();
        }

        auditContext.getProperties().put("session.id", session.get().getId());

        final Optional<Principal> result = this.sessionHelper.getPrincipalFromSession(session.get());

        if (!result.isPresent()) {
            return Optional.empty();
        }

        auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), result.get().getName());

        if (!isXsrfTokenValid(request, requestContext)) {
            auditLogger.warn("{} Rest - Failure - Session authentication failed, invalid XSRF token",
                    auditContext);
            return Optional.empty();
        }

        if (this.sessionHelper
                .isSessionExpired(session.get(), this.restServiceOptions.getSessionInactivityInterval())) {
            auditLogger.warn("{} Rest - Failure - Session authentication failed, session expired",
                    auditContext);
            session.get().invalidate();
            return Optional.empty();
        }

        if (isSessionLocked(session.get(), requestContext)) {
            auditLogger.warn("{} Rest - Failure - Session authentication failed, session is locked",
                    auditContext);
            return Optional.empty();
        }

        if (sessionHelper.credentialsChanged(session.get(), result.get().getName())) {
            auditLogger.warn("{} Rest - Failure - Session authentication failed, user credentials changed",
                    auditContext);
            session.get().invalidate();
            return Optional.empty();
        }

        this.sessionHelper.updateLastActivity(session.get());

        auditLogger.info("{} Rest - Success - Authentication succeeded via session provider", auditContext);

        return result;
    }

    private boolean isSessionLocked(final HttpSession session, final ContainerRequestContext requestContext) {
        if (containsPath(lockedSessionAllowedPaths, requestContext)) {
            return false;
        }

        return this.sessionHelper.isSessionLocked(session);
    }

    private boolean isXsrfTokenValid(final HttpServletRequest request, final ContainerRequestContext context) {
        if (containsPath(allowNoXsrfTokenPaths, context)) {
            return true;
        }

        return this.sessionHelper.isXsrfTokenValid(request);
    }

    private boolean containsPath(final Set<String> paths, final ContainerRequestContext requestContext) {
        final String reuqestPath = '/' + requestContext.getUriInfo().getPathSegments().stream()
                .map(PathSegment::getPath)
                .collect(Collectors.joining("/"));

        return paths.contains(reuqestPath);
    }

}
