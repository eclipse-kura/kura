/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.camelcloud;

import org.apache.camel.CamelContext;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.camel.ServiceStatus.Started;

public class DefaultCamelCloudService implements CamelCloudService {

    private final CamelContext camelContext;

    private final Map<String, CloudClient> clients = new ConcurrentHashMap<String, CloudClient>();

    private final Map<String, String> baseEndpoints = new ConcurrentHashMap<String, String>();

    public DefaultCamelCloudService(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CloudClient newCloudClient(String applicationId) throws KuraException {
        String baseEndpoint = this.baseEndpoints.get(applicationId);
        if(baseEndpoint == null) {
            baseEndpoint = "vm:%s";
        }
        CloudClient cloudClient = new CamelCloudClient(this, this.camelContext, applicationId, baseEndpoint);
        this.clients.put(applicationId, cloudClient);
        return cloudClient;
    }

    @Override
    public String[] getCloudApplicationIdentifiers() {
        return this.clients.keySet().toArray(new String[0]);
    }

    @Override
    public boolean isConnected() {
        return this.camelContext.getStatus() == Started;
    }

    @Override
    public void registerBaseEndpoint(String applicationId, String baseEndpoint) {
    	this.baseEndpoints.put(applicationId, baseEndpoint);
    }

    @Override
    public void release(String applicationId) {
    	this.clients.remove(applicationId);
    }
}
