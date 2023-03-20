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
package org.eclipse.kura.internal.network.status.provider;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.network.status.provider.api.InterfaceIdsDTO;
import org.eclipse.kura.network.status.provider.api.InterfaceStatusListDTO;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/networkStatus/v1")
public class NetworkStatusRestServiceImpl {

    private static final String KURA_PERMISSION_REST_NETWORK_STATUS_ROLE = "kura.permission.rest.network.status";

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatusRestServiceImpl.class);
    private static final String APP_ID = "NET-STATUS-V1";

    private NetworkStatusService networkStatusService;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

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

    public void setNetworkStatusService(final NetworkStatusService networkStatusService) {
        this.networkStatusService = networkStatusService;
    }

    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_NETWORK_STATUS_ROLE, Role.GROUP);
    }

    @GET
    @RolesAllowed("network.status")
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public InterfaceStatusListDTO getNetworkStatus() {
        return new InterfaceStatusListDTO(this.networkStatusService.getNetworkStatus());
    }

    @POST
    @RolesAllowed("network.status")
    @Path("/status/byInterfaceId")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public InterfaceStatusListDTO getNetworkStatus(final InterfaceIdsDTO interfaceIds) {

        final List<NetworkInterfaceStatus> result = new ArrayList<>();

        for (final String interfaceId : interfaceIds.getInterfaceIds()) {
            networkStatusService.getNetworkStatus(interfaceId).ifPresent(result::add);
        }

        return new InterfaceStatusListDTO(result);
    }

    @GET
    @RolesAllowed("network.status")
    @Path("/interfaceIds")
    @Produces(MediaType.APPLICATION_JSON)
    public InterfaceIdsDTO getInterfaceNames() {
        return new InterfaceIdsDTO(networkStatusService.getInterfaceIds());
    }

}
