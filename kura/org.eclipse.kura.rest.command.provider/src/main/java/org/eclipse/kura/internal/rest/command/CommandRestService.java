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
package org.eclipse.kura.internal.rest.command;

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
import org.eclipse.kura.core.inventory.InventoryHandlerV1;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/command/v1")
public class CommandRestService {

    private static final Logger logger = LoggerFactory.getLogger(CommandRestService.class);

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.inventory";

    private InventoryHandlerV1 inventoryHandlerV1;

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
    }

    public void setInventoryHandlerV1(InventoryHandlerV1 inventoryHandlerV1) {
        this.inventoryHandlerV1 = inventoryHandlerV1;

    }

    /**
     * POST method.
     *
     * Start selected bundle.
     *
     */
    @POST
    @RolesAllowed("command")
    @Path("/command")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startBundle(final RestCommandRequest restCommandPayload) {
        try {
            return makeInventoryDoExecRequest(buildKuraMessage(InventoryHandlerV1.START_BUNDLE, bundleJson));
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private KuraMessage buildKuraMessage(RestCommandRequest restCommandPayload) {

        String METRIC_CMD = "command.command";
        String METRIC_ARG = "command.argument";
        String METRIC_ENVP = "command.environment.pair";
        String METRIC_DIR = "command.working.directory";
        String METRIC_STDIN = "command.stdin";
        String METRIC_TOUT = "command.timeout";
        String METRIC_ASYNC = "command.run.async";

        Map<String, Object> payloadProperties = new HashMap<>();
        payloadProperties.put(ARGS_KEY.value(), requestObject);

        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setZipBytes(body.getBytes());

        return new KuraMessage(kuraPayload, payloadProperties);
    }

}