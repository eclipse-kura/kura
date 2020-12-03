/*******************************************************************************
 * Copyright (c) 2011, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.camelcloud;

import static org.apache.camel.ServiceStatus.Started;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.camel.internal.camelcloud.CamelCloudClient;
import org.eclipse.kura.cloud.CloudClient;

/**
 * A default implementation of the {@link CamelCloudService}
 */
public class DefaultCamelCloudService implements CamelCloudService {

    private final CamelContext camelContext;

    private final Map<String, CloudClient> clients = new ConcurrentHashMap<>();

    private final Map<String, String> baseEndpoints = new ConcurrentHashMap<>();

    public DefaultCamelCloudService(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CloudClient newCloudClient(String applicationId) throws KuraException {
        String baseEndpoint = this.baseEndpoints.get(applicationId);
        if (baseEndpoint == null) {
            baseEndpoint = "vm:%s";
        }
        final CloudClient cloudClient = new CamelCloudClient(this, this.camelContext, applicationId, baseEndpoint);
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
        CloudClient client = this.clients.remove(applicationId);
        if (client != null) {
            client.release();
        }
    }

    public void dispose() {
        final LinkedList<Exception> errors = new LinkedList<>();
        for (CloudClient client : this.clients.values()) {
            try {
                client.release();
            } catch (Exception e) {
                errors.add(e);
            }
        }
        this.clients.clear();

        if (!errors.isEmpty()) {
            final Exception first = errors.pollFirst();
            errors.forEach(first::addSuppressed);
            throw new RuntimeException(first);
        }
    }
}
