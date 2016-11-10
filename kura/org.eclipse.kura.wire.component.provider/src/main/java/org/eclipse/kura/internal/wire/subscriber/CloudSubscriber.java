/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.wire.SeverityLevel.ERROR;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
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
public final class CloudSubscriber implements WireEmitter, ConfigurableComponent, DataServiceListener {

    /** The Logger instance. */
    private static final Logger s_logger = LoggerFactory.getLogger(CloudSubscriber.class);

    /** Localization Resource */
    private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

    /** The cloud service. */
    private volatile CloudService cloudService;

    /** The data service. */
    private volatile DataService dataService;

    /** The cloud subscriber options. */
    private CloudSubscriberOptions options;

    /** The subscribed topic */
    private String topic;

    /** The Wire Helper Service. */
    private volatile WireHelperService wireHelperService;

    /** The wire supporter component. */
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
        s_logger.debug(s_message.activatingCloudSubscriber());
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.options = new CloudSubscriberOptions(properties);
        this.topic = this.options.getSubscribingTopic();
        try {
            if (this.dataService.isConnected()) {
                this.dataService.subscribe(this.topic, this.options.getSubscribingQos());
            }
        } catch (final KuraException e) {
            s_logger.error(s_message.errorCreatingCloudClinet() + e);
        }
        this.dataService.addDataServiceListener(this);
        s_logger.debug(s_message.activatingCloudSubscriberDone());
    }

    /**
     * Binds the cloud service.
     *
     * @param cloudService
     *            the new cloud service
     */
    public synchronized void bindCloudService(final CloudService cloudService) {
        if (this.cloudService == null) {
            this.cloudService = cloudService;
        }
    }

    /**
     * Binds the data service.
     *
     * @param dataService
     *            the new data service
     */
    public synchronized void bindDataService(final DataService dataService) {
        if (this.dataService == null) {
            this.dataService = dataService;
        }
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
     * @throws KuraRuntimeException
     *             if the payload provided is null
     */
    private WireRecord buildWireRecord(final KuraPayload payload) throws IOException {
        checkNull(payload, s_message.payloadNonNull());
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
        s_logger.debug(s_message.deactivatingCloudSubscriber());
        // close the client
        try {
            this.unsubsribe();
            this.dataService.removeDataServiceListener(this);
        } catch (final KuraException e) {
            s_logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        // close the disconnect manager
        s_logger.debug(s_message.deactivatingCloudSubscriberDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEstablished() {
        try {
            if (this.topic != null) {
                this.dataService.subscribe(this.topic, this.options.getSubscribingQos());
            }
        } catch (final KuraException e) {
            s_logger.error(s_message.errorCreatingCloudClinet() + e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionLost(final Throwable cause) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onDisconnected() {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onDisconnecting() {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageArrived(final String topic, final byte[] payload, final int qos, final boolean retained) {
        KuraPayload kuraPayload = null;
        try {
            kuraPayload = ((CloudPayloadProtoBufDecoder) this.cloudService).buildFromByteArray(payload);
        } catch (final KuraException e) {
            s_logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        if (payload != null) {
            WireRecord record = null;
            try {
                record = this.buildWireRecord(kuraPayload);
            } catch (final IOException e) {
                s_logger.error(ThrowableUtil.stackTraceAsString(e));
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
     * Unbinds cloud service.
     *
     * @param cloudService
     *            the cloud service
     */
    public synchronized void unbindCloudService(final CloudService cloudService) {
        if (this.cloudService == cloudService) {
            this.cloudService = null;
        }
    }

    /**
     * Unbinds data service.
     *
     * @param dataService
     *            the data service
     */
    public synchronized void unbindDataService(final DataService dataService) {
        if (this.dataService == dataService) {
            this.dataService = null;
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
        if (this.topic != null) {
            this.dataService.unsubscribe(this.topic);
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
        s_logger.debug(s_message.updatingCloudSubscriber());
        // recreate the Cloud Client
        try {
            this.unsubsribe();
        } catch (final KuraException e) {
            s_logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        // Update properties
        this.options = new CloudSubscriberOptions(properties);
        s_logger.debug(s_message.updatingCloudSubscriberDone());
    }

}
