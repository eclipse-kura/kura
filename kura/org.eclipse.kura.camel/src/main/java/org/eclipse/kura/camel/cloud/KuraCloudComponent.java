/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
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

        String[] res = remain.split("/", 2);
        if (res.length < 2) {
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
