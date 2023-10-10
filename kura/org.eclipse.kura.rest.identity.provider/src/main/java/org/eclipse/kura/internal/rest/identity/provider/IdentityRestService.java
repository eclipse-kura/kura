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
 ******************************************************************************/
package org.eclipse.kura.internal.rest.identity.provider;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.identity.provider.dto.PermissionDTO;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserConfigDTO;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("identity/v1")
public class IdentityRestService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityRestService.class);
    private static final String DEBUG_MESSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "ID-V1";
    private static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private CryptoService cryptoService;

    private UserAdmin userAdmin;
    private IdentityService identityService;
    private ConfigurationService configurationService;

    public void bindCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void bindUserAdmin(UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
        this.userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(MQTT_APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", MQTT_APP_ID, e);
        }
    }

    // Added mainly for testing purposes. Currently the service is created by this endpoint.
    public void bindIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(MQTT_APP_ID);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", MQTT_APP_ID, e);
        }
    }

    public void activate() {
        // create only if not set externally. Added mainly for testing purposes.
        if (this.identityService == null) {
            this.identityService = new IdentityService(this.cryptoService, this.userAdmin, this.configurationService);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(final UserDTO userName) {
        try {
            logger.debug(DEBUG_MESSAGE, "createUser");
            this.identityService.createUser(userName);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(final UserDTO user) {
        try {
            logger.debug(DEBUG_MESSAGE, "updateUser");
            this.identityService.updateUser(user);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities/byName")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDTO getUser(final UserDTO userName) {
        try {
            logger.debug(DEBUG_MESSAGE, "getUser");
            return this.identityService.getUser(userName.getUserName());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUser(final UserDTO userName) {
        try {
            logger.debug(DEBUG_MESSAGE, "deleteUser");
            this.identityService.deleteUser(userName.getUserName());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/defined-permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public PermissionDTO getDefinedPermissions() {
        try {
            logger.debug(DEBUG_MESSAGE, "getDefinedPermissions");
            return new PermissionDTO(this.identityService.getDefinedPermissions());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Produces(MediaType.APPLICATION_JSON)
    public UserConfigDTO getUserConfig() {
        try {
            logger.debug(DEBUG_MESSAGE, "getUserConfig");
            UserConfigDTO userConfig = new UserConfigDTO();
            userConfig.setUserConfig(this.identityService.getUserConfig());
            return userConfig;
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
