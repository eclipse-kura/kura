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