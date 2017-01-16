/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.wire.SeverityLevel.ERROR;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import java.io.IOException;
import java.util.Arrays;
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
        public CloudService addingService(ServiceReference<CloudService> reference) {
            CloudSubscriber.this.cloudService = CloudSubscriber.this.componentContext.getBundleContext()
                    .getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
                subscribeTopic();
            } catch (KuraException e) {
                logger.error(wireMessages.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
            }
            return CloudSubscriber.this.cloudService;
        }

        @Override
        public void modifiedService(ServiceReference<CloudService> reference, CloudService service) {
            CloudSubscriber.this.cloudService = CloudSubscriber.this.componentContext.getBundleContext()
                    .getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
                subscribeTopic();
            } catch (KuraException e) {
                logger.error(wireMessages.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
            }
        }

        @Override
        public void removedService(ServiceReference<CloudService> reference, CloudService service) {
            CloudSubscriber.this.cloudService = null;
        }
    }

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(CloudSubscriber.class);

    /** Localization Resource */
    private static final WireMessages wireMessages = LocalizationAdapter.adapt(WireMessages.class);

    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;

    /** The cloud service. */
    private volatile CloudService cloudService;

    private CloudClient cloudClient;

    private final ServiceTrackerCustomizer<CloudService, CloudService> customizer = new CloudSubscriberServiceTrackerCustomizer();

    /** The cloud subscriber options. */
    private CloudSubscriberOptions options;

    /** The subscribed topic */
    private String topic;

    /** The Wire Helper Service. */
    private volatile WireHelperService wireHelperService;

    /** The wire supporter component. */
    private WireSupport wireSupport;

    private ComponentContext componentContext;

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
        this.componentContext = componentContext;
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.options = new CloudSubscriberOptions(properties);
        this.topic = this.options.getSubscribingTopic();
        initCloudServiceTracking();
        logger.debug(wireMessages.activatingCloudSubscriberDone());
    }

    private void subscribeTopic() throws KuraException {
        if (this.cloudService.isConnected() && this.cloudClient != null) {
            this.cloudClient.subscribe(this.topic, this.options.getSubscribingQos());
        }
    }

    /**
     * Service tracker to manage Cloud Services
     */
    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.options.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.componentContext.getBundleContext().createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception " + ThrowableUtil.stackTraceAsString(e));
        }
        this.cloudServiceTracker = new ServiceTracker<CloudService, CloudService>(
                this.componentContext.getBundleContext(), filter, this.customizer);
        this.cloudServiceTracker.open();
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

    /**
     * Setup cloud client.
     *
     * @throws KuraException
     *             the kura exception
     */
    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.options.getSubscribingApplication();
        this.cloudClient = this.cloudService.newCloudClient(appId);
        this.cloudClient.addCloudClientListener(this);
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
        final List<WireField> wireFields = CollectionUtil.newArrayList();

        final String flag = "asset_flag";
        SeverityLevel level = INFO;
        final Object severityLevelMetric = payload.getMetric(flag);
        if ("ERROR".equalsIgnoreCase(String.valueOf(severityLevelMetric))) {
            level = ERROR;
        }

        for (final String metric : payload.metricNames()) {
            final Object metricValue = payload.getMetric(metric);
            TypedValue<?> val = TypedValues.newStringValue("");
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
            final WireField wireField = new WireField(metric, val, level);
            wireFields.add(wireField);
        }
        return new WireRecord(wireFields.toArray(new WireField[0]));
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
        // close the client
        try {
            unsubsribe();
            this.cloudClient.removeCloudClientListener(this);
        } catch (final KuraException e) {
            logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        // close the disconnect manager
        logger.debug(wireMessages.deactivatingCloudSubscriberDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEstablished() {
        try {
            if (this.topic != null) {
                subscribeTopic();
            }
        } catch (final KuraException e) {
            logger.error(wireMessages.errorCreatingCloudClinet() + e);
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
        if (this.topic != null && this.cloudClient != null) {
            this.cloudClient.unsubscribe(this.topic);
        }
        this.topic = this.options.getSubscribingTopic();
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
        this.options = new CloudSubscriberOptions(properties);

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        logger.debug(wireMessages.updatingCloudSubscriberDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
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
    public void onConnectionLost() {
        // Not required
    }

}
