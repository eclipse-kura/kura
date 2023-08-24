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
package org.eclipse.kura.rest.system;

import static org.eclipse.kura.rest.system.Constants.KURA_PERMISSION_REST_ROLE;
import static org.eclipse.kura.rest.system.Constants.MQTT_APP_ID;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_BUNDLES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.REST_APP_ID;
import static org.eclipse.kura.rest.system.Constants.REST_ROLE_NAME;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.system.dto.BundlesDTO;
import org.eclipse.kura.rest.system.dto.FilterDTO;
import org.eclipse.kura.rest.system.dto.PropertiesDTO;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(REST_APP_ID)
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
    @Path(RESOURCE_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    public PropertiesDTO getProperties() {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_PROPERTIES);
            return new PropertiesDTO(this.systemService);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_PROPERTIES_FILTER)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PropertiesDTO postPropertiesFilter(FilterDTO filter) {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_PROPERTIES_FILTER);
            return new PropertiesDTO(this.systemService, filter.getNames());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path(RESOURCE_BUNDLES)
    @Produces(MediaType.APPLICATION_JSON)
    public BundlesDTO getBundles() {
        try {
            logger.debug(DEBUG_MESSSAGE, RESOURCE_BUNDLES);
            return new BundlesDTO(this.systemService.getBundles());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
