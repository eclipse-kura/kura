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

import javax.ws.rs.Path;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEntriesDTO;
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

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(MQTT_APP_ID);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", MQTT_APP_ID, e);
        }
    }

    public CloudEntriesDTO findCloudEntries() {

    }

    public List<ConfigComponent> getStackConfigurationsByFactory(final String factoryPid,
            final String cloudServicePid) {

    }

    public String findSuggestedCloudServicePid(String factoryPid) {

    }

    public String findCloudServicePidRegex(String factoryPid) {

    }

    public void createCloudServiceFromFactory(String factoryPid, String cloudServicePid) {

    }

    public void deleteCloudServiceFromFactory(String factoryPid, String cloudServicePid) {

    }

    public GwtCloudComponentFactories getCloudComponentFactories() {

    }

    public void createPubSubInstance(String pid, String factoryPid, String cloudConnectionPid) {

    }

    public void deletePubSubInstance(String pid) {

    }

    public GwtConfigComponent getPubSubConfiguration(String pid) {

    }

    public void updateStackComponentConfiguration(GwtConfigComponent component) {

    }

    public void connectDataService(String connectionId) {

    }

    public void disconnectDataService(String connectionId) {

    }

    public boolean isConnected(String connectionId) {

    }

}
