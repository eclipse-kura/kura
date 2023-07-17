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
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.app.command.CommandCloudApp;
import org.eclipse.kura.cloud.app.command.KuraCommandRequestPayload;
import org.eclipse.kura.cloud.app.command.KuraCommandResponsePayload;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.rest.command.api.RestCommandRequest;
import org.eclipse.kura.rest.command.api.RestCommandResponse;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/command/v1")
public class CommandRestService {

    private static final Logger logger = LoggerFactory.getLogger(CommandRestService.class);

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.command";

    private static final String PASSWORD_METRIC_NAME = "command.password";
    public static final String RESOURCE_COMMAND = "command";

    private CommandCloudApp commandCloudApp;

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
    }

    public void setCommandCloudApp(CommandCloudApp commandCloudApp) {
        this.commandCloudApp = commandCloudApp;

    }

    /**
     * POST method.
     *
     * Run command with Executor service.
     *
     */
    @POST
    @RolesAllowed("command")
    @Path("/command")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestCommandResponse execCommand(final RestCommandRequest restCommandPayload) {
        try {
            return doExecCommand(restCommandPayload);
        } catch (KuraException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Run command with Async Executor service.
     *
     */
    @POST
    @RolesAllowed("command")
    @Path("/command/async")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response execAsyncCommand(final RestCommandRequest restCommandPayload) {
        try {
            return doExecAsyncCommand(restCommandPayload);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private RestCommandResponse doExecCommand(RestCommandRequest restCommandPayload) throws KuraException {
        return buildRestCommandResponse(this.commandCloudApp.doExec(null, buildKuraMessage(restCommandPayload, false)));
    }

    private Response doExecAsyncCommand(RestCommandRequest restCommandPayload) throws KuraException {
        try {
            this.commandCloudApp.doExec(null, buildKuraMessage(restCommandPayload, true));
            return Response.accepted().build();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private KuraMessage buildKuraMessage(RestCommandRequest restCommandPayload, boolean isAsync) {

        KuraCommandRequestPayload kuraCommandRequestPayload = new KuraCommandRequestPayload(
                restCommandPayload.getCommand());

        kuraCommandRequestPayload.addMetric(PASSWORD_METRIC_NAME, restCommandPayload.getPassword());
        kuraCommandRequestPayload.setZipBytes(restCommandPayload.getZipBytesAsByteArray());

        kuraCommandRequestPayload.setWorkingDir(restCommandPayload.getWorkingDirectory());
        kuraCommandRequestPayload.setArguments(restCommandPayload.getArguments());
        kuraCommandRequestPayload.setEnvironmentPairs(restCommandPayload.getEnvironmentPairsAsStringArray());

        kuraCommandRequestPayload.setRunAsync(isAsync);

        Map<String, Object> payloadProperties = new HashMap<>();
        payloadProperties.put(ARGS_KEY.value(), Arrays.asList(RESOURCE_COMMAND));

        return new KuraMessage(kuraCommandRequestPayload, payloadProperties);
    }

    private RestCommandResponse buildRestCommandResponse(KuraMessage kuraMessage) throws KuraException {

        if (kuraMessage.getPayload() instanceof KuraCommandResponsePayload) {
            RestCommandResponse restCommandResponse = new RestCommandResponse();
            KuraCommandResponsePayload kuraCommandResponsePayload = (KuraCommandResponsePayload) kuraMessage
                    .getPayload();

            restCommandResponse.setStdout(kuraCommandResponsePayload.getStdout());
            restCommandResponse.setStderr(kuraCommandResponsePayload.getStderr());
            restCommandResponse.setExitCode(kuraCommandResponsePayload.getExitCode());
            restCommandResponse.setIsTimeOut(kuraCommandResponsePayload.isTimedout());

            return restCommandResponse;
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

}