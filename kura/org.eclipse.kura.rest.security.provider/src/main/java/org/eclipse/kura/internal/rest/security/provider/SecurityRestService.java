/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Path("security/v1")
public class SecurityRestService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityRestService.class);
    private static final String DEBUG_MESSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "SEC-V1";
    private static final String REST_ROLE_NAME = "security";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private SecurityService security;
    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    public void bindSecurityService(SecurityService securityService) {
        this.security = securityService;
    }

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(MQTT_APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", MQTT_APP_ID, e);
        }
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(MQTT_APP_ID);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", MQTT_APP_ID, e);
        }
    }

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
     * POST method <br /> This method replaces the security policy with the default production one. Then a fingerprint
     * reload is performed.
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy/load-default-production")
    public Response loadDefaultProductionSecurityPolicy() {
        try {
            logger.debug(DEBUG_MESSAGE, "loadDefaultProductionSecurityPolicy");

            this.security.loadDefaultProductionSecurityPolicy();
            this.security.reloadSecurityPolicyFingerprint();
            this.security.reloadCommandLineFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * POST method <br /> This method replaces the security policy with the provided one. Then a fingerprint reload is
     * performed.
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy/load")
    public Response loadSecurityPolicy(InputStream securityPolicyInputStream) {
        try {
            logger.debug(DEBUG_MESSAGE, "loadSecurityPolicy");

            this.security.loadSecurityPolicy(readSecurityPolicyString(securityPolicyInputStream));
            this.security.reloadSecurityPolicyFingerprint();
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
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            logger.debug(DEBUG_MESSAGE, "isDebugEnabled");
            return new DebugEnabledDTO(this.security.isDebugEnabled());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    private String readSecurityPolicyString(InputStream securityPolicyInputStream) throws IOException {
        if (securityPolicyInputStream == null) {
            throw new IllegalArgumentException("Security Policy cannot be null or empty");
        }
        int bytesRead;
        int chunksRead = 0;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        while ((bytesRead = securityPolicyInputStream.read(data, 0, data.length)) != -1) {
            if (chunksRead++ > 1024) {
                throw new IllegalArgumentException("Security policy too large");
            }
            buffer.write(data, 0, bytesRead);
        }
        buffer.flush();
        if (buffer.size() == 0) {
            throw new IllegalArgumentException("Security Policy cannot be null or empty");
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
