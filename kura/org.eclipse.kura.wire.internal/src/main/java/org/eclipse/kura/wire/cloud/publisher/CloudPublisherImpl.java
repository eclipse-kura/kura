/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire.cloud.publisher;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
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
import org.eclipse.kura.wire.cloud.publisher.CloudPublisherOptions.AutoConnectMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudPublisherImpl is the implementation of {@link CloudPublisher}
 * to publish a list of wire records as received in Wire Envelope to the
 * configured cloud platform.
 */
public final class CloudPublisherImpl implements WireReceiver, DataServiceListener, ConfigurableComponent {

	/** The Cloud Publisher Disconnection Manager. */
	private static CloudPublisherDisconnectManager s_disconnectManager;

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherImpl.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The cloud client. */
	private CloudClient m_cloudClient;

	/** The cloud service. */
	private volatile CloudService m_cloudService;

	/** The data service. */
	private volatile DataService m_dataService;

	/** Synchronization Monitor. */
	private final Lock m_monitor;

	/** The cloud publisher options. */
	private CloudPublisherOptions m_options;

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** The wire supporter component. */
	private WireSupport m_wireSupport;

	/**
	 * Instantiates a new cloud publisher instance.
	 */
	public CloudPublisherImpl() {
		this.m_monitor = new ReentrantLock();
	}

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
		s_logger.debug(s_message.activatingCloudPublisher());
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
		// Update properties
		this.m_options = new CloudPublisherOptions(properties);
		// create the singleton disconnect manager
		if (s_disconnectManager == null) {
			s_disconnectManager = new CloudPublisherDisconnectManager(this.m_dataService,
					this.m_options.getAutoConnectQuiesceTimeout());
		}
		// recreate the CloudClient
		try {
			this.setupCloudClient();
		} catch (final KuraException e) {
			s_logger.error(s_message.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.activatingCloudPublisherDone());
	}

	/**
	 * Binds the cloud service.
	 *
	 * @param cloudService
	 *            the new cloud service
	 */
	public synchronized void bindCloudService(final CloudService cloudService) {
		if (this.m_cloudService == null) {
			this.m_cloudService = cloudService;
		}
	}

	/**
	 * Binds the data service.
	 *
	 * @param dataService
	 *            the new data service
	 */
	public synchronized void bindDataService(final DataService dataService) {
		if (this.m_dataService == null) {
			this.m_dataService = dataService;
		}
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
	}

	/**
	 * Builds the JSON instance from the provided wire record.
	 *
	 * @param wireRecord
	 *            the wire record
	 * @return the JSON instance
	 * @throws KuraRuntimeException
	 *             if the wire record provided is null
	 */
	private JSONObject buildJsonObject(final WireRecord wireRecord) throws JSONException {
		checkNull(wireRecord, s_message.wireRecordNonNull());
		final JSONObject jsonObject = new JSONObject();
		if (wireRecord.getTimestamp() != null) {
			jsonObject.put(s_message.timestamp(), wireRecord.getTimestamp());
		}
		if (wireRecord.getPosition() != null) {
			jsonObject.put(s_message.position(), this.buildKuraPositionForJson(wireRecord.getPosition()));
		}
		for (final WireField dataField : wireRecord.getFields()) {
			final Object wrappedValue = dataField.getValue().getValue();
			jsonObject.put(dataField.getName(), wrappedValue);
		}
		return jsonObject;
	}

	/**
	 * Builds the kura payload from the provided wire record.
	 *
	 * @param wireRecord
	 *            the wire record
	 * @return the kura payload
	 * @throws KuraRuntimeException
	 *             if the wire record provided is null
	 */
	private KuraPayload buildKuraPayload(final WireRecord wireRecord) {
		checkNull(wireRecord, s_message.wireRecordNonNull());
		final KuraPayload kuraPayload = new KuraPayload();

		if (wireRecord.getTimestamp() != null) {
			kuraPayload.setTimestamp(wireRecord.getTimestamp());
		}
		if (wireRecord.getPosition() != null) {
			kuraPayload.setPosition(this.buildKuraPosition(wireRecord.getPosition()));
		}
		for (final WireField dataField : wireRecord.getFields()) {
			final Object wrappedValue = dataField.getValue().getValue();
			kuraPayload.addMetric(dataField.getName(), wrappedValue);
		}
		return kuraPayload;
	}

