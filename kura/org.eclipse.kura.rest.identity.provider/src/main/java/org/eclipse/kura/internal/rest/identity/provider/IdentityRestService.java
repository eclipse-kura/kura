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

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserConfigRequestDTO;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@Path("identity/v1")
public class IdentityRestService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityRestService.class);
    private static final String DEBUG_MESSSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "ID-V1";
    private static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private CryptoService cryptoService;

    private UserAdmin userAdmin;
    private IdentityService identityService;
    private ConfigurationService configurationService;
    private Gson gson = new Gson();

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
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(final UserDTO userName) {
        try {
            logger.debug(DEBUG_MESSSAGE, "createUser");
            this.identityService.createUser(userName.getUserName());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/users")
    public Response deleteUser(final UserDTO userName) {
        try {
            logger.debug(DEBUG_MESSSAGE, "deleteUser");
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
    public Set<String> getDefinedPermissions() {
        try {
            logger.debug(DEBUG_MESSSAGE, "getDefinedPermissions");
            return this.identityService.getDefinedPermissions();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/users/configs")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<UserDTO> getUserConfig() {
        try {
            logger.debug(DEBUG_MESSSAGE, "getUserConfig");
            return this.identityService.getUserConfig();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/users/configs")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setUserConfig(UserConfigRequestDTO userConfigrRequest) {
        try {
            logger.debug(DEBUG_MESSSAGE, "setUserConfig");
            Set<UserDTO> userConfig = userConfigrRequest.getUserConfig();

            for (final UserDTO config : userConfig) {
                final String newPassword = config.getPassword();

                if (newPassword != null) {
                    this.identityService.validateUserPassword(newPassword);
                }
            }
            this.identityService.setUserConfig(userConfig);
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

}
