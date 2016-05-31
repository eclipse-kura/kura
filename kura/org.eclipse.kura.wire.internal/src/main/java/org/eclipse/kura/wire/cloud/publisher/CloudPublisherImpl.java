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

import static org.eclipse.kura.device.util.Preconditions.checkCondition;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.wire.CloudPublisher;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.cloud.publisher.CloudPublisherOptions.AutoConnectMode;
import org.eclipse.kura.wire.util.Wires;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Monitor;

/**
 * The Class CloudPublisherImpl is the implementation of {@link CloudPublisher}
 * to publish a list of wire records as received in Wire Envelope to the
 * configured cloud platform.
 */
@Beta
public final class CloudPublisherImpl
		implements WireReceiver, DataServiceListener, ConfigurableComponent, CloudPublisher {

	/** The Cloud Publisher Disconnection Manager. */
	private static CloudPublisherDisconnectManager s_disconnectManager;

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherImpl.class);

	/** The cloud client. */
	private CloudClient m_cloudClient;

	/** The cloud service. */
	private volatile CloudService m_cloudService;

	/** The data service. */
	private volatile DataService m_dataService;

	/** Synchronization Monitor. */
	private final Monitor m_monitor;

	/** The cloud publisher options. */
	private CloudPublisherOptions m_options;

	/** The wire supporter component. */
	private WireSupport m_wireSupport;

	/**
	 * Instantiates a new cloud publisher instance.
	 */
	public CloudPublisherImpl() {
		this.m_monitor = new Monitor();
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
		s_logger.info("Activating Cloud Publisher Wire Component...");
		this.m_wireSupport = Wires.newWireSupport(this);
		// Update properties
		this.m_options = new CloudPublisherOptions(properties);

		// create the singleton disconnect manager
		if (s_disconnectManager == null) {
			s_disconnectManager = new CloudPublisherDisconnectManager(this.m_dataService,
					this.m_options.getAutoConnectQuieceTimeout());
		}

		// recreate the CloudClient
		try {
			this.setupCloudClient();
		} catch (final KuraException e) {
			s_logger.warn("Cannot setup CloudClient.." + Throwables.getStackTraceAsString(e));
		}
		s_logger.info("Activating Cloud Publisher Wire Component...Done");
	}

	/**
	 * Builds the JSON instance from the provided wire record.
	 *
	 * @param wireRecord
	 *            the wire record
	 * @return the json instance
	 * @throws KuraRuntimeException
	 *             if the wire record provided is null
	 */
	private JSONObject buildJsonObject(final WireRecord wireRecord) {
		checkCondition(wireRecord == null, "Wire Record cannot be null");

		final JSONObject jsonObject = new JSONObject();
		try {
			if (wireRecord.getTimestamp() != null) {
				jsonObject.put("timestamp", wireRecord.getTimestamp());
			}

			if (wireRecord.getPosition() != null) {
				jsonObject.put("position", this.buildKuraPositionForJson(wireRecord.getPosition()));
			}

			for (final WireField dataField : wireRecord.getFields()) {
				Object value = null;
				switch (dataField.getValue().getType()) {
				case STRING:
					value = dataField.getValue().getValue();
					break;
				case DOUBLE:
					value = dataField.getValue().getValue();
					break;
				case INTEGER:
					value = dataField.getValue().getValue();
					break;
				case LONG:
					value = dataField.getValue().getValue();
					break;
				case BOOLEAN:
					value = dataField.getValue().getValue();
					break;
				case BYTE_ARRAY:
					value = dataField.getValue().getValue();
					break;
				case BYTE:
					value = ((Byte) dataField.getValue().getValue()).intValue();
					break;
				case SHORT:
					value = ((Short) dataField.getValue().getValue()).intValue();
					break;
				default:
					break;
				}
				jsonObject.put(dataField.getName(), value);
			}
		} catch (final Exception ex) {
			s_logger.error("Error while building JSON instance from the wire records..."
					+ Throwables.getStackTraceAsString(ex));
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
		checkCondition(wireRecord == null, "Wire Record cannot be null");

		final KuraPayload kuraPayload = new KuraPayload();

		if (wireRecord.getTimestamp() != null) {
			kuraPayload.setTimestamp(wireRecord.getTimestamp());
		}

		if (wireRecord.getPosition() != null) {
			kuraPayload.setPosition(this.buildKuraPosition(wireRecord.getPosition()));
		}

		for (final WireField dataField : wireRecord.getFields()) {
			Object value = null;
			switch (dataField.getValue().getType()) {
			case STRING:
				value = dataField.getValue().getValue();
				break;
			case DOUBLE:
				value = dataField.getValue().getValue();
				break;
			case INTEGER:
				value = dataField.getValue().getValue();
				break;
			case LONG:
				value = dataField.getValue().getValue();
				break;
			case BOOLEAN:
				value = dataField.getValue().getValue();
				break;
			case BYTE_ARRAY:
				value = dataField.getValue().getValue();
				break;
			case BYTE:
				value = ((Byte) dataField.getValue().getValue()).intValue();
				break;
			case SHORT:
				value = ((Short) dataField.getValue().getValue()).intValue();
				break;
			default:
				break;
			}
			kuraPayload.addMetric(dataField.getName(), value);
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
		checkCondition(position == null, "Position cannot be null");

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
		checkCondition(position == null, "Position cannot be null");

		final JSONObject jsonObject = new JSONObject();
		if (position.getLatitude() != null) {
			jsonObject.put("latitude", position.getLatitude().getValue());
		}
		if (position.getLongitude() != null) {
			jsonObject.put("longitude", position.getLongitude().getValue());
		}
		if (position.getAltitude() != null) {
			jsonObject.put("altitude", position.getAltitude().getValue());
		}
		if (position.getSpeed() != null) {
			jsonObject.put("speed", position.getSpeed().getValue());
		}
		if (position.getTrack() != null) {
			jsonObject.put("heading", position.getTrack().getValue());
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
		s_logger.info("Deactivating Cloud Publisher Wire Component...");

		// close the client
		this.closeCloudClient();

		// close the disconnect manager
		this.m_monitor.enter();
		try {
			if (s_disconnectManager != null) {
				s_disconnectManager.stop();
			}
			s_disconnectManager = null;
		} finally {
			this.m_monitor.leave();
		}

		// no need to release the cloud clients as the updated application
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dataService = null;
		this.m_cloudService = null;
		s_logger.info("Deactivating Cloud Publisher Wire Component...Done");
	}

	/**
	 * Gets the cloud service.
	 *
	 * @return the cloud service
	 */
	public CloudService getCloudService() {
		return this.m_cloudService;
	}

	/**
	 * Gets the data service.
	 *
	 * @return the data service
	 */
	public DataService getDataService() {
		return this.m_dataService;
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return this.getClass().getName();
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
		s_logger.info("Received WireEnvelope from {}", wireEnvelope.getEmitterName());
		this.publish(wireEnvelope.getRecords());
		this.stopPublishing();
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		checkCondition(wires == null, "Wires cannot be null");
		this.m_wireSupport.producersConnected(wires);
	}

	/** {@inheritDoc} */
	@Override
	public void publish(final List<WireRecord> wireRecords) {
		checkCondition(this.m_cloudClient == null, "Cloud Client cannot be null");
		checkCondition(wireRecords == null, "Wire Records cannot be null");

		if (!AutoConnectMode.AUTOCONNECT_MODE_OFF.equals(this.m_options.getAutoConnectMode())
				&& !this.m_dataService.isAutoConnectEnabled() && !this.m_dataService.isConnected()) {

			// FIXME: this connect should be a connectWithRetry
			// While the CloudPublisher is active the connection should be in
			// retry mode
			// m_dataService.connectAndStayConnected();
			try {
				this.m_dataService.connect();
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
			} catch (final KuraException e) {
				s_logger.error("Error in publishing wire records using cloud publisher.."
						+ Throwables.getStackTraceAsString(e));
			}

		}
	}

	/**
	 * Sets the cloud service.
	 *
	 * @param cloudService
	 *            the new cloud service
	 */
	public synchronized void setCloudService(final CloudService cloudService) {
		this.m_cloudService = cloudService;
	}

	/**
	 * Sets the data service.
	 *
	 * @param dataService
	 *            the new data service
	 */
	public synchronized void setDataService(final DataService dataService) {
		this.m_dataService = dataService;
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
		checkCondition(this.m_cloudClient == null, "Cloud Client cannot be null");

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
	 * Unset cloud service.
	 *
	 * @param cloudService
	 *            the cloud service
	 */
	public synchronized void unsetCloudService(final CloudService cloudService) {
		this.m_cloudService = null;
	}

	/**
	 * Unset data service.
	 *
	 * @param dataService
	 *            the data service
	 */
	public synchronized void unsetDataService(final DataService dataService) {
		this.m_dataService = null;
	}

	/**
	 * OSGi Service Component callback for updating.
	 *
	 * @param properties
	 *            the updated properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info("Updating Cloud Publisher Wire Component...");

		// Update properties
		this.m_options = new CloudPublisherOptions(properties);

		// create the singleton disconnect manager
		this.m_monitor.enter();
		try {
			if (s_disconnectManager != null) {
				s_disconnectManager.setQuieceTimeout(this.m_options.getAutoConnectQuieceTimeout());

				final int minDelay = this.m_options.getAutoConnectMode().getDisconnectDelay();
				s_disconnectManager.disconnectInMinutes(minDelay);
			}
		} finally {
			this.m_monitor.leave();
		}

		// recreate the CloudClient
		try {
			this.setupCloudClient();
		} catch (final KuraException e) {
			s_logger.warn("Cannot setup CloudClient..." + Throwables.getStackTraceAsString(e));
		}
		s_logger.info("Updating Cloud Publisher Wire Component...Done");
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
