/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthenticationProviderHolder implements Comparable<AuthenticationProviderHolder>, AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderHolder.class);

    private final int priority;
    private final AuthenticationProvider wrapped;

    public AuthenticationProviderHolder(final AuthenticationProvider provider) {
        this.priority = getPriority(provider.getClass());
        this.wrapped = provider;
    }

    private static int getPriority(final Class<?> classz) {
        if (classz == Object.class) {
            return Integer.MAX_VALUE;
        }

        final Priority priorityAnnotation = classz.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {
            return priorityAnnotation.value();
        }

        return getPriority(classz.getSuperclass());
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {
        try {
            return wrapped.authenticate(request, requestContext);
        } catch (final Exception e) {
            logger.warn("Unexpected exception calling authentication provider", e);
            return Optional.empty();
        }
    }

    @Override
    public void onEnabled() {
        try {
            this.wrapped.onEnabled();
        } catch (final Exception e) {
            logger.warn("Unexpected exception enabling authentication provider", e);
        }
    }

    @Override
    public void onDisabled() {
        try {
            this.wrapped.onDisabled();
        } catch (final Exception e) {
            logger.warn("Unexpected exception disabling authentication provider", e);
        }
    }

    @Override
    public int compareTo(AuthenticationProviderHolder other) {
        return Integer.compare(this.priority, other.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, wrapped);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AuthenticationProviderHolder)) {
            return false;
        }
        AuthenticationProviderHolder other = (AuthenticationProviderHolder) obj;
        return priority == other.priority && Objects.equals(wrapped, other.wrapped);
    }

}