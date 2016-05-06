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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudPublisherOptions is responsible to provide all the required
 * options for the Cloud Publisher Wire Component
 */
public final class CloudPublisherOptions {

	/**
	 * The different Auto Connect Modes.
	 */
	public static enum AutoConnectMode {

		/** The autoconnect mode off. */
		AUTOCONNECT_MODE_OFF(-2),
		/** The autoconnect mode on and off. */
		AUTOCONNECT_MODE_ON_AND_OFF(0),
		/** The autoconnect mode on and stay. */
		AUTOCONNECT_MODE_ON_AND_STAY(-1),
		/** The AUTOCONNECT ON and STAY for 1 min. */
		AUTOCONNECT_MODE_ON_AND_STAY_1_MIN(1),
		/** The AUTOCONNECT ON and STAY for 10 min. */
		AUTOCONNECT_MODE_ON_AND_STAY_10_MIN(10),
		/** The AUTOCONNECT ON and STAY for 15 min. */
		AUTOCONNECT_MODE_ON_AND_STAY_15_MIN(15),
		/** The AUTOCONNECT ON and STAY for 30 min. */
		AUTOCONNECT_MODE_ON_AND_STAY_30_MIN(30),
		/** The AUTOCONNECT ON and STAY for 5 min. */
		AUTOCONNECT_MODE_ON_AND_STAY_5_MIN(5),
		/** The AUTOCONNECT ON and STAY for 60 min. */
		AUTOCONNECT_MODE_ON_AND_STAY_60_MIN(60);

		/** The disconnect delay. */
		private int disconnectDelay;

		/**
		 * Instantiates a new auto connect mode.
		 *
		 * @param disconnectDelay
		 *            the disconnect delay
		 */
		private AutoConnectMode(final int disconnectDelay) {
			this.disconnectDelay = disconnectDelay;
		}

		/**
		 * Gets the disconnect delay.
		 *
		 * @return the disconnect delay
		 */
		public int getDisconnectDelay() {
			return this.disconnectDelay;
		}
	}

	/** The Constant denoting the publisher application. */
	private static final String CONF_APPLICATION = "publish.application";;

	/** The Constant denoting autoconnect mode. */
	private static final String CONF_AUTOCONNECT_MODE = "autoconnect.mode";

	/** The Constant denoting wire emitters. */
	private static final String CONF_EMITTERS = "data.emitters";

	/** The Constant denoting priority. */
	private static final String CONF_PRIORITY = "publish.priority";

	/** The Constant denotiy QoS. */
	private static final String CONF_QOS = "publish.qos";

	/** The Constant denoting quiece timeout. */
	private static final String CONF_QUIECE_TIMEOUT = "autoconnect.quiceTimeout";

	/** The Constant denoting mqtt retain */
	private static final String CONF_RETAIN = "publish.retain";

	/** The Constant denoting mqtt topic. */
	private static final String CONF_TOPIC = "publish.topic";

	/** The Constant application to perform (either publish or subscribe). */
	private static final String DEFAULT_APPLICATION = "PUB";

	/** The Constant denoting default auto connect mode. */
	private static final AutoConnectMode DEFAULT_AUTOCONNECT_MODE = AutoConnectMode.AUTOCONNECT_MODE_ON_AND_OFF;

	/** The Constant denoting default priority. */
	private static final int DEFAULT_PRIORITY = 7;

	/** The Constant denoting default QoS. */
	private static final int DEFAULT_QOS = 0;

	/** The Constant DEFAULT_QUIECE_TIMEOUT. */
	private static final int DEFAULT_QUIECE_TIMEOUT = 1000;

	/** The Constant denoting default mqtt retain. */
	private static final boolean DEFAULT_RETAIN = false;

	/** The Constant denoting default mqtt topic. */
	private static final String DEFAULT_TOPIC = "EVENT";

	/** The Logger. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherOptions.class);

	/** The service component properties container. */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new cloud publisher options.
	 *
	 * @param properties
	 *            the properties
	 */
	public CloudPublisherOptions(final Map<String, Object> properties) {
		this.m_properties = properties;
	}

