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

package org.eclipse.kura.internal.rest.provider;

import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String KURA_PERMISSION_PREFIX = "kura.permission.";
    private static final String KURA_PERMISSION_REST_PREFIX = KURA_PERMISSION_PREFIX + "rest.";
    private static final String KURA_USER_PREFIX = "kura.user.";

    private final Set<AuthenticationProviderHolder> authenticationProviders = new TreeSet<>();
    private UserAdmin userAdmin;

    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    public void registerAuthenticationProvider(final AuthenticationProvider authenticationProvider) {
        synchronized (this.authenticationProviders) {
            final AuthenticationProviderHolder holder = new AuthenticationProviderHolder(authenticationProvider);
            this.authenticationProviders.add(holder);
            holder.onEnabled();
        }
    }

    public void unregisterAuthenticationProvider(final AuthenticationProvider authenticationProvider) {
        synchronized (this.authenticationProviders) {
            final AuthenticationProviderHolder holder = new AuthenticationProviderHolder(authenticationProvider);
            if (this.authenticationProviders.remove(holder)) {
                holder.onDisabled();
            }
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        RestServiceUtils.initAuditContext(requestContext, request);

        final Optional<Principal> principal = authenticate(requestContext);
        final boolean isSecure = requestContext.getUriInfo().getRequestUri().getScheme().equals("https");

        if (principal.isPresent()) {
            requestContext.setSecurityContext(new SecurityContext() {

                final Principal currentPrincipal = principal.get();

                @Override
                public String getAuthenticationScheme() {
                    return null;
                }

                @Override
                public Principal getUserPrincipal() {
                    return currentPrincipal;
                }

                @Override
                public boolean isSecure() {
                    return isSecure;
                }

                @Override
                public boolean isUserInRole(final String role) {
                    return AuthenticationFilter.this.isUserInRole(currentPrincipal, role);
                }
            });
        }
    }

    private Optional<Principal> authenticate(final ContainerRequestContext requestContext) {
        synchronized (this.authenticationProviders) {
            for (final AuthenticationProviderHolder provider : this.authenticationProviders) {
                final Optional<Principal> principal = provider.authenticate(request, requestContext);

                if (principal.isPresent()) {
                    return principal;
                }
            }
        }

        return Optional.empty();
    }

    private boolean isUserInRole(final Principal requestUser, final String role) {

        try {
            final User user = (User) this.userAdmin.getRole(KURA_USER_PREFIX + requestUser.getName());

            return containsBasicMember(this.userAdmin.getRole(KURA_PERMISSION_REST_PREFIX + role), user)
                    || containsBasicMember(this.userAdmin.getRole(KURA_PERMISSION_PREFIX + "kura.admin"), user);

        } catch (final Exception e) {
            return false;
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

    public void close() {
        synchronized (this.authenticationProviders) {
            final Iterator<AuthenticationProviderHolder> iter = this.authenticationProviders.iterator();
            while (iter.hasNext()) {
                iter.next().onDisabled();
                iter.remove();
            }
        }
    }
}
