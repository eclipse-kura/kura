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
 ******************************************************************************/
package org.eclipse.kura.internal.rest.security.provider;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.security.provider.dto.DebugEnabledDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.security.SecurityService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

public abstract class AbstractRestSecurityService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRestSecurityService.class);
    protected static final String DEBUG_MESSAGE = "Processing request for method '{}'";

    protected static final String REST_ROLE_NAME = "security";
    protected static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    protected SecurityService security;
    protected final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    public void bindSecurityService(SecurityService securityService) {
        this.security = securityService;
    }

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(getMqttAppId(), this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", getMqttAppId(), e);
        }
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(getMqttAppId());
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", getMqttAppId(), e);
        }
    }

    public abstract String getMqttAppId();

    /**
     * POST method <br /> This method allows the reload of the security policy's fingerprint
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy-fingerprint/reload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadSecurityPolicyFingerprint() {
        try {
            logger.debug(DEBUG_MESSAGE, "reloadSecurityPolicyFingerprint");
            this.security.reloadSecurityPolicyFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * POST method <br /> This method allows the reload of the command line fingerprint
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/command-line-fingerprint/reload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadCommandLineFingerprint() {
        try {
            logger.debug(DEBUG_MESSAGE, "reloadCommandLineFingerprint");
            this.security.reloadCommandLineFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * GET method
     *
     * @return true if the debug is permitted. False otherwise.
     */
    @GET
    @Path("/debug-enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public DebugEnabledDTO isDebugEnabled(final @Context ContainerRequestContext context) {
        try {

            if (context != null && !Optional.ofNullable(context.getSecurityContext())
                    .filter(c -> c.getUserPrincipal() != null).isPresent()) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            logger.debug(DEBUG_MESSAGE, "isDebugEnabled");
            return new DebugEnabledDTO(this.security.isDebugEnabled());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }
}
