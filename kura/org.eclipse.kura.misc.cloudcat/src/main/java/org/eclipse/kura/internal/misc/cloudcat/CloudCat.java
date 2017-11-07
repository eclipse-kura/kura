/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.misc.cloudcat;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudCat implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(CloudCat.class);

    private CloudCatOptions options;
    private CloudService firstCloudService;
    private CloudService secondCloudService;
    private CloudClient firstCloudClient;
    private CloudClient secondCloudClient;
    private CloudClientRelay firstCloudClientRelay;
    private CloudClientRelay secondCloudClientRelay;

    private ServiceTracker<CloudService, CloudService> firstCloudServiceTracker;
    private ServiceTracker<CloudService, CloudService> secondCloudServiceTracker;

    private ComponentContext componentContext;

    private void bindFirstCloudService(CloudService firstCloudService) {
        this.firstCloudService = firstCloudService;

        try {
            this.firstCloudClient = this.firstCloudService.newCloudClient(this.options.getFirstCloudClientAppId());
            initClients();
        } catch (KuraException e) {
            logger.error("CloudClient: {} instantiation failed", this.options.getFirstCloudClientAppId(), e);
        }
    }

    private void bindSecondCloudService(CloudService secondCloudService) {
        this.secondCloudService = secondCloudService;

        try {
            this.secondCloudClient = this.secondCloudService.newCloudClient(this.options.getSecondCloudClientAppId());
            initClients();
        } catch (KuraException e) {
            logger.error("CloudClient: {} instantiation failed", this.options.getSecondCloudClientAppId(), e);
        }
    }

    private void unbindFirstCloudService(CloudService firstCloudService) {
        cleanupClients();
        this.firstCloudService = null;
    }

    private void unbindSecondCloudService(CloudService firstCloudService) {
        cleanupClients();
        this.secondCloudService = null;
    }

    protected void activate(ComponentContext ctx, Map<String, Object> properties) {
        logger.info("Activating {}", ctx.getProperties().get(KURA_SERVICE_PID));
        this.componentContext = ctx;
        init(properties);
    }

    protected void updated(ComponentContext ctx, Map<String, Object> properties) {
        logger.info("Updating {}", ctx.getProperties().get(KURA_SERVICE_PID));
        cleanup();
        init(properties);
    }

    protected void deactivate(ComponentContext ctx) {
        logger.info("Deactivating {}", ctx.getProperties().get(KURA_SERVICE_PID));
        cleanup();
    }

    private void init(Map<String, Object> properties) {
        try {
            this.options = CloudCatOptions.parseOptions(properties);
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.error("Invalid configuration", e);
            return;
        }

        if (!this.options.isRelayEnabled()) {
            logger.info("Relay is disabled");
            return;
        }

        // Start trackers
        try {
            Filter filter = this.componentContext.getBundleContext()
                    .createFilter("(" + KURA_SERVICE_PID + "=" + this.options.getFirstCloudServicePid() + ")");
            this.firstCloudServiceTracker = new ServiceTracker<>(this.componentContext.getBundleContext(), filter,
                    new ServiceTrackerCustomizer<CloudService, CloudService>() {

                        @Override
                        public CloudService addingService(ServiceReference<CloudService> reference) {
                            CloudService cloudService = CloudCat.this.componentContext.getBundleContext()
                                    .getService(reference);
                            bindFirstCloudService(cloudService);
                            return cloudService;
                        }

                        @Override
                        public void modifiedService(ServiceReference<CloudService> reference, CloudService service) {
                            // Ignore
                        }

                        @Override
                        public void removedService(ServiceReference<CloudService> reference, CloudService service) {
                            unbindFirstCloudService(service);
                        }

                    });
            this.firstCloudServiceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Invalid filter", e);
            return;
        }

        try {
            Filter filter = this.componentContext.getBundleContext()
                    .createFilter("(" + KURA_SERVICE_PID + "=" + this.options.getSecondCloudServicePid() + ")");
            this.secondCloudServiceTracker = new ServiceTracker<>(this.componentContext.getBundleContext(), filter,
                    new ServiceTrackerCustomizer<CloudService, CloudService>() {

                        @Override
                        public CloudService addingService(ServiceReference<CloudService> reference) {
                            CloudService cloudService = CloudCat.this.componentContext.getBundleContext()
                                    .getService(reference);
                            bindSecondCloudService(cloudService);
                            return cloudService;
                        }

                        @Override
                        public void modifiedService(ServiceReference<CloudService> reference, CloudService service) {
                            // Ignore
                        }

                        @Override
                        public void removedService(ServiceReference<CloudService> reference, CloudService service) {
                            unbindSecondCloudService(service);
                        }

                    });
            this.secondCloudServiceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Invalid filter", e);
            return;
        }
    }

    private void initClients() {
        if (this.firstCloudClient != null && this.secondCloudClient != null && this.firstCloudClientRelay == null
                && this.secondCloudClientRelay == null) {
            logger.info("Initializing relay");

            this.firstCloudClientRelay = new CloudClientRelay(this.firstCloudClient, this.secondCloudClient,
                    this.options.getFirstCloudClientDataSubscriptions(),
                    this.options.getFirstCloudClientControlSubscriptions());

            this.secondCloudClientRelay = new CloudClientRelay(this.secondCloudClient, this.firstCloudClient,
                    this.options.getSecondCloudClientDataSubscriptions(),
                    this.options.getSecondCloudClientControlSubscriptions());

            this.firstCloudClientRelay.listen();
            this.secondCloudClientRelay.listen();

            if (this.firstCloudClientRelay.isConnected()) {
                this.firstCloudClientRelay.subscribe();
            }
            if (this.secondCloudClientRelay.isConnected()) {
                this.secondCloudClientRelay.subscribe();
            }
        }
    }

    private void cleanup() {
        if (this.firstCloudServiceTracker != null) {
            this.firstCloudServiceTracker.close();
            this.firstCloudServiceTracker = null;
        }
        if (this.secondCloudServiceTracker != null) {
            this.secondCloudServiceTracker.close();
            this.secondCloudServiceTracker = null;
        }
        cleanupClients();
    }

    private void cleanupClients() {
        if (this.firstCloudClientRelay != null) {
            this.firstCloudClientRelay.unlisten();
            if (this.firstCloudClientRelay.isConnected()) {
                this.firstCloudClientRelay.unsubscribe();
            }
            this.firstCloudClientRelay = null;
        }
        if (this.secondCloudClientRelay != null) {
            this.secondCloudClientRelay.unlisten();
            if (this.secondCloudClientRelay.isConnected()) {
                this.secondCloudClientRelay.unsubscribe();
            }
            this.secondCloudClientRelay = null;
        }

        if (this.firstCloudClient != null) {
            this.firstCloudClient.release();
            this.firstCloudClient = null;
        }
        if (this.secondCloudClient != null) {
            this.secondCloudClient.release();
            this.secondCloudClient = null;
        }
    }
}
