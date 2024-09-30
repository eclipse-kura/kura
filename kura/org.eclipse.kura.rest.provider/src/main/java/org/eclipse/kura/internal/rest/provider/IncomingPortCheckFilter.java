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
import java.util.Collections;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
class IncomingPortCheckFilter implements ContainerRequestFilter {

    private static final Response NOT_FOUND_RESPONSE = Response.status(Response.Status.NOT_FOUND).build();

    @Context
    private HttpServletRequest request;

    private Set<Integer> allowedPorts = Collections.emptySet();

    public void setAllowedPorts(final Set<Integer> allowedPorts) {
        this.allowedPorts = allowedPorts;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        RestServiceUtils.initAuditContext(requestContext, request);

        if (allowedPorts.isEmpty()) {
            return;
        }

        final int port = request.getLocalPort();

        if (!allowedPorts.contains(port)) {
            requestContext.abortWith(NOT_FOUND_RESPONSE);
        }
    }

}