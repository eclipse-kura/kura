/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import static java.util.Objects.requireNonNull;

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
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.util.position.Position;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * The Class CloudPublisher is the specific Wire Component to publish a list of
 * wire records as received in Wire Envelope to the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every Wire Record as found in Wire Envelope will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform. In addition, the user can
 * avail to wrap every Wire Record as a JSON object as well.
 */
public final class CloudPublisher implements WireReceiver, CloudClientListener, ConfigurableComponent {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class CloudPublisherServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudService, CloudService> {

        @Override
        public CloudService addingService(ServiceReference<CloudService> reference) {
            CloudPublisher.this.cloudService = CloudPublisher.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (KuraException e) {
                logger.error(message.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
            }
            return CloudPublisher.this.cloudService;
        }

        @Override
        public void modifiedService(ServiceReference<CloudService> reference, CloudService service) {
            CloudPublisher.this.cloudService = CloudPublisher.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (KuraException e) {
                logger.error(message.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
            }
        }

        @Override
        public void removedService(ServiceReference<CloudService> reference, CloudService service) {
            CloudPublisher.this.cloudService = null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CloudPublisher.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private BundleContext bundleContext;

    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;

    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;

    private volatile CloudService cloudService;

    private CloudClient cloudClient;

    private CloudPublisherOptions cloudPublisherOptions;

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
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
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
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(message.activatingCloudPublisher());
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.bundleContext = componentContext.getBundleContext();

        // Update properties
        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        this.cloudServiceTrackerCustomizer = new CloudPublisherServiceTrackerCustomizer();
        initCloudServiceTracking();

        logger.debug(message.activatingCloudPublisherDone());
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingCloudPublisher());
        // Update properties
        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        logger.debug(message.updatingCloudPublisherDone());
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingCloudPublisher());
        // close the client
        closeCloudClient();

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        logger.debug(message.deactivatingCloudPublisherDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEstablished() {
        // Not required
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
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.info(message.wireEnvelopeReceived(wireEnvelope.getEmitterPid()));
        // filtering list of wire records based on the provided severity level
        final List<WireRecord> records = this.wireSupport.filter(wireEnvelope.getRecords());

        if (this.cloudService != null && this.cloudClient != null) {
            publish(records);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        requireNonNull(wires, message.wiresNonNull());
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    /** {@inheritDoc} */
    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // Not required
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
     * Service tracker to manage Cloud Services
     */
    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.cloudPublisherOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception " + ThrowableUtil.stackTraceAsString(e));
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    /**
     * Builds the JSON instance from the provided wire record.
     *
     * @param wireRecord
     *            the wire record
     * @return the JSON instance
     * @throws NullPointerException
     *             if the wire record provided is null
     */
    private JsonObject buildJsonObject(final WireRecord wireRecord) {
        requireNonNull(wireRecord, message.wireRecordNonNull());
        final JsonObject jsonObject = Json.object();
        if (wireRecord.getTimestamp() != null) {
            jsonObject.add(message.timestamp(), wireRecord.getTimestamp().getTime());
        }
        if (wireRecord.getPosition() != null) {
            jsonObject.add(message.position(), buildKuraPositionForJson(wireRecord.getPosition()));
        }
        for (final WireField dataField : wireRecord.getFields()) {
            final Object wrappedValue = dataField.getValue().getValue();
            jsonObject.add(dataField.getName(), wrappedValue.toString());
        }
        return jsonObject;
    }

    /**
     * Builds the Kura payload from the provided wire record.
     *
     * @param wireRecord
     *            the wire record
     * @return the Kura payload
     * @throws NullPointerException
     *             if the wire record provided is null
     */
    private KuraPayload buildKuraPayload(final WireRecord wireRecord) {
        requireNonNull(wireRecord, message.wireRecordNonNull());
        final KuraPayload kuraPayload = new KuraPayload();

        if (wireRecord.getTimestamp() != null) {
            kuraPayload.setTimestamp(wireRecord.getTimestamp());
        }
        if (wireRecord.getPosition() != null) {
            kuraPayload.setPosition(buildKuraPosition(wireRecord.getPosition()));
        }
        for (final WireField dataField : wireRecord.getFields()) {
            final Object wrappedValue = dataField.getValue().getValue();
            kuraPayload.addMetric(dataField.getName(), wrappedValue);
        }
        return kuraPayload;
    }

    /**
     * Builds the Kura position from the OSGi position instance.
     *
     * @param position
     *            the OSGi position instance
     * @return the Kura position
     * @throws NullPointerException
     *             if the position provided is null
     */
    private KuraPosition buildKuraPosition(final Position position) {
        requireNonNull(position, message.positionNonNull());
        final KuraPosition kuraPosition = new KuraPosition();
        if (position.getLatitude() != null) {
            kuraPosition.setLatitude(position.getLatitude().getValue());
        }
        if (position.getLongitude() != null) {
            kuraPosition.setLongitude(position.getLongitude().getValue());
        }
        if (position.getAltitude() != null) {
            kuraPosition.setAltitude(position.getAltitude().getValue());
        }
        if (position.getSpeed() != null) {
            kuraPosition.setSpeed(position.getSpeed().getValue());
        }
        if (position.getTrack() != null) {
            kuraPosition.setHeading(position.getTrack().getValue());
        }
        return kuraPosition;
    }

    /**
     * Builds the Kura position from the OSGi position instance.
     *
     * @param position
     *            the OSGi position instance
     * @return the Kura position
     * @throws NullPointerException
     *             if position provided is null
     */
    private JsonObject buildKuraPositionForJson(final Position position) {
        requireNonNull(position, message.positionNonNull());
        final JsonObject jsonObject = Json.object();
        if (position.getLatitude() != null) {
            jsonObject.add(message.latitude(), position.getLatitude().getValue());
        }
        if (position.getLongitude() != null) {
            jsonObject.add(message.longitude(), position.getLongitude().getValue());
        }
        if (position.getAltitude() != null) {
            jsonObject.add(message.altitude(), position.getAltitude().getValue());
        }
        if (position.getSpeed() != null) {
            jsonObject.add(message.speed(), position.getSpeed().getValue());
        }
        if (position.getTrack() != null) {
            jsonObject.add(message.heading(), position.getTrack().getValue());
        }
        return jsonObject;
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
     * Publishes the list of provided Wire Records
     *
     * @param wireRecords
     *            the provided list of Wire Records
     */
    private void publish(final List<WireRecord> wireRecords) {
        requireNonNull(this.cloudClient, message.cloudClientNonNull());
        requireNonNull(wireRecords, message.wireRecordsNonNull());

        try {
            for (final WireRecord dataRecord : wireRecords) {
                // prepare the topic
                final String appTopic = this.cloudPublisherOptions.getPublishingTopic();
                if (this.cloudPublisherOptions.getPayloadType() == PayloadType.KURA_PAYLOAD) {
                    publishKuraPayload(dataRecord, appTopic);
                }
                if (this.cloudPublisherOptions.getPayloadType() == PayloadType.JSON) {
                    publishJson(dataRecord, appTopic);
                }
            }
        } catch (final Exception e) {
            logger.error(message.errorPublishingWireRecords() + ThrowableUtil.stackTraceAsString(e));
        }
    }

    private void publishJson(final WireRecord dataRecord, final String appTopic) throws KuraException {
        final JsonObject jsonWire = buildJsonObject(dataRecord);
        this.cloudClient.publish(appTopic, jsonWire.toString().getBytes(),
                this.cloudPublisherOptions.getPublishingQos(), this.cloudPublisherOptions.getPublishingRetain(),
                this.cloudPublisherOptions.getPublishingPriority());
    }

    private void publishKuraPayload(final WireRecord dataRecord, final String appTopic) throws KuraException {
        final KuraPayload kuraPayload = buildKuraPayload(dataRecord);

        if (this.cloudPublisherOptions.isControlMessage()) {
            this.cloudClient.controlPublish(appTopic, kuraPayload, this.cloudPublisherOptions.getPublishingQos(),
                    this.cloudPublisherOptions.getPublishingRetain(),
                    this.cloudPublisherOptions.getPublishingPriority());
        } else {
            this.cloudClient.publish(appTopic, kuraPayload, this.cloudPublisherOptions.getPublishingQos(),
                    this.cloudPublisherOptions.getPublishingRetain(),
                    this.cloudPublisherOptions.getPublishingPriority());
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
        final String appId = this.cloudPublisherOptions.getPublishingApplication();
        this.cloudClient = this.cloudService.newCloudClient(appId);
        this.cloudClient.addCloudClientListener(this);
    }
}
