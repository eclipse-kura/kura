/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.internal.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudClientCacheImpl implements CloudClientCache {

    private final class CloudClientHandleImplementation implements CloudClientHandle {

        private final String applicationId;

        private final CloudClient client;

        public CloudClientHandleImplementation(String applicationId, CloudClient client) {
            this.applicationId = applicationId;
            this.client = client;
        }

        @Override
        public void close() throws Exception {
            removeHandle(CloudClientHandleImplementation.this, this.applicationId, this.client);
        }

        @Override
        public CloudClient getClient() {
            return this.client;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CloudClientCacheImpl.class);

    private final CloudService cloudService;

    private final Map<String, Set<CloudClientHandle>> cache = new HashMap<>();

    public CloudClientCacheImpl(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    @Override
    public synchronized CloudClientHandle getOrCreate(final String applicationId) {
        try {
            Set<CloudClientHandle> set = this.cache.get(applicationId);
            if (set == null) {
                logger.debug("CloudClient for application ID {} not found. Creating new one.", applicationId);
                set = new HashSet<>();
                this.cache.put(applicationId, set);
            } else {
                logger.debug("CloudClient for application ID {} ... cache hit.", applicationId);
            }

            boolean created = false;
            CloudClient client = null;
            try {

                if (set.isEmpty()) {
                    logger.debug("Creating new cloud client for: {}", applicationId);
                    created = true;
                    client = this.cloudService.newCloudClient(applicationId);
                } else {
                    client = set.iterator().next().getClient();
                    logger.debug("Re-using cloud client: {} -> {}", applicationId, client);
                }

                try {
                    final CloudClientHandle handle = new CloudClientHandleImplementation(applicationId, client);
                    set.add(handle);
                    return handle;
                } finally {
                    // mark as returned
                    client = null;
                }
            } finally {
                if (created && client != null) {
                    // clean up leaked resource
                    client.release();
                }
            }

        } catch (KuraException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeHandle(CloudClientHandle handle, String applicationId, CloudClient client) {

        logger.debug("Remove handle: {}", handle);

        final Set<CloudClientHandle> set;

        synchronized (this) {
            set = this.cache.get(applicationId);
            if (set == null) {
                return;
            }

            set.remove(handle); // don't process result, we clean up anyway

            if (set.isEmpty()) {
                logger.debug("Removing last handle for: {}", applicationId);
                this.cache.remove(applicationId);
            }
        }

        if (set.isEmpty()) {
            // release outside of lock
            logger.debug("Releasing client: {} / {}", applicationId, client);
            client.release();
        }
    }

    @Override
    public void close() {
        final List<CloudClientHandle> handles = new ArrayList<>();
        synchronized (this) {
            for (final Set<CloudClientHandle> set : this.cache.values()) {
                handles.addAll(set);
            }
            this.cache.clear();
        }

        // release outside the lock

        final Set<CloudClient> clients = new HashSet<>();
        for (final CloudClientHandle handle : handles) {

            final CloudClient client = handle.getClient();

            if (clients.add(client)) {
                client.release();
                logger.info("Closing client: {}", client);
            }
        }
    }

}