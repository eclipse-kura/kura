/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.cloud.subscriber;

import static java.util.Objects.nonNull;
import static org.eclipse.kura.core.message.MessageConstants.APP_ID;
import static org.eclipse.kura.core.message.MessageConstants.APP_TOPIC;
import static org.eclipse.kura.core.message.MessageConstants.CONTROL;
import static org.eclipse.kura.core.message.MessageConstants.QOS;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.cloud.CloudServiceImpl;
import org.eclipse.kura.core.message.MessageType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudSubscriberImpl
        implements CloudSubscriber, ConfigurableComponent, CloudConnectionListener, CloudSubscriberListener {

    private final class CloudServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudConnectionManager, CloudConnectionManager> {

        @Override
        public CloudConnectionManager addingService(final ServiceReference<CloudConnectionManager> reference) {
            CloudConnectionManager tempCloudService = CloudSubscriberImpl.this.bundleContext.getService(reference);

            if (tempCloudService instanceof CloudServiceImpl) {
                CloudSubscriberImpl.this.cloudService = (CloudServiceImpl) tempCloudService;

                Map<String, Object> subscriptionProps = new HashMap<>();
                subscriptionProps.put(APP_ID.name(), CloudSubscriberImpl.this.cloudSubscriberOptions.getAppId());
                subscriptionProps.put(APP_TOPIC.name(), CloudSubscriberImpl.this.cloudSubscriberOptions.getAppTopic());
                subscriptionProps.put(QOS.name(), CloudSubscriberImpl.this.cloudSubscriberOptions.getQos());
                subscriptionProps.put(CONTROL.name(),
                        MessageType.CONTROL.equals(CloudSubscriberImpl.this.cloudSubscriberOptions.getMessageType()));

                CloudSubscriberImpl.this.cloudService.registerSubscriber(subscriptionProps, CloudSubscriberImpl.this);
                CloudSubscriberImpl.this.cloudService.registerCloudConnectionListener(CloudSubscriberImpl.this);
                return tempCloudService;
            } else {
                CloudSubscriberImpl.this.bundleContext.ungetService(reference);
            }

            return null;
        }

        @Override
        public void removedService(final ServiceReference<CloudConnectionManager> reference,
                final CloudConnectionManager service) {
            Map<String, Object> subscriptionProps = new HashMap<>();
            subscriptionProps.put(APP_ID.name(), CloudSubscriberImpl.this.cloudSubscriberOptions.getAppId());

            CloudSubscriberImpl.this.cloudService.unregisterSubscriber(subscriptionProps);
            CloudSubscriberImpl.this.cloudService.unregisterCloudConnectionListener(CloudSubscriberImpl.this);
            CloudSubscriberImpl.this.cloudService = null;
        }

        @Override
        public void modifiedService(ServiceReference<CloudConnectionManager> reference,
                CloudConnectionManager service) {
            // Not needed
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CloudSubscriberImpl.class);

    private ServiceTrackerCustomizer<CloudConnectionManager, CloudConnectionManager> cloudServiceTrackerCustomizer;
    private ServiceTracker<CloudConnectionManager, CloudConnectionManager> cloudServiceTracker;

    private CloudSubscriberOptions cloudSubscriberOptions;
    private CloudServiceImpl cloudService;
    private BundleContext bundleContext;

    private final Set<CloudSubscriberListener> subscribers = new CopyOnWriteArraySet<>();
    private final Set<CloudConnectionListener> cloudConnectionListeners = new CopyOnWriteArraySet<>();

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating Cloud Publisher...");
        this.bundleContext = componentContext.getBundleContext();

        this.cloudServiceTrackerCustomizer = new CloudServiceTrackerCustomizer();

        doUpdate(properties);

        logger.debug("Activating Cloud Publisher... Done");
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("Updating Cloud Publisher...");

        doUpdate(properties);

        logger.debug("Updating Cloud Publisher... Done");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating Cloud Publisher...");

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        logger.debug("Deactivating Cloud Publisher... Done");
    }

    private void doUpdate(Map<String, Object> properties) {
        this.cloudSubscriberOptions = new CloudSubscriberOptions(properties);

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();
    }

    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.cloudSubscriberOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudConnectionManager.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    @Override
    public void registerCloudSubscriberListener(CloudSubscriberListener listener) {
        this.subscribers.add(listener);
    }

    @Override
    public void unregisterCloudSubscriberListener(CloudSubscriberListener listener) {
        this.subscribers.remove(listener);
    }

    @Override
    public void onConnectionEstablished() {
        this.cloudConnectionListeners.forEach(CloudConnectionListener::onConnectionEstablished);
    }

    @Override
    public void onConnectionLost() {
        this.cloudConnectionListeners.forEach(CloudConnectionListener::onConnectionLost);
    }

    @Override
    public void onDisconnected() {
        this.cloudConnectionListeners.forEach(CloudConnectionListener::onDisconnected);
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.remove(cloudConnectionListener);
    }

    @Override
    public void onMessageArrived(KuraMessage message) {
        this.subscribers.forEach(subscriber -> subscriber.onMessageArrived(message));
    }
}
