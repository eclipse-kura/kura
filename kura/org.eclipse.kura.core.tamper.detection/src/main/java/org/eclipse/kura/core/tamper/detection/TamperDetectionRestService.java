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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.tamper.detection.model.TamperDetectionServiceInfo;
import org.eclipse.kura.core.tamper.detection.model.TamperStatusInfo;
import org.eclipse.kura.core.tamper.detection.util.TamperDetectionRemoteService;

@Path("/tamper")
public class TamperDetectionRestService extends TamperDetectionRemoteService {

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
