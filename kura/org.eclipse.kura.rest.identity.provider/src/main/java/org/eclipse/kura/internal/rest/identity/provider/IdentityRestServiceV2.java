/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider;

import java.util.Collections;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.internal.rest.identity.provider.util.IdentityDTOUtils;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("identity/v2")
public class IdentityRestServiceV2 extends AbstractIdentityRestService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityRestServiceV2.class);

    private static final String MQTT_APP_ID = "IDN-V2";

    private IdentityService identityService;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected String getMqttApplicationId() {
        return MQTT_APP_ID;
    }

    public void bindIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIdentity(final IdentityDTO identity) {

        boolean created = false;

        try {
            logger.debug(DEBUG_MESSAGE, "createIdentity");
            created = this.identityService.createIdentity(identity.getName());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return created ? Response.ok().build() : Response.status(Status.CONFLICT).build();
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateIdentity(final IdentityConfigurationDTO indentityConfigurationDTO) {
        try {
            logger.debug(DEBUG_MESSAGE, "updateIdentity");
            this.identityService.updateIdentityConfigurations(
                    Collections.singletonList(IdentityDTOUtils.toIdentityConfiguration(indentityConfigurationDTO)));
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

}
