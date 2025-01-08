/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.tamper.detection;

import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.tamper.detection.model.TamperDetectionServiceInfo;
import org.eclipse.kura.core.tamper.detection.model.TamperStatusInfo;
import org.eclipse.kura.core.tamper.detection.util.TamperDetectionRemoteService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@Path("/tamper/v1")
public class TamperDetectionRestService extends TamperDetectionRemoteService {

    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole("kura.permission.rest.tamper.detection", Role.GROUP);
    }

    @GET
    @Path("/list")
    @RolesAllowed("tamper.detection")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TamperDetectionServiceInfo> listTamperDetectionServices() {
        return listTamperDetectionServicesInternal();
    }

    @GET
    @RolesAllowed("tamper.detection")
    @Path("/pid/{pid}")
    @Produces(MediaType.APPLICATION_JSON)
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
