/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.base.TypeUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireField;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudSubscriber is the specific Wire Component to subscribe a list
 * of wire records as received in Wire Envelope from the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every Wire Record as found in Wire Envelope will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform. Unlike Cloud Publisher Wire
 * Component, the user can only avail to wrap every Wire Record in the default
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
                logger.error(wireMessages.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
            }
            return CloudSubscriber.this.cloudService;
        }

        /** {@inheritDoc} */
        @Override
        public void modifiedService(final ServiceReference<CloudService> reference, final CloudService service) {
            CloudSubscriber.this.cloudService = CloudSubscriber.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
                subscribeTopic();
            } catch (final KuraException e) {
                logger.error(wireMessages.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
            }
        }

        @Override
        public void removedService(final ServiceReference<CloudService> reference, final CloudService service) {
            CloudSubscriber.this.cloudService = null;
        }
    }

    /** Logger instance */
    private static final Logger logger = LoggerFactory.getLogger(CloudSubscriber.class);

    /** Localization Resource */
    private static final WireMessages wireMessages = LocalizationAdapter.adapt(WireMessages.class);

    /** Application Topic as configured */
    private String applicationTopic;

    /** OSGi Bundle Context */
    private BundleContext bundleContext;

    /** Cloud Client reference */
    private CloudClient cloudClient;

    /** Cloud Service reference */
    private volatile CloudService cloudService;

    /** Service Tracker to track the injection of Cloud Service to this component */
    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;

    /** Service Tracker Customizer to customize Cloud Service tracker */
    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;

    /** Configurations Provider */
    private CloudSubscriberOptions cloudSubscriberOptions;

    /** Device ID as configured */
    private String deviceId;

    /** Wire Helper Service reference */
    private volatile WireHelperService wireHelperService;

    /** Wire Support instance to emit and filter Wire Envelope */
    private WireSupport wireSupport;

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(wireMessages.activatingCloudSubscriber());
        this.bundleContext = componentContext.getBundleContext();
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.cloudSubscriberOptions = new CloudSubscriberOptions(properties);
        this.applicationTopic = this.cloudSubscriberOptions.getSubscribingAppTopic();
        this.deviceId = this.cloudSubscriberOptions.getSubscribingDeviceId();

        this.cloudServiceTrackerCustomizer = new CloudSubscriberServiceTrackerCustomizer();
        initCloudServiceTracking();
        logger.debug(wireMessages.activatingCloudSubscriberDone());
    }

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Builds the Wire Record from the provided Kura Payload.
     *
     * @param payload
     *            the payload
     * @return the Wire Record
     * @throws IOException
     *             if the byte array conversion fails
     * @throws NullPointerException
     *             if the payload provided is null
     */
    private WireRecord buildWireRecord(final KuraPayload payload) throws IOException {
        requireNonNull(payload, wireMessages.payloadNonNull());

        final String errorSuffix = ".e";
        final List<WireField> wireFields = CollectionUtil.newArrayList();
        SeverityLevel level = INFO;

        for (final Map.Entry<String, Object> entry : payload.metrics().entrySet()) {
            final String metricKey = entry.getKey();
            final Object metricValue = entry.getValue();

            if (metricKey.endsWith(errorSuffix)) {
                level = SeverityLevel.ERROR;
            }

            TypedValue<?> val = TypedValues.EMPTY_VALUE;
            // check instance of this metric value properly
            if (metricValue instanceof Boolean) {
                final boolean value = Boolean.parseBoolean(String.valueOf(metricValue));
                val = TypedValues.newBooleanValue(value);
            }
            if (metricValue instanceof Byte) {
                final byte value = Byte.parseByte(String.valueOf(metricValue));
                val = TypedValues.newByteValue(value);
            }
            if (metricValue instanceof Long) {
                final long value = Long.parseLong(String.valueOf(metricValue));
                val = TypedValues.newLongValue(value);
            }
            if (metricValue instanceof Double) {
                final double value = Double.parseDouble(String.valueOf(metricValue));
                val = TypedValues.newDoubleValue(value);
            }
            if (metricValue instanceof Integer) {
                final int value = Integer.parseInt(String.valueOf(metricValue));
                val = TypedValues.newIntegerValue(value);
            }
            if (metricValue instanceof Short) {
                final short value = Short.parseShort(String.valueOf(metricValue));
                val = TypedValues.newShortValue(value);
            }
            if (metricValue instanceof String) {
                final String value = String.valueOf(metricValue);
                val = TypedValues.newStringValue(value);
            }
            if (metricValue instanceof byte[]) {
                final byte[] value = TypeUtil.objectToByteArray(metricValue);
                val = TypedValues.newByteArrayValue(value);
            }
            final WireField wireField = new WireField(metricKey, val, level);
            wireFields.add(wireField);
        }
        return new WireRecord(new Timestamp(new Date().getTime()), wireFields);
    }

    /**
     * Closes the cloud client.
     */
    private void closeCloudClient() {
        if (this.cloudClient != null) {
            this.cloudClient.removeCloudClientListener(this);
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(wireMessages.deactivatingCloudSubscriber());

        try {
            unsubsribe();
        } catch (final KuraException e) {
            logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        closeCloudClient();

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        logger.debug(wireMessages.deactivatingCloudSubscriberDone());
    }

    /**
     * Service tracker to manage Cloud Services
     */
    private void initCloudServiceTracking() {
        final String selectedCloudServicePid = this.cloudSubscriberOptions.getCloudServicePid();
        final String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (final InvalidSyntaxException e) {
            logger.error("Filter setup exception " + ThrowableUtil.stackTraceAsString(e));
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEstablished() {
        try {
            if ((this.applicationTopic != null) && (this.deviceId != null)) {
                subscribeTopic();
            }
        } catch (final KuraException e) {
            logger.error(wireMessages.errorCreatingCloudClinet() + ThrowableUtil.stackTraceAsString(e));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionLost() {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onControlMessageArrived(final String deviceId, final String appTopic, final KuraPayload msg,
            final int qos, final boolean retain) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageArrived(final String deviceId, final String appTopic, final KuraPayload msg, final int qos,
            final boolean retain) {
        if (msg != null) {
            WireRecord record = null;
            try {
                record = buildWireRecord(msg);
            } catch (final IOException e) {
                logger.error(ThrowableUtil.stackTraceAsString(e));
            }
            if (record != null) {
                this.wireSupport.emit(Arrays.asList(record));
            }
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
     * Performs subscription via a cloud client instance.
     *
     * @throws KuraException
     *             if the subscription fails
     */
    private void subscribeTopic() throws KuraException {
        if (this.cloudService.isConnected() && (this.cloudClient != null)) {
            this.cloudClient.subscribe(this.deviceId, this.applicationTopic,
                    this.cloudSubscriberOptions.getSubscribingQos());
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * Unsubscribe previous topic.
     *
     * @throws KuraException
     *             if couln't unsubscribe
     */
    private void unsubsribe() throws KuraException {
        if ((this.applicationTopic != null) && (this.deviceId != null) && (this.cloudClient != null)) {
            this.cloudClient.unsubscribe(this.deviceId, this.applicationTopic);
        }
        this.applicationTopic = this.cloudSubscriberOptions.getSubscribingAppTopic();
        this.deviceId = this.cloudSubscriberOptions.getSubscribingDeviceId();
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(wireMessages.updatingCloudSubscriber());
        // recreate the Cloud Client
        try {
            unsubsribe();
        } catch (final KuraException e) {
            logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        // Update properties
        this.cloudSubscriberOptions = new CloudSubscriberOptions(properties);

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        logger.debug(wireMessages.updatingCloudSubscriberDone());
    }
}
