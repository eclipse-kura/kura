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
 ******************************************************************************/
package org.eclipse.kura.internal.rest.cloudconnection.provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentFactories;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentInstances;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionFactoryPidAndCloudEndpointPid;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEndpointPidRequest;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConnectedStatus;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PidAndFactoryPidAndCloudEndpointPid;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPid;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.eclipse.kura.rest.configuration.api.UpdateComponentConfigurationRequest;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("cloudconnection/v1")
public class CloudConnectionRestService {

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionRestService.class);

    private static final String MQTT_APP_ID = "CLD-V1";
    private static final String REST_ROLE_NAME = "cloudconnection";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private CloudConnectionService cloudConnectionService;
    private CloudConnectionManagerBridge cloudConnectionManagerBridge;
    private ConfigurationService configurationService;
    private CryptoService cryptoService;

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(MQTT_APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", MQTT_APP_ID, e);
        }
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(MQTT_APP_ID);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", MQTT_APP_ID, e);
        }
    }

    public void activate() {
        this.cloudConnectionService = new CloudConnectionService(this.configurationService);
        this.cloudConnectionManagerBridge = new CloudConnectionManagerBridge();
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public CloudComponentInstances findCloudComponentInstances() {
        try {
            return new CloudComponentInstances(this.cloudConnectionService.findCloudEndpointInstances(),
                    this.cloudConnectionService.findPubsubInstances());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudEndpoint/stackComponentPids")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getStackComponentsPids(
            final CloudConnectionFactoryPidAndCloudEndpointPid cloudConnectionFactoryPidAndCloudEndpointPid) {
        try {
            Set<String> pidSet = this.cloudConnectionService.getStackComponentsPids(
                    cloudConnectionFactoryPidAndCloudEndpointPid.getCloudConnectionFactoryPid(),
                    cloudConnectionFactoryPidAndCloudEndpointPid.getCloudEndpointPid());

            return new PidSet(pidSet);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudEndpoint")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCloudEndpoint(
            final CloudConnectionFactoryPidAndCloudEndpointPid cloudConnectionFactoryPidAndCloudEndpointPid) {
        try {
            this.cloudConnectionService.createCloudEndpointFromFactory(
                    cloudConnectionFactoryPidAndCloudEndpointPid.getCloudConnectionFactoryPid(),
                    cloudConnectionFactoryPidAndCloudEndpointPid.getCloudEndpointPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudEndpoint")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteCloudEndpoint(
            final CloudConnectionFactoryPidAndCloudEndpointPid cloudConnectionFactoryPidAndCloudEndpointPid) {
        try {
            this.cloudConnectionService.deleteCloudEndpointFromFactory(
                    cloudConnectionFactoryPidAndCloudEndpointPid.getCloudConnectionFactoryPid(),
                    cloudConnectionFactoryPidAndCloudEndpointPid.getCloudEndpointPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/factories")
    @Produces(MediaType.APPLICATION_JSON)
    public CloudComponentFactories getCloudComponentFactories() {
        try {
            return this.cloudConnectionService.getCloudComponentFactories();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/pubSub")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPubSubInstance(
            final PidAndFactoryPidAndCloudEndpointPid pidAndFactoryPidAndCloudEndpointPid) {
        try {
            this.cloudConnectionService.createPubSubInstance(pidAndFactoryPidAndCloudEndpointPid.getPid(),
                    pidAndFactoryPidAndCloudEndpointPid.getFactoryPid(),
                    pidAndFactoryPidAndCloudEndpointPid.getCloudEndpointPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/pubSub")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePubSubInstance(final PidAndFactoryPid pidAndFactoryPid) {
        try {
            this.cloudConnectionService.deletePubSubInstance(pidAndFactoryPid.getPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/configurations")
    @Consumes(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getConfigurations(final PidSet pidSet) {
        try {
            List<ComponentConfiguration> result = this.cloudConnectionService.getPubSubConfiguration(pidSet.getPids());

            result.addAll(this.cloudConnectionService.getStackConfigurationsByPid(pidSet.getPids()));

            return new ComponentConfigurationList(
                    result.stream().map(c -> DTOUtil.toComponentConfigurationDTO(c, this.cryptoService, false)
                            .replacePasswordsWithPlaceholder()).collect(Collectors.toList()));

        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/configurations")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateStackComponentConfigurations(
            UpdateComponentConfigurationRequest updateComponentConfigurationRequest) {
        try {
            this.cloudConnectionService.updateStackComponentConfiguration(
                    updateComponentConfigurationRequest.getComponentConfigurations(),
                    updateComponentConfigurationRequest.isTakeSnapshot());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudEndpoint/connect")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response connectCloudEndpoint(CloudEndpointPidRequest cloudEndpointPid) {
        try {
            this.cloudConnectionManagerBridge.connectCloudEndpoint(cloudEndpointPid.getCloudEndpointPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudEndpoint/disconnect")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response disconnectCloudEndpoint(CloudEndpointPidRequest cloudEndpointPid) {
        try {
            this.cloudConnectionManagerBridge.disconnectCloudEndpoint(cloudEndpointPid.getCloudEndpointPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudEndpoint/isConnected")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectedStatus isConnectedCloudEndpoint(CloudEndpointPidRequest cloudEndpointPid) {
        try {
            return new ConnectedStatus(
                    this.cloudConnectionManagerBridge.isConnectedCloudEndpoint(cloudEndpointPid.getCloudEndpointPid()));
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
