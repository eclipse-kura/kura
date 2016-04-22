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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapCloudClientCache implements CloudClientCache {

    private final static Logger LOG = LoggerFactory.getLogger(ConcurrentHashMapCloudClientCache.class);

    private final Map<String, CloudClient> cacheMap = new ConcurrentHashMap<String, CloudClient>();

    @Override
    public void put(String appId, CloudClient cloudClient) {
        cacheMap.put(appId, cloudClient);
    }

    @Override
    public CloudClient get(String appId) {
        return cacheMap.get(appId);
    }

    @Override
    public CloudClient getOrCreate(String applicationId, CloudService cloudService) {
        try {
            CloudClient cloudClient = get(applicationId);
            if (cloudClient == null) {
                LOG.debug("CloudClient for application ID {} not found. Creating new one.", applicationId);
                cloudClient = cloudService.newCloudClient(applicationId);
                put(applicationId, cloudClient);
            }
            return cloudClient;
        } catch (KuraException e) {
            throw new RuntimeException(e);
        }
    }

}