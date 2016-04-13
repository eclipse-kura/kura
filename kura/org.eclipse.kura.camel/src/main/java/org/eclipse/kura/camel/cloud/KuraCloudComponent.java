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
package org.eclipse.kura.camel.cloud;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.kura.cloud.CloudService;

import static org.eclipse.kura.camel.utils.KuraServiceFactory.retrieveService;

public class KuraCloudComponent extends UriEndpointComponent {

    private final static CloudClientCache clientCache = new ConcurrentHashMapCloudClientCache();

    private CloudService cloudService;

    public KuraCloudComponent() {
        super(KuraCloudEndpoint.class);
    }

    // Constructors

    public KuraCloudComponent(CamelContext context, Class<? extends Endpoint> endpointClass) {
        super(context, endpointClass);
    }

    // Operations

    @Override
    protected Endpoint createEndpoint(String uri, String remain, Map<String, Object> parameters) throws Exception {
        KuraCloudEndpoint kuraCloudEndpoint = new KuraCloudEndpoint(uri, this, cloudService);

        String[] res = remain.split("/");
        if (res.length != 2) {
            throw new IllegalArgumentException("Wrong kura-cloud URI format. Should be: kura-cloud:app/topic");
        }
        parameters.put(KuraCloudConstants.APPLICATION_ID, res[0]);
        parameters.put(KuraCloudConstants.TOPIC, res[1]);

        setProperties(kuraCloudEndpoint, parameters);

        return kuraCloudEndpoint;
    }

    public static CloudClientCache clientCache() {
        return clientCache;
    }

    public CloudService getCloudService() {
        if(cloudService == null) {
            cloudService = retrieveService(CloudService.class, getCamelContext().getRegistry());
        }
        return cloudService;
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

}
