/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils;

import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class SparkplugCloudEndpointTracker {

    private final BundleContext bundleContext;
    private ServiceTracker<CloudConnectionManager, CloudConnectionManager> cloudConnectionManagerTracker;
    private final Consumer<SparkplugCloudEndpoint> serviceAddedConsumer;
    private final Consumer<SparkplugCloudEndpoint> serviceRemovedConsumer;
    private final String endpointPid;

    public SparkplugCloudEndpointTracker(BundleContext bundleContext,
            Consumer<SparkplugCloudEndpoint> serviceAddedConsumer,
            Consumer<SparkplugCloudEndpoint> serviceRemovedConsumer, String endpointPid) {
        this.bundleContext = bundleContext;
        this.serviceAddedConsumer = serviceAddedConsumer;
        this.serviceRemovedConsumer = serviceRemovedConsumer;
        this.endpointPid = endpointPid;
    }

    public void startEndpointTracker() throws InvalidSyntaxException {
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudConnectionManager.class.getName(), this.endpointPid);

        final Filter filter = this.bundleContext.createFilter(filterString);
        this.cloudConnectionManagerTracker = new ServiceTracker<>(this.bundleContext, filter,
                new SparkplugCloudEndpointTrackerCustomizer());

        this.cloudConnectionManagerTracker.open();
    }

    public void stopEndpointTracker() {
        if (Objects.nonNull(this.cloudConnectionManagerTracker)) {
            this.cloudConnectionManagerTracker.close();
        }
    }

    private class SparkplugCloudEndpointTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudConnectionManager, CloudConnectionManager> {

        @Override
        public synchronized CloudConnectionManager addingService(
                final ServiceReference<CloudConnectionManager> reference) {
            CloudConnectionManager cloudConnectionManager = SparkplugCloudEndpointTracker.this.bundleContext
                    .getService(reference);

            if (cloudConnectionManager instanceof SparkplugCloudEndpoint) {
                SparkplugCloudEndpointTracker.this.serviceAddedConsumer
                        .accept((SparkplugCloudEndpoint) cloudConnectionManager);
                return cloudConnectionManager;
            } else {
                SparkplugCloudEndpointTracker.this.bundleContext.ungetService(reference);
            }

            return null;
        }

        @Override
        public synchronized void removedService(final ServiceReference<CloudConnectionManager> reference,
                final CloudConnectionManager service) {
            if (service instanceof SparkplugCloudEndpoint) {
                SparkplugCloudEndpointTracker.this.serviceRemovedConsumer.accept((SparkplugCloudEndpoint) service);
            }
        }

        @Override
        public synchronized void modifiedService(final ServiceReference<CloudConnectionManager> reference,
                final CloudConnectionManager service) {
            // Not needed
        }

    }

}
