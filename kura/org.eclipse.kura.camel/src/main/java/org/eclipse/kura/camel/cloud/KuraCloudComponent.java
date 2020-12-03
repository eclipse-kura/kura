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
package org.eclipse.kura.camel.cloud;

import static org.eclipse.kura.camel.internal.utils.KuraServiceFactory.retrieveService;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.eclipse.kura.camel.internal.cloud.CloudClientCache;
import org.eclipse.kura.camel.internal.cloud.CloudClientCacheImpl;
import org.eclipse.kura.cloud.CloudService;

/**
 * The Camel component for providing "kura-cloud"
 */
public class KuraCloudComponent extends DefaultComponent {

    public static final String DEFAULT_NAME = "kura-cloud";

    private CloudService cloudService;
    private CloudClientCache cache;

    public KuraCloudComponent() {
        super();
    }

    // Constructors

    public KuraCloudComponent(final CamelContext context) {
        super(context);
    }

    public KuraCloudComponent(final CamelContext context, final CloudService cloudService) {
        super(context);
        this.cloudService = cloudService;
    }

    @Override
    protected void doStart() throws Exception {
        final CloudService cloudServiceInstance = lookupCloudService();

        if (cloudServiceInstance == null) {
            throw new IllegalStateException(
                    "'cloudService' is not set and not found in Camel context service registry");
        }

        this.cache = new CloudClientCacheImpl(cloudServiceInstance);

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (this.cache != null) {
            this.cache.close();
            this.cache = null;
        }
    }

    // Operations

    @Override
    protected Endpoint createEndpoint(String uri, String remain, Map<String, Object> parameters) throws Exception {
        final KuraCloudEndpoint kuraCloudEndpoint = new KuraCloudEndpoint(uri, this, this.cache);

        final String[] res = remain.split("/", 2);
        if (res.length < 2) {
            throw new IllegalArgumentException("Wrong kura-cloud URI format. Should be: kura-cloud:app/topic");
        }
        parameters.put(KuraCloudConstants.APPLICATION_ID, res[0]);
        parameters.put(KuraCloudConstants.TOPIC, res[1]);

        setProperties(kuraCloudEndpoint, parameters);

        return kuraCloudEndpoint;
    }

    protected CloudService lookupCloudService() {
        if (this.cloudService == null) {
            this.cloudService = retrieveService(CloudService.class, getCamelContext().getRegistry());
        }
        return this.cloudService;
    }
}