	/**
	 * Returns the retain to be used for message publishing.
	 *
	 * @return the auto connect mode
	 */
	public AutoConnectMode getAutoConnectMode() {
		AutoConnectMode autoConnectMode = DEFAULT_AUTOCONNECT_MODE;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_AUTOCONNECT_MODE) != null)
				&& (this.m_properties.get(CONF_AUTOCONNECT_MODE) instanceof String)) {
			final String autoconnectModeValue = (String) this.m_properties.get(CONF_AUTOCONNECT_MODE);
			try {
				autoConnectMode = AutoConnectMode.valueOf(autoconnectModeValue);
			} catch (final IllegalArgumentException iea) {
				s_logger.warn("Invalid autoconnect mode:" + autoconnectModeValue, iea);
			}
		}
		return autoConnectMode;
	}

	/**
	 * Returns the QoS to be used for message publishing.
	 *
	 * @return the auto connect quiece timeout
	 */
	public int getAutoConnectQuieceTimeout() {
		int quieceTimeout = DEFAULT_QUIECE_TIMEOUT;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_QUIECE_TIMEOUT) != null)
				&& (this.m_properties.get(CONF_QUIECE_TIMEOUT) instanceof Integer)) {
			quieceTimeout = (Integer) this.m_properties.get(CONF_QUIECE_TIMEOUT);
		}
		return quieceTimeout;
	}

	/**
	 * Returns the topic to be used for message publishing.
	 *
	 * @return the publishing application
	 */
	public String getPublishingApplication() {
		String publishingApp = DEFAULT_APPLICATION;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_APPLICATION) != null)
				&& (this.m_properties.get(CONF_APPLICATION) instanceof String)) {
			publishingApp = (String) this.m_properties.get(CONF_APPLICATION);
		}
		return publishingApp;
	}

	/**
	 * Returns the priority to be used for message publishing.
	 *
	 * @return the publishing priority
	 */
	public int getPublishingPriority() {
		int publishingPriority = DEFAULT_PRIORITY;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_PRIORITY) != null)
				&& (this.m_properties.get(CONF_PRIORITY) instanceof Integer)) {
			publishingPriority = (Integer) this.m_properties.get(CONF_PRIORITY);
		}
		return publishingPriority;
	}

	/**
	 * Returns the QoS to be used for message publishing.
	 *
	 * @return the publishing qos
	 */
	public int getPublishingQos() {
		int publishingQos = DEFAULT_QOS;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_QOS) != null)
				&& (this.m_properties.get(CONF_QOS) instanceof Integer)) {
			publishingQos = (Integer) this.m_properties.get(CONF_QOS);
		}
		return publishingQos;
	}

	/**
	 * Returns the retain to be used for message publishing.
	 *
	 * @return the publishing retain
	 */
	public boolean getPublishingRetain() {
		boolean publishingRetain = DEFAULT_RETAIN;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_RETAIN) != null)
				&& (this.m_properties.get(CONF_RETAIN) instanceof Integer)) {
			publishingRetain = (Boolean) this.m_properties.get(CONF_RETAIN);
		}
		return publishingRetain;
	}

	/**
	 * Returns the topic to be used for message publishing.
	 *
	 * @return the publishing topic
	 */
	public String getPublishingTopic() {
		String publishingTopic = DEFAULT_TOPIC;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_TOPIC) != null)
				&& (this.m_properties.get(CONF_TOPIC) instanceof String)) {
			publishingTopic = (String) this.m_properties.get(CONF_TOPIC);
		}
		return publishingTopic;
	}

	/**
	 * Returns the emitters to be used for message publishing.
	 *
	 * @return the subscribed emitters
	 */
	public String[] getSubscribedEmitters() {
		String[] emitteres = {};
		if ((this.m_properties != null) && (this.m_properties.get(CONF_EMITTERS) != null)
				&& (this.m_properties.get(CONF_EMITTERS) instanceof String)) {
			final String emittersStr = (String) this.m_properties.get(CONF_EMITTERS);
			emitteres = emittersStr.split(",");
		}
		return emitteres;
	}
}
