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
package org.eclipse.kura.internal.rest.position;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.position.api.IsLockedDTO;
import org.eclipse.kura.rest.position.api.LocalDateTimeDTO;
import org.eclipse.kura.rest.position.api.PositionDTO;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/position/v1")
public class PositionRestService {

    private static final Logger logger = LoggerFactory.getLogger(PositionRestService.class);

    private static final String APP_ID = "POS-V1";

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.position";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private PositionService positionServiceImpl;

    public void setPositionServiceImpl(PositionService positionServiceImpl) {
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
    @RolesAllowed("position")
    @Path("/position")
    @Produces(MediaType.APPLICATION_JSON)
    public PositionDTO getPosition() {
        if (positionServiceImpl.isLocked()) {
            return new PositionDTO(positionServiceImpl.getPosition());
        }

        throw DefaultExceptionHandler.toWebApplicationException(
                new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, "Position is not locked"));
    }

    /**
     * GET method.
     *
     * Get localDateTime.
     *
     * @return a list of long that represents the list of snapshots managed by the
     *         framework.
     */
    @GET
    @RolesAllowed("position")
    @Path("/localdatetime")
    @Produces(MediaType.APPLICATION_JSON)
    public LocalDateTimeDTO getLocalDateTime() {
        if (positionServiceImpl.isLocked()) {
            return new LocalDateTimeDTO(positionServiceImpl.getDateTime());
        }

        throw DefaultExceptionHandler.toWebApplicationException(
                new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, "Position is not locked"));

    }

    /**
     * GET method.
     *
     * Get returns true if a valid geographic position has been received by position
     * service.
     *
     * @return a list of long that represents the list of snapshots managed by the
     *         framework.
     */
    @GET
    @RolesAllowed("position")
    @Path("/islocked")
    @Produces(MediaType.APPLICATION_JSON)
    public IsLockedDTO getIsLocked() {
        try {
            return new IsLockedDTO(positionServiceImpl.isLocked());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }
}