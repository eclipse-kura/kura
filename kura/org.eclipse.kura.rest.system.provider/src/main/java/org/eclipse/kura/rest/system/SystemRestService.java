/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.system;

import static org.eclipse.kura.rest.system.Constants.KURA_PERMISSION_REST_ROLE;
import static org.eclipse.kura.rest.system.Constants.MQTT_APP_ID;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_EXTENDED_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_EXTENDED_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_FRAMEWORK_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_FRAMEWORK_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_KURA_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_KURA_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.REST_APP_ID;
import static org.eclipse.kura.rest.system.Constants.REST_ROLE_NAME;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.system.dto.ExtendedPropertiesDTO;
import org.eclipse.kura.rest.system.dto.FilterDTO;
import org.eclipse.kura.rest.system.dto.FrameworkPropertiesDTO;
import org.eclipse.kura.rest.system.dto.KuraPropertiesDTO;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(REST_APP_ID)
@Tag(name = "System", description = "System information and properties API for Eclipse Kura gateway")
@SecurityRequirement(name = "basicAuth")
@SuppressWarnings("all")
public class SystemRestService {

    private static final Logger logger = LoggerFactory.getLogger(SystemRestService.class);
    private static final String DEBUG_MESSSAGE = "Processing request for resource '{}'";

    private SystemService systemService;
    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    public void bindSystemService(SystemService systemService) {
        this.systemService = systemService;
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

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_FRAMEWORK_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Framework Properties", description = "Retrieves comprehensive system framework properties including hardware, Java runtime, OS, and Kura-specific information", operationId = "getFrameworkProperties")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Framework properties successfully retrieved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FrameworkPropertiesDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error") })
    public FrameworkPropertiesDTO getFrameworkProperties() {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_FRAMEWORK_PROPERTIES);
            return new FrameworkPropertiesDTO(this.systemService);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_EXTENDED_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    public ExtendedPropertiesDTO getExtendedProperties() {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_EXTENDED_PROPERTIES);
            return new ExtendedPropertiesDTO(this.systemService);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_KURA_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    public KuraPropertiesDTO getKuraProperties() {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_KURA_PROPERTIES);
            return new KuraPropertiesDTO(this.systemService.getProperties());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_FRAMEWORK_PROPERTIES_FILTER)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Filtered Framework Properties", description = "Retrieves system framework properties filtered by specific property names. Only returns the properties specified in the filter.", operationId = "getFilteredFrameworkProperties")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filtered framework properties successfully retrieved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FrameworkPropertiesDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid filter parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error") })
    public FrameworkPropertiesDTO postFrameworkPropertiesFilter(
            @RequestBody(description = "Filter criteria specifying which properties to include in the response", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FilterDTO.class))) FilterDTO filter) {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_FRAMEWORK_PROPERTIES_FILTER);
            return new FrameworkPropertiesDTO(this.systemService, filter.getNames());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_EXTENDED_PROPERTIES_FILTER)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ExtendedPropertiesDTO postExtendedPropertiesFilter(FilterDTO filter) {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_EXTENDED_PROPERTIES_FILTER);
            return new ExtendedPropertiesDTO(this.systemService, filter.getGroupNames());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_KURA_PROPERTIES_FILTER)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public KuraPropertiesDTO postKuraPropertiesFilter(FilterDTO filter) {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_KURA_PROPERTIES_FILTER);
            return new KuraPropertiesDTO(this.systemService.getProperties(), filter.getNames());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
