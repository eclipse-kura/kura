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
package org.eclipse.kura.internal.rest.linux.position;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.linux.position.PositionServiceImpl;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.linux.position.api.PositionDTO;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/linux/position/v1")
public class LinuxPositionRestService {

    private static final Logger logger = LoggerFactory.getLogger(LinuxPositionRestService.class);

    private static final String APP_ID = "POS-V1";

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.linux.position";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private PositionServiceImpl positionServiceImpl;

    public void setPositionServiceImpl(PositionServiceImpl positionServiceImpl) {
        this.positionServiceImpl = positionServiceImpl;
    }

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
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
     * Get position.
     *
     * @return a list of long that represents the list of snapshots managed by the
     *         framework.
     */
    @GET
    @RolesAllowed("configuration")
    @Path("/snapshots")
    @Produces(MediaType.APPLICATION_JSON)
    public PositionDTO listSnapshots() {
        return new PositionDTO(positionServiceImpl.getPosition());
    }
}