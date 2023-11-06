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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentFactories;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEntries;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.Connected;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConnectionId;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.FactoryPidAndCloudServicePid;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PidAndFactoryPidAndCloudConnectionPid;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPid;
import org.eclipse.kura.rest.configuration.api.UpdateComponentConfigurationRequest;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("cloudconnection/v1")
public class CloudConnectionRestService {

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionRestService.class);
    private static final String DEBUG_MESSSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "CC-V1";
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
    @Path("/cloudEntries")
    @Produces(MediaType.APPLICATION_JSON)
    public CloudEntries findCloudEntries() {
        try {
            logger.debug(DEBUG_MESSSAGE, "findCloudEntries");
            return new CloudEntries(this.cloudConnectionService.findCloudEntries());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/stackConfigurations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getStackConfigurationsByFactory(
            final FactoryPidAndCloudServicePid factoryPidAndCloudServicePid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "getStackConfigurationsByFactory");
            List<ComponentConfiguration> cloudStackConfigurations = this.cloudConnectionService
                    .getStackConfigurationsByFactory(factoryPidAndCloudServicePid.getFactoryPid(),
                            factoryPidAndCloudServicePid.getCloudServicePid());

            return DTOUtil.toComponentConfigurationList(cloudStackConfigurations, this.cryptoService, false)
                    .replacePasswordsWithPlaceholder();

        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudService")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createCloudServiceFromFactory(final FactoryPidAndCloudServicePid factoryPidAndCloudServicePid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "createCloudServiceFromFactory");
            this.cloudConnectionService.createCloudServiceFromFactory(factoryPidAndCloudServicePid.getFactoryPid(),
                    factoryPidAndCloudServicePid.getCloudServicePid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudService")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteCloudServiceFromFactory(final FactoryPidAndCloudServicePid factoryPidAndCloudServicePid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "deleteCloudServiceFromFactory");
            this.cloudConnectionService.deleteCloudServiceFromFactory(factoryPidAndCloudServicePid.getFactoryPid(),
                    factoryPidAndCloudServicePid.getCloudServicePid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudComponentFactories")
    @Produces(MediaType.APPLICATION_JSON)
    public CloudComponentFactories getCloudComponentFactories() {
        try {
            logger.debug(DEBUG_MESSSAGE, "getCloudComponentFactories");
            return this.cloudConnectionService.getCloudComponentFactories();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/pubSubInstance")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createPubSubInstance(
            final PidAndFactoryPidAndCloudConnectionPid pidAndFactoryPidAndCloudConnectionPid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "createPubSubInstance");
            this.cloudConnectionService.createPubSubInstance(pidAndFactoryPidAndCloudConnectionPid.getPid(),
                    pidAndFactoryPidAndCloudConnectionPid.getFactoryPid(),
                    pidAndFactoryPidAndCloudConnectionPid.getCloudConnectionPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/pubSubInstance")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deletePubSubInstance(final PidAndFactoryPid pidAndFactoryPid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "deletePubSubInstance");
            this.cloudConnectionService.deletePubSubInstance(pidAndFactoryPid.getPid());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/pubSubInstance/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public ComponentConfigurationDTO getPubSubConfiguration(final PidAndFactoryPid pidAndFactoryPid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "getPubSubConfiguration");
            return DTOUtil.toComponentConfigurationDTO(
                    this.cloudConnectionService.getPubSubConfiguration(pidAndFactoryPid.getPid()), this.cryptoService,
                    false).replacePasswordsWithPlaceholder();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/stackConfigurations")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateStackComponentConfiguration(
            UpdateComponentConfigurationRequest updateComponentConfigurationRequest) {
        try {
            logger.debug(DEBUG_MESSSAGE, "updateStackComponentConfiguration");
            this.cloudConnectionService.updateStackComponentConfiguration(
                    updateComponentConfigurationRequest.getComponentConfigurations());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudConnectionManager/connect")
    @Consumes(MediaType.APPLICATION_JSON)
    public void connectDataService(ConnectionId connectionIdDTO) {
        try {
            logger.debug(DEBUG_MESSSAGE, "connectDataService");
            this.cloudConnectionManagerBridge.connectDataService(connectionIdDTO.getConnectionId());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudConnectionManager/disconnect")
    @Consumes(MediaType.APPLICATION_JSON)
    public void disconnectDataService(ConnectionId connectionIdDTO) {
        try {
            logger.debug(DEBUG_MESSSAGE, "disconnectDataService");
            this.cloudConnectionManagerBridge.disconnectDataService(connectionIdDTO.getConnectionId());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/cloudConnectionManager/isConnected")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Connected isConnected(ConnectionId connectionIdDTO) {
        try {
            logger.debug(DEBUG_MESSSAGE, "isConnected");
            return new Connected(this.cloudConnectionManagerBridge.isConnected(connectionIdDTO.getConnectionId()));
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
