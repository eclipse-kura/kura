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
import org.eclipse.kura.rest.position.api.DateTimeDTO;
import org.eclipse.kura.rest.position.api.IsLockedDTO;
import org.eclipse.kura.rest.position.api.PositionDTO;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/position/v1")
public class PositionRestService {

    private static final String POSITION_IS_NOT_LOCKED = "Position is not locked";

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
            logger.warn("failed to register {} request handler", APP_ID, e);
        }
    }

    public void unsetRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.warn("failed to unregister {} request handler", APP_ID, e);
        }
    }

    /**
     * GET method.
     *
     *
     * @return a PositionDTO that represents the current position. Values returned can be null.
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
                new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, POSITION_IS_NOT_LOCKED));
    }

    /**
     * GET method.
     *
     *
     * @return a DateTimeDTO that represents the current date and time. Date and time are represented in UTC.
     */
    @GET
    @RolesAllowed("position")
    @Path("/dateTime")
    @Produces(MediaType.APPLICATION_JSON)
    public DateTimeDTO getLocalDateTime() {
        if (positionServiceImpl.isLocked()) {
            return new DateTimeDTO(positionServiceImpl.getDateTime());
        }

        throw DefaultExceptionHandler.toWebApplicationException(
                new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, POSITION_IS_NOT_LOCKED));

    }

    /**
     * GET method.
     *
     *
     * @return IsLockedDTO which contains a boolean that represents the current lock status of the position service.
     */
    @GET
    @RolesAllowed("position")
    @Path("/isLocked")
    @Produces(MediaType.APPLICATION_JSON)
    public IsLockedDTO getIsLocked() {
        try {
            return new IsLockedDTO(positionServiceImpl.isLocked());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }
}