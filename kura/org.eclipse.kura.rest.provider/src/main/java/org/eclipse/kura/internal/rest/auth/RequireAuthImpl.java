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

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.internal.rest.auth.annotation.RequireAuthenticatedUser;

@Provider
@Priority(Priorities.AUTHORIZATION + 10)
@RequireAuthenticatedUser
public class RequireAuthImpl implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        if (!getPrincipal(requestContext).isPresent()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
    }

    private Optional<Principal> getPrincipal(final ContainerRequestContext containerRequestContext) {
        return Optional.ofNullable(containerRequestContext.getSecurityContext())
                .flatMap(entry -> Optional.ofNullable(entry.getUserPrincipal()));
    }
}
