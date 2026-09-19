/*******************************************************************************
 * Copyright (c) 2021, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.tamper.detection;

import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.rest.tamper.detection.util.TamperDetectionRemoteService;
import org.eclipse.kura.rest.tamper.detection.api.TamperDetectionServiceInfo;
import org.eclipse.kura.rest.tamper.detection.api.TamperStatusInfo;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
@Tag(name = "Tamper detection", description = "Requires rest.tamper.detection or kura.admin permission.")
@ApiResponses({
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthenticated"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
@Path("/tamper/v1")
@Component(
    name = "org.eclipse.kura.internal.rest.tamper.detection.TamperDetectionRestService",
    immediate = true,
    service = { org.eclipse.kura.internal.rest.tamper.detection.TamperDetectionRestService.class },
    property = { "osgi.jakartars.resource=true" })
public class TamperDetectionRestService extends TamperDetectionRemoteService {

    @Reference(name = "UserAdmin", service = org.osgi.service.useradmin.UserAdmin.class, unbind = "-")
    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole("kura.permission.rest.tamper.detection", Role.GROUP);
    }

    @GET
    @Path("/list")
    @RolesAllowed("tamper.detection")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "get_tamper_v1_list", summary = "List tamper-detection services",
            description = "Returns configured tamper-detection service information.")
    @ApiResponse(responseCode = "200", description = "Configured services.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = TamperDetectionServiceInfo.class))))
    public List<TamperDetectionServiceInfo> listTamperDetectionServices() {
        return listTamperDetectionServicesInternal();
    }

    @GET
    @RolesAllowed("tamper.detection")
    @Path("/pid/{pid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "get_tamper_v1_pid", summary = "Read tamper status",
            description = "Returns current tamper status for the named service PID.")
    @ApiResponse(responseCode = "200", description = "Current tamper status.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TamperStatusInfo.class)))
    public TamperStatusInfo getTamperStatus(@PathParam("pid") final String pid) {
        try {
            return getTamperStatusInternal(pid);
        } catch (final KuraException e) {
            throw toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed("tamper.detection")
    @Path("/pid/{pid}/_reset")
    @Operation(operationId = "post_tamper_v1_pid_reset", summary = "Reset tamper status",
            description = "Resets tamper status for the named service PID.")
    @ApiResponse(responseCode = "204", description = "Status reset; response body is empty.")
    public void resetTamperStatus(@PathParam("pid") final String pid) {
        try {
            resetTamperStatusInternal(pid);
        } catch (final KuraException e) {
            throw toWebApplicationException(e);
        }
    }

    private WebApplicationException toWebApplicationException(final KuraException e) {
        if (e.getCode() == KuraErrorCode.NOT_FOUND) {
            return new WebApplicationException(404);
        } else {
            return new WebApplicationException(e);
        }
    }
}
