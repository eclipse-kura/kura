/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.IdentityTokenService;
import org.eclipse.kura.internal.rest.auth.dto.JwtDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Path("/jwt/v1")
public class JwtRestService {

    private static final String INVALID_SESSION_MESSAGE = "Current session is not valid";

    private final RestIdentityHelper identityHelper;
    private final IdentityTokenService tokenService;

    public JwtRestService(RestIdentityHelper identityHelper, IdentityTokenService tokenService) {
        this.identityHelper = identityHelper;
        this.tokenService = tokenService;
    }

    @GET
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    public JwtDTO issueNewToken(@Context final ContainerRequestContext requestContext) {
        final Optional<Principal> currentPrincipal = Optional.ofNullable(requestContext.getSecurityContext())
                .flatMap(c -> Optional.ofNullable(c.getUserPrincipal()));

        if (currentPrincipal.isEmpty()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
        }


        try {
            final String identityName = currentPrincipal.get().getName();
            
            if (!this.identityHelper.identityExists(identityName)) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                        INVALID_SESSION_MESSAGE);
            }

            final Duration ttl = Duration.ofMinutes(15);
            final Instant expirationTime = Instant.now().plus(ttl);
            final String token = this.tokenService.issueTokenFor(identityName, ttl);

            return new JwtDTO(token, expirationTime.toEpochMilli());
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }
}
