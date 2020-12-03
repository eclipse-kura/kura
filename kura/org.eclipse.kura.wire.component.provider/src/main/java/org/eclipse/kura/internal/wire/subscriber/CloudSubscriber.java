/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class CloudSubscriber is the specific Wire Component to subscribe a list
 * of {@link WireRecord}s as received in {@link org.eclipse.kura.wire.WireEnvelope} from the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every {@link WireRecord} as found in {@link org.eclipse.kura.wire.WireEnvelope} will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform. Unlike Cloud Publisher Wire
 * Component, the user can only avail to wrap every {@link WireRecord} in the default
 * Google Protobuf Payload.
 */
public final class CloudSubscriber implements WireEmitter, ConfigurableComponent, CloudSubscriberListener {

    private static final Logger logger = LogManager.getLogger(CloudSubscriber.class);

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber cloudSubscriber;
    private CloudSubscriberOptions options;

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

    public void setCloudSubscriber(org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber cloudSubscriber) {
        this.cloudSubscriber = cloudSubscriber;
        this.cloudSubscriber.registerCloudSubscriberListener(CloudSubscriber.this);
    }

    public void unsetCloudSubscriber(org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber cloudSubscriber) {
        cloudSubscriber.unregisterCloudSubscriberListener(CloudSubscriber.this);
        if (this.cloudSubscriber == cloudSubscriber) {
            this.cloudSubscriber = null;
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
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        this.options = new CloudSubscriberOptions(properties);
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

        this.options = new CloudSubscriberOptions(properties);

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

        logger.debug("Deactivating Cloud Subscriber Wire Component... Done");
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wires) {
        return this.wireSupport.polled(wires);
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

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

        final Optional<String> bodyProperty = this.options.getBodyProperty();

        if (bodyProperty.isPresent()) {
            emitBody(wireProperties, payload, bodyProperty.get(), this.options.getBodyPropertyType());
        }

        final WireRecord wireRecord = new WireRecord(wireProperties);
        return Arrays.asList(wireRecord);
    }

    private void emitBody(final Map<String, TypedValue<?>> wireRecordProperties, final KuraPayload payload,
            final String property, final DataType dataType) {
        try {
            final byte[] body = payload.getBody();

            if (body == null) {
                return;
            }

            if (dataType == DataType.BYTE_ARRAY) {
                wireRecordProperties.put(property, new ByteArrayValue(body));
            } else {
                wireRecordProperties.put(property, new StringValue(new String(body, StandardCharsets.UTF_8)));
            }
        } catch (final Exception e) {
            logger.warn("failed to emit body", e);
        }
    }

    @Override
    public void onMessageArrived(KuraMessage message) {
        this.wireSupport.emit(buildWireRecord(message.getPayload()));
    }
}
