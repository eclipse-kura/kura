/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.inventory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.core.inventory.InventoryHandlerV1;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/inventory/v1")
public class InventoryRestService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryRestService.class);

    private static final String APP_ID = "INVEN-V1";

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.inventory";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private InventoryHandlerV1 inventoryHandlerV1;

    private ConfigurationService configurationService;
    private OCDService ocdService;
    private CryptoService cryptoService;

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
        logger.error("roles activated - GREG");
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setOCDService(OCDService ocdService) {
        this.ocdService = ocdService;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void setInventoryHandlerV1(InventoryHandlerV1 inventoryHandlerV1) {
        this.inventoryHandlerV1 = inventoryHandlerV1;

    }

    public void setRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("failed to register request handler", e);
        }
    }

    public void unsetRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.warn("failed to unregister request handler", e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available snapshots in the framework
     *
     * @return a list of long that represents the list of snapshots managed by the
     *         framework.
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/inventory")
    @Produces(MediaType.APPLICATION_JSON)
    public String listInventory() {
        logger.error("IT WORKED");

        return "{\"test\":\"string\"}";
    }
    
    /**
     * GET method.
     *
     * Lists all the available snapshots in the framework
     *
     * @return a list of long that represents the list of snapshots managed by the
     *         framework.
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/inventory")
    @Produces(MediaType.APPLICATION_JSON)
    public String listInventory() {
        logger.error("IT WORKED");

        return "{\"test\":\"string\"}";
    }
}
