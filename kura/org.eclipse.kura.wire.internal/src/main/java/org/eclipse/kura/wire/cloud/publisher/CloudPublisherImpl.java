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

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * The Class CloudPublisherImpl is the implementation of {@link CloudPublisher}
 * to publish a list of wire records as received in Wire Envelope to the
 * configured cloud platform
 */
public final class CloudPublisherImpl
		implements WireReceiver, DataServiceListener, ConfigurableComponent, CloudPublisher {

	// FIXME: Add option to select the format of the message being published:
	// KuraProtoBuf or JSON

	/** The Cloud Publisher Disconnection Manager. */
	private static CloudPublisherDisconnectManager s_disconnectManager;

	/** The Logger. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherImpl.class);

	/** The cloud client. */
	private CloudClient m_cloudClient;

	/** The cloud service. */
	private volatile CloudService m_cloudService;

	/** The data service. */
	private volatile DataService m_dataService;

	/** The cloud publisher options. */
	private CloudPublisherOptions m_options;

	/** The wire supporter component. */
	private WireSupport m_wireSupport;

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
		this.m_wireSupport = WireSupport.of(this);
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
	 * Builds the kura payload.
	 *
	 * @param dataRecord
	 *            the data record
	 * @return the kura payload
	 */
	private KuraPayload buildKuraPayload(final WireRecord dataRecord) {
		final KuraPayload kuraPayload = new KuraPayload();

		if (dataRecord.getTimestamp() != null) {
			kuraPayload.setTimestamp(dataRecord.getTimestamp());
		}

		if (dataRecord.getPosition() != null) {
			kuraPayload.setPosition(this.buildKuraPosition(dataRecord.getPosition()));
		}

		for (final WireField dataField : dataRecord.getFields()) {
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
	 * Builds the kura position.
	 *
	 * @param position
	 *            the position
	 * @return the kura position
	 */
	private KuraPosition buildKuraPosition(final Position position) {
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
		synchronized (s_disconnectManager) {
			if (s_disconnectManager != null) {
				s_disconnectManager.stop();
			}
			s_disconnectManager = null;
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
		this.m_wireSupport.producersConnected(wires);
	}

	/** {@inheritDoc} */
	@Override
	public void publish(final List<WireRecord> wireRecords) {
		if (this.m_cloudClient == null) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "CloudClient not available");
		}

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

					// prepare the payload
					final KuraPayload kuraPayload = this.buildKuraPayload(dataRecord);

					// publish the payload
					this.m_cloudClient.publish(appTopic, kuraPayload, this.m_options.getPublishingQos(),
							this.m_options.getPublishingRetain(), this.m_options.getPublishingPriority());
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
	public void setCloudService(final CloudService cloudService) {
		this.m_cloudService = cloudService;
	}

	/**
	 * Sets the data service.
	 *
	 * @param dataService
	 *            the new data service
	 */
	public void setDataService(final DataService dataService) {
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
	 */
	private void stopPublishing() {
		if (this.m_cloudClient == null) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "CloudClient not available");
		}

		if (this.m_dataService.isConnected() && !this.m_dataService.isAutoConnectEnabled()) {
			final AutoConnectMode autoConnMode = this.m_options.getAutoConnectMode();
			switch (autoConnMode) {
			case AUTOCONNECT_MODE_OFF:
			case AUTOCONNECT_MODE_ON_AND_STAY:
				// nothing to do. Connection is either not opened or should not
				// be closed.
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
	public void unsetCloudService(final CloudService cloudService) {
		this.m_cloudService = null;
	}

	/**
	 * Unset data service.
	 *
	 * @param dataService
	 *            the data service
	 */
	public void unsetDataService(final DataService dataService) {
		this.m_dataService = null;
	}

	/**
	 * OSGi Service Component callback for updating.
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info("Updating Cloud Publisher Wire Component...");

		// Update properties
		this.m_options = new CloudPublisherOptions(properties);

		// create the singleton disconnect manager
		synchronized (s_disconnectManager) {
			if (s_disconnectManager != null) {

				s_disconnectManager.setQuieceTimeout(this.m_options.getAutoConnectQuieceTimeout());

				final int minDelay = this.m_options.getAutoConnectMode().getDisconnectDelay();
				s_disconnectManager.disconnectInMinutes(minDelay);
			}
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
