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

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.core.inventory.InventoryHandlerV1;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
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

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
        logger.error("roles activated - GREG");
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
     * Lists all the available inventory items.
     *
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/inventory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInventorySummary() {
        try {
            return makeInventoryRequest(buildKuraMessage(Arrays.asList(InventoryHandlerV1.INVENTORY), ""));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available bundles.
     *
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/bundles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBundles() {
        try {
            return makeInventoryRequest(buildKuraMessage(Arrays.asList(InventoryHandlerV1.RESOURCE_BUNDLES), ""));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Start selected bundle.
     *
     */
    @POST
    @RolesAllowed("inventory")
    @Path("/bundles/bundles/_start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startBundle(final String bundleJson) {
        try {
            return makeInventoryRequest(buildKuraMessage(InventoryHandlerV1.START_BUNDLE, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Start selected bundle.
     *
     */
    @POST
    @RolesAllowed("inventory")
    @Path("/bundles/bundles/_stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopBundle(final String bundleJson) {
        try {
            return makeInventoryRequest(buildKuraMessage(InventoryHandlerV1.STOP_BUNDLE, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private KuraMessage buildKuraMessage(List<String> requestObject, String body) {

        Map<String, Object> payloadProperties = new HashMap<>();
        payloadProperties.put(ARGS_KEY.value(), requestObject);

        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setBody(body.getBytes());

        return new KuraMessage(kuraPayload, payloadProperties);
    }

    private Response makeInventoryRequest(KuraMessage kuraMessage) throws KuraException {
        KuraMessage inventoryResponse = inventoryHandlerV1.doGet(null, kuraMessage);
        return Response.ok(inventoryResponse.getPayload().getBody()).build();
    }

}