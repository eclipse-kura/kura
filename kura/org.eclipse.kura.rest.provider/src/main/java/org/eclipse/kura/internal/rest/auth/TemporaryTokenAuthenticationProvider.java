/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.TemporaryIdentityService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication provider that validates temporary tokens issued for container instances.
 * This provider extracts tokens from the Authorization header and validates them
 * against the TemporaryIdentityService.
 */
@Priority(150) // Between certificate (100) and password (200) authentication
public class TemporaryTokenAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(TemporaryTokenAuthenticationProvider.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String KURA_TOKEN_PREFIX = "Kura-Token ";

    private TemporaryIdentityService temporaryIdentityService;

    public void setTemporaryIdentityService(final TemporaryIdentityService temporaryIdentityService) {
        this.temporaryIdentityService = temporaryIdentityService;
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request, 
            final ContainerRequestContext requestContext) {
        if (this.temporaryIdentityService == null) {
            return Optional.empty();
        }

        final String token = extractToken(request);
        if (token == null) {
            return Optional.empty();
        }

        try {
            final String identityName = this.temporaryIdentityService.validateTemporaryToken(token);
            return Optional.of(new TemporaryTokenPrincipal(identityName, token));
        } catch (KuraException e) {
            logger.debug("Failed to validate temporary token", e);
            return Optional.empty();
        }
    }

    @Override
    public void onEnabled() {
        logger.info("Temporary token authentication provider enabled");
    }

    @Override
    public void onDisabled() {
        logger.info("Temporary token authentication provider disabled");
    }

    private String extractToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }

        if (authHeader.startsWith(BEARER_PREFIX)) {
            final String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (token.startsWith("kura-temp-")) {
                return token;
            }
        } else if (authHeader.startsWith(KURA_TOKEN_PREFIX)) {
            return authHeader.substring(KURA_TOKEN_PREFIX.length()).trim();
        }

        return null;
    }

    /**
     * Principal implementation that holds temporary token information.
     */
    private static class TemporaryTokenPrincipal implements Principal {
        private final String name;
        private final String token;

        public TemporaryTokenPrincipal(String name, String token) {
            this.name = name;
            this.token = token;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getToken() {
            return token;
        }
    }
}