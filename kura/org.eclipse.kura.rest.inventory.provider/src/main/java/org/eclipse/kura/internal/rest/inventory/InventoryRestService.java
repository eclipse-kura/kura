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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.inventory.InventoryHandlerV1;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/inventory/v1")
public class InventoryRestService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryRestService.class);

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.inventory";

    private InventoryHandlerV1 inventoryHandlerV1;

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
    }

    public void setInventoryHandlerV1(InventoryHandlerV1 inventoryHandlerV1) {
        this.inventoryHandlerV1 = inventoryHandlerV1;

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
            return makeInventoryDoGetRequest(buildKuraMessage(Arrays.asList(InventoryHandlerV1.INVENTORY), ""));
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
            return makeInventoryDoGetRequest(buildKuraMessage(Arrays.asList(InventoryHandlerV1.RESOURCE_BUNDLES), ""));
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
    @Path("/bundles/_start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startBundle(final String bundleJson) {
        try {
            return makeInventoryDoExecRequest(buildKuraMessage(InventoryHandlerV1.START_BUNDLE, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Stop selected bundle.
     *
     */
    @POST
    @RolesAllowed("inventory")
    @Path("/bundles/_stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopBundle(final String bundleJson) {
        try {
            return makeInventoryDoExecRequest(buildKuraMessage(InventoryHandlerV1.STOP_BUNDLE, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available Deployment Packages.
     *
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/deploymentPackages")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeploymentPackages() {
        try {
            return makeInventoryDoGetRequest(
                    buildKuraMessage(Arrays.asList(InventoryHandlerV1.RESOURCE_DEPLOYMENT_PACKAGES), ""));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available System Packages.
     *
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/systemPackages")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemPackages() {
        try {
            return makeInventoryDoGetRequest(
                    buildKuraMessage(Arrays.asList(InventoryHandlerV1.RESOURCE_SYSTEM_PACKAGES), ""));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available containers.
     *
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/containers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContainers() {
        try {
            return makeInventoryDoGetRequest(
                    buildKuraMessage(Arrays.asList(InventoryHandlerV1.RESOURCE_DOCKER_CONTAINERS), ""));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Start selected container.
     *
     */
    @POST
    @RolesAllowed("inventory")
    @Path("/containers/_start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startContainer(final String bundleJson) {
        try {
            return makeInventoryDoExecRequest(buildKuraMessage(InventoryHandlerV1.START_CONTAINER, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Stop selected container.
     *
     */
    @POST
    @RolesAllowed("inventory")
    @Path("/containers/_stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopContainer(final String bundleJson) {
        try {
            return makeInventoryDoExecRequest(buildKuraMessage(InventoryHandlerV1.STOP_CONTAINER, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available container images.
     *
     */
    @GET
    @RolesAllowed("inventory")
    @Path("/images")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImages() {
        try {
            return makeInventoryDoGetRequest(
                    buildKuraMessage(Arrays.asList(InventoryHandlerV1.RESOURCE_CONTAINER_IMAGES), ""));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Delete selected container image.
     *
     */
    @POST
    @RolesAllowed("inventory")
    @Path("/images/_delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteImage(final String bundleJson) {
        try {
            return makeInventoryDoExecRequest(buildKuraMessage(InventoryHandlerV1.DELETE_IMAGE, bundleJson));
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

    private Response makeInventoryDoGetRequest(KuraMessage kuraMessage) throws KuraException {
        return buildResponse(inventoryHandlerV1.doGet(null, kuraMessage).getPayload());
    }

    private Response makeInventoryDoExecRequest(KuraMessage kuraMessage) throws KuraException {
        return buildResponse(inventoryHandlerV1.doExec(null, kuraMessage).getPayload());
    }

    private Response buildResponse(KuraPayload kuraPayload) throws KuraException {
        if (kuraPayload instanceof KuraResponsePayload) {
            KuraResponsePayload kuraResponsePayload = (KuraResponsePayload) kuraPayload;
            return Response.status(kuraResponsePayload.getResponseCode()).entity(kuraPayload.getBody()).build();
        } else {
            throw new KuraException(KuraErrorCode.INVALID_MESSAGE_EXCEPTION);
        }

    }

}