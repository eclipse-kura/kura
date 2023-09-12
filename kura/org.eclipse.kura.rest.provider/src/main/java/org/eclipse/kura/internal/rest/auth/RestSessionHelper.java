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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.util.useradmin.UserAdminHelper;

@SuppressWarnings("restriction")
public class RestSessionHelper {

    private final UserAdminHelper userAdminHelper;

    public RestSessionHelper(final UserAdminHelper userAdminHelper) {
        this.userAdminHelper = userAdminHelper;
    }

    public HttpSession createNewAuthenticatedSession(final HttpServletRequest request, final String user) {

        final Optional<HttpSession> existingSession = getExistingSession(request);

        if (existingSession.isPresent()) {
            existingSession.get().invalidate();
        }

        final HttpSession newSession = request.getSession(true);

        newSession.setAttribute(SessionAttributes.AUTORIZED_USER.getValue(), user);
        updateLastActivity(newSession);

        final Optional<Integer> credentialsHash = userAdminHelper.getCredentialsHash(user);

        if (credentialsHash.isPresent()) {
            newSession.setAttribute(SessionAttributes.CREDENTIALS_HASH.getValue(),
                    credentialsHash.get());
        }

        getOrCreateXsrfToken(newSession);

        AuditContext.currentOrInternal().getProperties().put("session.id", newSession.getId());

        return newSession;
    }

    public void lockSession(final HttpSession session) {
        session.setAttribute(SessionAttributes.LOCKED.getValue(), true);
    }

    public void unlockSession(final HttpSession session) {
        session.setAttribute(SessionAttributes.LOCKED.getValue(), false);
    }

    public boolean isSessionLocked(final HttpSession session) {
        return Objects.equals(true, session.getAttribute(SessionAttributes.LOCKED.getValue()));
    }

    public void updateLastActivity(final HttpSession session) {

        session.setAttribute(SessionAttributes.LAST_ACTIVITY.getValue(), System.nanoTime());
    }

    public Optional<Principal> getPrincipalFromSession(final HttpSession session) {

        final Object authorized = session.getAttribute(SessionAttributes.AUTORIZED_USER.getValue());

        if (!(authorized instanceof String)) {
            return Optional.empty();
        }

        final String userName = (String) authorized;

        return Optional.of(principalForIdentity(userName));
    }

    public boolean credentialsChanged(final HttpSession session, final String userName) {
        return !Objects.equals(
                session.getAttribute(SessionAttributes.CREDENTIALS_HASH.getValue()),
                userAdminHelper.getCredentialsHash(userName).orElse(null));
    }

    public Optional<Principal> getCurrentPrincipal(final ContainerRequestContext context) {
        return Optional.ofNullable(context.getSecurityContext())
                .flatMap(c -> Optional.ofNullable(c.getUserPrincipal()));
    }

    public boolean isSessionExpired(final HttpSession session, final int maxInactiveInterval) {

        final long now = System.nanoTime();

        if (!session.isNew()) {
            final long lastActivity = getLastActivity(session);

            final long delta = now - lastActivity;
            if (maxInactiveInterval > 0 && delta > TimeUnit.SECONDS.toNanos(maxInactiveInterval)) {
                session.invalidate();
                return true;
            }
        }

        return false;
    }

    public Optional<String> getXsrfToken(final HttpSession httpSession) {
        return Optional
                .ofNullable(httpSession.getAttribute(SessionAttributes.XSRF_TOKEN.getValue()))
                .flatMap(t -> t instanceof String ? Optional.of((String) t) : Optional.empty());
    }

    public String getOrCreateXsrfToken(final HttpSession httpSession) {
        final Optional<String> existiongToken = getXsrfToken(httpSession);

        if (existiongToken.isPresent()) {
            return existiongToken.get();
        }

        final UUID token = UUID.randomUUID();

        final String asString = token.toString();

        httpSession.setAttribute(SessionAttributes.XSRF_TOKEN.getValue(), asString);

        return asString;
    }

    public boolean isXsrfTokenValid(final HttpServletRequest httpServletRequest) {

        return checkXsrfToken(Optional.ofNullable(httpServletRequest.getHeader("X-XSRF-Token")), httpServletRequest);
    }

    public boolean checkXsrfToken(final Optional<String> userToken, final HttpServletRequest httpServletRequest) {

        final Optional<HttpSession> session = getExistingSession(httpServletRequest);

        if (!userToken.isPresent() || !session.isPresent()) {
            return false;
        }

        return Objects.equals(userToken, getXsrfToken(session.get()));
    }

    public void logout(final HttpServletRequest request, final HttpServletResponse response) {

        final Optional<HttpSession> maybeSession = getExistingSession(request);

        if (!maybeSession.isPresent()) {
            return;
        }

        final HttpSession session = maybeSession.get();

        session.invalidate();

        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            cookie.setMaxAge(0);
            cookie.setValue(null);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
    }

    public Optional<HttpSession> getExistingSession(final HttpServletRequest request) {
        return Optional.ofNullable(request.getSession(false));
    }

    private static Principal principalForIdentity(final String name) {
        return () -> name;
    }

    private static long getLastActivity(final HttpSession session) {
        final Object lastActivityRaw = session.getAttribute(SessionAttributes.LAST_ACTIVITY.getValue());

        if (!(lastActivityRaw instanceof Long)) {
            return 0;
        }

        return (long) lastActivityRaw;
    }
}