	/**
	 * Builds the kura position from the OSGi position instance.
	 *
	 * @param position
	 *            the OSGi position instance
	 * @return the kura position
	 * @throws KuraRuntimeException
	 *             if the position provided is null
	 */
	private KuraPosition buildKuraPosition(final Position position) {
		checkNull(position, s_message.positionNonNull());
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
	 * Builds the kura position from the OSGi position instance.
	 *
	 * @param position
	 *            the OSGi position instance
	 * @return the kura position
	 * @throws JSONException
	 *             if it encounters any JSON parsing specific error
	 * @throws KuraRuntimeException
	 *             if position provided is null
	 */
	private JSONObject buildKuraPositionForJson(final Position position) throws JSONException {
		checkNull(position, s_message.positionNonNull());
		final JSONObject jsonObject = new JSONObject();
		if (position.getLatitude() != null) {
			jsonObject.put(s_message.latitude(), position.getLatitude().getValue());
		}
		if (position.getLongitude() != null) {
			jsonObject.put(s_message.longitude(), position.getLongitude().getValue());
		}
		if (position.getAltitude() != null) {
			jsonObject.put(s_message.altitude(), position.getAltitude().getValue());
		}
		if (position.getSpeed() != null) {
			jsonObject.put(s_message.speed(), position.getSpeed().getValue());
		}
		if (position.getTrack() != null) {
			jsonObject.put(s_message.heading(), position.getTrack().getValue());
		}
		return jsonObject;
	}

	/**
	 * Closes cloud client.
	 */
	private void closeCloudClient() {
		if (this.m_cloudClient != null) {
			this.m_cloudClient.release();
			this.m_cloudClient = null;
		}
	}

	/**
	 * OSGi Service Component callback for deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivatingCloudPublisher());
		// close the client
		this.closeCloudClient();
		// close the disconnect manager
		this.m_monitor.lock();
		try {
			if (s_disconnectManager != null) {
				s_disconnectManager.stop();
			}
			s_disconnectManager = null;
		} finally {
			this.m_monitor.unlock();
		}
		// no need to release the cloud clients as the updated application
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dataService = null;
		this.m_cloudService = null;
		s_logger.debug(s_message.deactivatingCloudPublisherDone());
	}

	/** {@inheritDoc} */
	@Override
	public void onConnectionEstablished() {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void onConnectionLost(final Throwable cause) {
		// nothing to do here; this is managed by the DataService
	}

	/** {@inheritDoc} */
	@Override
	public void onDisconnected() {
		// somebody is calling disconnect, so we stop our timer if any
		s_disconnectManager.stop();
	}

	/** {@inheritDoc} */
	@Override
	public void onDisconnecting() {
		// somebody is calling disconnect, so we stop our timer if any
		s_disconnectManager.stop();
	}

	/** {@inheritDoc} */
	@Override
	public void onMessageArrived(final String topic, final byte[] payload, final int qos, final boolean retained) {
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
		checkNull(wireEnvelope, s_message.wireEnvelopeNonNull());
		s_logger.info(s_message.wireRecordReceived(wireEnvelope.getEmitterPid()));
		this.publish(wireEnvelope.getRecords());
		this.stopPublishing();
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		checkNull(wires, s_message.wiresNonNull());
		this.m_wireSupport.producersConnected(wires);
	}

	/** {@inheritDoc} */
	public void publish(final List<WireRecord> wireRecords) {
		checkNull(this.m_cloudClient, s_message.cloudClientNonNull());
		checkNull(wireRecords, s_message.wireRecordsNonNull());

		if (!AutoConnectMode.AUTOCONNECT_MODE_OFF.equals(this.m_options.getAutoConnectMode())
				&& !this.m_dataService.isAutoConnectEnabled() && !this.m_dataService.isConnected()) {
			try {
				if (!this.m_dataService.isConnected()) {
					this.m_dataService.connect();
				}
				for (final WireRecord dataRecord : wireRecords) {
					// prepare the topic
					final String appTopic = this.m_options.getPublishingTopic();
					if (this.m_options.getMessageType() == 1) { // Kura Payload
						// prepare the payload
						final KuraPayload kuraPayload = this.buildKuraPayload(dataRecord);
						// publish the payload
						this.m_cloudClient.publish(appTopic, kuraPayload, this.m_options.getPublishingQos(),
								this.m_options.getPublishingRetain(), this.m_options.getPublishingPriority());
					}
					if (this.m_options.getMessageType() == 2) { // JSON
						final JSONObject jsonWire = this.buildJsonObject(dataRecord);
						this.m_cloudClient.publish(appTopic, jsonWire.toString().getBytes(),
								this.m_options.getPublishingQos(), this.m_options.getPublishingRetain(),
								this.m_options.getPublishingPriority());
					}
				}
			} catch (final Exception e) {
				s_logger.error(s_message.errorPublishingWireRecords() + ThrowableUtil.stackTraceAsString(e));
			}

		}
	}

	/**
	 * Setup cloud client.
	 *
	 * @throws KuraException
	 *             the kura exception
	 */
	private void setupCloudClient() throws KuraException {
		this.closeCloudClient();
		// create the new CloudClient for the specified application
		final String appId = this.m_options.getPublishingApplication();
		this.m_cloudClient = this.m_cloudService.newCloudClient(appId);
	}

	/**
	 * Stop publishing.
	 *
	 * @throws KuraRuntimeException
	 *             if cloud client is null
	 */
	private void stopPublishing() {
		checkNull(this.m_cloudClient, s_message.cloudClientNonNull());
		if (this.m_dataService.isConnected() && !this.m_dataService.isAutoConnectEnabled()) {
			final AutoConnectMode autoConnMode = this.m_options.getAutoConnectMode();
			switch (autoConnMode) {
			case AUTOCONNECT_MODE_OFF:
			case AUTOCONNECT_MODE_ON_AND_STAY:
				// nothing to do. Connection is either not opened or should not
				// be closed
				break;
			default:
				final int minDelay = this.m_options.getAutoConnectMode().getDisconnectDelay();
				s_disconnectManager.disconnectInMinutes(minDelay);
				break;
			}
		}
	}

	/**
	 * Unbinds cloud service.
	 *
	 * @param cloudService
	 *            the cloud service
	 */
	public synchronized void unbindCloudService(final CloudService cloudService) {
		if (this.m_cloudService == cloudService) {
			this.m_cloudService = null;
		}
	}

	/**
	 * Unbinds data service.
	 *
	 * @param dataService
	 *            the data service
	 */
	public synchronized void unbindDataService(final DataService dataService) {
		if (this.m_dataService == dataService) {
			this.m_dataService = null;
		}
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi Service Component callback for updating.
	 *
	 * @param properties
	 *            the updated properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingCloudPublisher());
		// Update properties
		this.m_options = new CloudPublisherOptions(properties);
		// create the singleton disconnect manager
		this.m_monitor.lock();
		try {
			if (s_disconnectManager != null) {
				s_disconnectManager.setQuiesceTimeout(this.m_options.getAutoConnectQuiesceTimeout());
				final int minDelay = this.m_options.getAutoConnectMode().getDisconnectDelay();
				s_disconnectManager.disconnectInMinutes(minDelay);
			}
		} finally {
			this.m_monitor.unlock();
		}
		// recreate the Cloud Client
		try {
			this.setupCloudClient();
		} catch (final KuraException e) {
			s_logger.error(s_message.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.updatingCloudPublisherDone());
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
