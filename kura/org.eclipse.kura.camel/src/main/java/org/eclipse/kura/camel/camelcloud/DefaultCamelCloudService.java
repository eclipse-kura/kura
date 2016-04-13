/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        String baseEndpoint = baseEndpoints.get(applicationId);
        if(baseEndpoint == null) {
            baseEndpoint = "seda:%s";
        }
        CloudClient cloudClient = new CamelCloudClient(this, camelContext, applicationId, baseEndpoint);
        clients.put(applicationId, cloudClient);
        return cloudClient;
    }

    @Override
    public String[] getCloudApplicationIdentifiers() {
        return clients.keySet().toArray(new String[0]);
    }

    @Override
    public boolean isConnected() {
        return camelContext.getStatus() == Started;
    }

    @Override
    public void registerBaseEndpoint(String applicationId, String baseEndpoint) {
        baseEndpoints.put(applicationId, baseEndpoint);
    }

    @Override
    public void release(String applicationId) {
        clients.remove(applicationId);
    }

}
