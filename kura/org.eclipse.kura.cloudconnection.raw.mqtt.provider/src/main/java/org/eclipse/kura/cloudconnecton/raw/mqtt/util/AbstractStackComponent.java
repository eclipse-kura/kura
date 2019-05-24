/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnecton.raw.mqtt.util;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.StackComponentOptions.OptionsFactory;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;

public abstract class AbstractStackComponent<T> implements ConfigurableComponent, CloudConnectionListener {

    private final Set<CloudConnectionListener> cloudConnectionListeners = new CopyOnWriteArraySet<>();
    private final AtomicReference<StackComponentOptions<T>> options = new AtomicReference<>();
    private final AtomicReference<Optional<RawMqttCloudEndpoint>> endpoint = new AtomicReference<>(Optional.empty());

    private ServiceTracker<CloudEndpoint, RawMqttCloudEndpoint> tracker;
    private final BundleContext context = FrameworkUtil.getBundle(AbstractStackComponent.class).getBundleContext();

    protected abstract Logger getLogger();

    protected abstract OptionsFactory<T> getOptionsFactory();

    protected void setCloudEndpoint(final RawMqttCloudEndpoint endpoint) {
        this.endpoint.set(Optional.of(endpoint));
        endpoint.registerCloudConnectionListener(this);
    }

    protected void unsetCloudEndpoint(final RawMqttCloudEndpoint endpoint) {
        endpoint.unregisterCloudConnectionListener(this);
        this.endpoint.set(Optional.empty());
    }

    public void activated(final Map<String, Object> properties) {
        getLogger().info("activating...");

        updated(properties);

        getLogger().info("activating...done");
    }

    public void updated(final Map<String, Object> properties) {
        getLogger().info("updating...");

        this.options.set(new StackComponentOptions<>(properties, getOptionsFactory()));
        reopenTracker();

        getLogger().info("updating...done");
    }

    public void deactivated() {
        getLogger().info("deactivating...");

        shutdownTracker();

        getLogger().info("deactivating...done");
    }

    protected StackComponentOptions<T> getOptions() {
        return options.get();
    }

    protected Optional<RawMqttCloudEndpoint> getEndpoint() {
        return endpoint.get();
    }

    private void shutdownTracker() {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }
    }

    private void reopenTracker() {
        shutdownTracker();

        final StackComponentOptions<?> currentOptions = this.options.get();

        final Optional<String> endpointPid = currentOptions.getCloudEndpointPid();

        if (!endpointPid.isPresent()) {
            return;
        }

        final Filter filter;
        try {
            filter = Utils.createFilter(CloudEndpoint.class, endpointPid.get());
        } catch (final Exception e) {
            getLogger().warn("invalid cloud endpoint pid", e);
            return;
        }

        this.tracker = new ServiceTracker<>(context, filter, new Customizer());
        this.tracker.open();
    }

    private class Customizer implements ServiceTrackerCustomizer<CloudEndpoint, RawMqttCloudEndpoint> {

        @Override
        public RawMqttCloudEndpoint addingService(final ServiceReference<CloudEndpoint> reference) {
            final CloudEndpoint service = context.getService(reference);

            if (service == null) {
                return null;
            }

            if (!(service instanceof RawMqttCloudEndpoint)) {
                context.ungetService(reference);
                return null;
            }

            final RawMqttCloudEndpoint trackedService = (RawMqttCloudEndpoint) service;

            setCloudEndpoint(trackedService);

            return trackedService;
        }

        @Override
        public void modifiedService(final ServiceReference<CloudEndpoint> reference,
                final RawMqttCloudEndpoint service) {
            // do nothing
        }

        @Override
        public void removedService(final ServiceReference<CloudEndpoint> reference,
                final RawMqttCloudEndpoint service) {

            unsetCloudEndpoint(service);
            context.ungetService(reference);
        }

    }

    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        cloudConnectionListeners.add(cloudConnectionListener);
    }

    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        cloudConnectionListeners.remove(cloudConnectionListener);
    }

    @Override
    public void onDisconnected() {
        cloudConnectionListeners.forEach(Utils.catchAll(CloudConnectionListener::onDisconnected));
    }

    @Override
    public void onConnectionLost() {
        cloudConnectionListeners.forEach(Utils.catchAll(CloudConnectionListener::onConnectionLost));
    }

    @Override
    public void onConnectionEstablished() {
        cloudConnectionListeners.forEach(Utils.catchAll(CloudConnectionListener::onConnectionEstablished));
    }
}
