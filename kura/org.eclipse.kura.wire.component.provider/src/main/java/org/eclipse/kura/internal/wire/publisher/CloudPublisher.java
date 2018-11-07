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
package org.eclipse.kura.internal.wire.publisher;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class CloudPublisher is the specific Wire Component to publish a list of
 * {@link WireRecord}s as received in {@link WireEnvelope} to the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every {@link WireRecord} as found in {@link WireEnvelope} will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform.
 */
public final class CloudPublisher implements WireReceiver, ConfigurableComponent {

    private static final Logger logger = LogManager.getLogger(CloudPublisher.class);

    private static final String ASSET_NAME_PROPERTY_KEY = "assetName";

    private CloudPublisherOptions cloudPublisherOptions;

    private volatile WireHelperService wireHelperService;
    private PositionService positionService;

    private WireSupport wireSupport;

    private org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudConnectionPublisher;

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

    public void setPositionService(PositionService positionService) {
        this.positionService = positionService;
    }

    public void unsetPositionService(PositionService positionService) {
        this.positionService = null;
    }

    public void setCloudPublisher(org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudPublisher) {
        this.cloudConnectionPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudPublisher) {
        this.cloudConnectionPublisher = null;
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
        logger.debug("Activating Cloud Publisher Wire Component...");
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        // Update properties
        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        logger.debug("Activating Cloud Publisher Wire Component... Done");
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug("Updating Cloud Publisher Wire Component...");
        // Update properties
        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        logger.debug("Updating Cloud Publisher Wire Component... Done");
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating Cloud Publisher Wire Component...");

        logger.debug("Deactivating Cloud Publisher Wire Component... Done");
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        if (nonNull(this.cloudConnectionPublisher)) {
            final List<WireRecord> records = wireEnvelope.getRecords();
            publish(records);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    /**
     * Builds the Kura payload from the provided {@link WireRecord}.
     *
     * @param wireRecord
     *            the {@link WireRecord}
     * @return the Kura payload
     * @throws NullPointerException
     *             if the {@link WireRecord} provided is null
     */
    private KuraPayload buildKuraPayload(final WireRecord wireRecord) {
        requireNonNull(wireRecord, "Wire Record cannot be null");
        final KuraPayload kuraPayload = new KuraPayload();

        kuraPayload.setTimestamp(new Date());

        if (this.cloudPublisherOptions.getPositionType() != PositionType.NONE) {
            KuraPosition kuraPosition = getPosition();
            kuraPayload.setPosition(kuraPosition);
        }

        for (final Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
            kuraPayload.addMetric(entry.getKey(), entry.getValue().getValue());
        }

        return kuraPayload;
    }

    private KuraPosition getPosition() {
        NmeaPosition position = this.positionService.getNmeaPosition();

        KuraPosition kuraPosition = new KuraPosition();
        kuraPosition.setAltitude(position.getAltitude());
        kuraPosition.setLatitude(position.getLatitude());
        kuraPosition.setLongitude(position.getLongitude());

        if (this.cloudPublisherOptions.getPositionType() == PositionType.FULL) {
            kuraPosition.setHeading(position.getTrack());
            kuraPosition.setPrecision(position.getDOP());
            kuraPosition.setSpeed(position.getSpeed());
            kuraPosition.setSatellites(position.getNrSatellites());
        }

        return kuraPosition;
    }

    /**
     * Publishes the list of provided {@link WireRecord}s
     *
     * @param wireRecords
     *            the provided list of {@link WireRecord}s
     * @throws NullPointerException
     *             if one of the arguments is null
     */
    private void publish(final List<WireRecord> wireRecords) {
        requireNonNull(wireRecords, "Wire Records cannot be null");

        try {
            for (final WireRecord dataRecord : wireRecords) {
                final Map<String, Object> properties = buildKuraMessageProperties(dataRecord);
                final KuraPayload kuraPayload = buildKuraPayload(dataRecord);
                KuraMessage message = new KuraMessage(kuraPayload, properties);
                this.cloudConnectionPublisher.publish(message);
            }
        } catch (final Exception e) {
            logger.error("Error in publishing wire records using cloud publisher..", e);
        }
    }

    private Map<String, Object> buildKuraMessageProperties(final WireRecord wireRecord) {
        Map<String, TypedValue<?>> wireRecordProps = wireRecord.getProperties();

        final Map<String, Object> properties = new HashMap<>();
        if (wireRecordProps.containsKey(ASSET_NAME_PROPERTY_KEY)) {
            properties.put(ASSET_NAME_PROPERTY_KEY, wireRecordProps.get(ASSET_NAME_PROPERTY_KEY).getValue());
        }
        return properties;
    }
}
