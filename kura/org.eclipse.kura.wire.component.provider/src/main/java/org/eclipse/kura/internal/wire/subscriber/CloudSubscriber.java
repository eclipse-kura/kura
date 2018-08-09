/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The Class CloudSubscriber is the specific Wire Component to subscribe a list
 * of {@link WireRecord}s as received in {@link WireEnvelope} from the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every {@link WireRecord} as found in {@link WireEnvelope} will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform. Unlike Cloud Publisher Wire
 * Component, the user can only avail to wrap every {@link WireRecord} in the default
 * Google Protobuf Payload.
 */
public final class CloudSubscriber implements WireEmitter, ConfigurableComponent, CloudClientListener {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class CloudSubscriberServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudService, CloudService> {

        @Override
        public CloudService addingService(final ServiceReference<CloudService> reference) {
            CloudSubscriber.this.cloudService = CloudSubscriber.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
                subscribeTopic();
            } catch (final KuraException e) {
                logger.error("Cannot setup CloudClient...", e);
            }
            return CloudSubscriber.this.cloudService;
        }

        @Override
        public void modifiedService(final ServiceReference<CloudService> reference, final CloudService service) {
            CloudSubscriber.this.cloudService = CloudSubscriber.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
                subscribeTopic();
            } catch (final KuraException e) {
                logger.error("Cannot setup CloudClient...", e);
            }
        }

        @Override
        public void removedService(final ServiceReference<CloudService> reference, final CloudService service) {
            CloudSubscriber.this.cloudService = null;
        }
    }

    private static final Logger logger = LogManager.getLogger();

    private BundleContext bundleContext;

    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;

    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;

    private volatile CloudService cloudService;

    private CloudClient cloudClient;

    private CloudSubscriberOptions cloudSubscriberOptions;

    private String deviceId;

    private String applicationTopic;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating Cloud Subscriber Wire Component...");
        this.bundleContext = componentContext.getBundleContext();
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        this.cloudSubscriberOptions = new CloudSubscriberOptions(properties);
        this.applicationTopic = this.cloudSubscriberOptions.getSubscribingAppTopic();
        this.deviceId = this.cloudSubscriberOptions.getSubscribingDeviceId();

        this.cloudServiceTrackerCustomizer = new CloudSubscriberServiceTrackerCustomizer();
        initCloudServiceTracking();
        logger.debug("Activating Cloud Subscriber Wire Component... Done");
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug("Updating Cloud Subscriber Wire Component...");
        // recreate the Cloud Client
        try {
            unsubsribe();
        } catch (final KuraException e) {
            logger.error("Error unsubscribing...", e);
        }
        // Update properties
        this.cloudSubscriberOptions = new CloudSubscriberOptions(properties);

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        logger.debug("Updating Cloud Subscriber Wire Component... Done");
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating Cloud Subscriber Wire Component...");

        try {
            unsubsribe();
        } catch (final KuraException e) {
            logger.error("Error unsubscribing...", e);
        }
        closeCloudClient();

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        logger.debug("Deactivating Cloud Subscriber Wire Component... Done");
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEstablished() {
        try {
            if (nonNull(this.applicationTopic) && nonNull(this.deviceId)) {
                subscribeTopic();
            }
        } catch (final KuraException e) {
            logger.error("Error in creating cloud client", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageConfirmed(final int messageId, final String topic) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessagePublished(final int messageId, final String topic) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wires) {
        return this.wireSupport.polled(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        if (nonNull(msg)) {
            List<WireRecord> records = buildWireRecord(msg);
            this.wireSupport.emit(records);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionLost() {
        // Not required
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    /**
     * Performs subscription via a cloud client instance.
     *
     * @throws KuraException
     *             if the subscription fails
     */
    private void subscribeTopic() throws KuraException {
        if (this.cloudService.isConnected() && nonNull(this.cloudClient)) {
            this.cloudClient.subscribe(this.deviceId, this.applicationTopic,
                    this.cloudSubscriberOptions.getSubscribingQos());
        }
    }

    /**
     * Service tracker to manage Cloud Services
     */
    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.cloudSubscriberOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Error while building a Bundle Context filter.", e);
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    /**
     * Closes the cloud client.
     */
    private void closeCloudClient() {
        if (nonNull(this.cloudClient)) {
            this.cloudClient.removeCloudClientListener(this);
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    /**
     * Setup cloud client.
     *
     * @throws KuraException
     *             the kura exception
     */
    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.cloudSubscriberOptions.getSubscribingApplication();
        this.cloudClient = this.cloudService.newCloudClient(appId);
        this.cloudClient.addCloudClientListener(this);
    }

    /**
     * Builds a list of {@link WireRecord}s from the provided Kura Payload.
     *
     * @param payload
     *            the payload
     * @return a List of {@link WireRecord}s
     * @throws NullPointerException
     *             if the payload provided is null
     */
    private List<WireRecord> buildWireRecord(final KuraPayload payload) {
        requireNonNull(payload, "Payload cannot be null");

        final Map<String, Object> kuraPayloadProperties = payload.metrics();
        final Map<String, TypedValue<?>> wireProperties = new HashMap<>();

        for (Entry<String, Object> entry : kuraPayloadProperties.entrySet()) {
            final String entryKey = entry.getKey();
            final Object entryValue = entry.getValue();

            final TypedValue<?> convertedValue = TypedValues.newTypedValue(entryValue);
            wireProperties.put(entryKey, convertedValue);
        }

        final WireRecord wireRecord = new WireRecord(wireProperties);
        return Arrays.asList(wireRecord);
    }

    /**
     * Unsubscribe previous topic.
     *
     * @throws KuraException
     *             if couln't unsubscribe
     */
    private void unsubsribe() throws KuraException {
        if (nonNull(this.applicationTopic) && nonNull(this.deviceId) && nonNull(this.cloudClient)) {
            this.cloudClient.unsubscribe(this.deviceId, this.applicationTopic);
        }
        this.applicationTopic = this.cloudSubscriberOptions.getSubscribingAppTopic();
        this.deviceId = this.cloudSubscriberOptions.getSubscribingDeviceId();
    }
}
