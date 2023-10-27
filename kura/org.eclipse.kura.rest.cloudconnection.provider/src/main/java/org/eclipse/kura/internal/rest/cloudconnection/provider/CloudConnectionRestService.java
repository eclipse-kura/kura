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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentFactoriesDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEntriesDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConfigComponentDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
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
    private ConfigurationService configurationService;

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
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
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    public CloudEntriesDTO findCloudEntries() {
        try {
            logger.debug(DEBUG_MESSSAGE, "findCloudEntries");
            return new CloudEntriesDTO(this.cloudConnectionService.findCloudEntries());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public List<ConfigComponentDTO> getStackConfigurationsByFactory(final String factoryPid,
            final String cloudServicePid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "getStackConfigurationsByFactory");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public String findSuggestedCloudServicePid(String factoryPid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "findSuggestedCloudServicePid");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    public String findCloudServicePidRegex(String factoryPid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "findCloudServicePidRegex");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public void createCloudServiceFromFactory(String factoryPid, String cloudServicePid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "createCloudServiceFromFactory");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    public void deleteCloudServiceFromFactory(String factoryPid, String cloudServicePid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "deleteCloudServiceFromFactory");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    public CloudComponentFactoriesDTO getCloudComponentFactories() {
        try {
            logger.debug(DEBUG_MESSSAGE, "getCloudComponentFactories");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public void createPubSubInstance(String pid, String factoryPid, String cloudConnectionPid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "createPubSubInstance");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    public void deletePubSubInstance(String pid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "deletePubSubInstance");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public ConfigComponentDTO getPubSubConfiguration(String pid) {
        try {
            logger.debug(DEBUG_MESSSAGE, "getPubSubConfiguration");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    public void updateStackComponentConfiguration(ConfigComponentDTO component) {
        try {
            logger.debug(DEBUG_MESSSAGE, "updateStackComponentConfiguration");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public void connectDataService(String connectionId) {
        try {
            logger.debug(DEBUG_MESSSAGE, "connectDataService");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public void disconnectDataService(String connectionId) {
        try {
            logger.debug(DEBUG_MESSSAGE, "disconnectDataService");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    public boolean isConnected(String connectionId) {
        try {
            logger.debug(DEBUG_MESSSAGE, "isConnected");
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
