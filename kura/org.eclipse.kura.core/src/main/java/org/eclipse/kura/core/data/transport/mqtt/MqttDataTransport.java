/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.data.transport.mqtt;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.KuraTooManyInflightMessagesException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.data.transport.mqtt.MqttClientConfiguration.PersistenceType;
import org.eclipse.kura.core.util.ValidationUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.data.DataTransportListener;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.ssl.SslServiceListener;
import org.eclipse.kura.system.SystemService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttDataTransport implements DataTransportService, MqttCallback, ConfigurableComponent, SslServiceListener {
	private static final Logger s_logger = LoggerFactory.getLogger(MqttDataTransport.class);
	private static final String APP_PID = "service.pid";

	private static final String ENV_JAVA_SECURITY= System.getProperty("java.security.manager");
	private static final String ENV_OSGI_FRAMEWORK_SECURITY= System.getProperty("org.osgi.framework.security");
	private static final String ENV_OSGI_SIGNED_CONTENT_SUPPORT= System.getProperty("osgi.signedcontent.support");
	private static final String ENV_OSGI_FRAMEWORK_TRUST_REPOSITORIES= System.getProperty("org.osgi.framework.trust.repositories");

	private static final String MQTT_SCHEME = "mqtt://";
	private static final String MQTTS_SCHEME = "mqtts://";
	// TODO: add mqtt+ssl for secure mqtt

	private static final String TOPIC_PATTERN = "#([^\\s/]+)"; // '#' followed
	// by one or
	// more
	// non-whitespace
	// but not the
	// '/'
	private static final Pattern s_topicPattern = Pattern.compile(TOPIC_PATTERN);

	private SystemService m_systemService;
	private SslManagerService m_sslManagerService;

	private MqttAsyncClient m_mqttClient;

	private DataTransportListeners m_dataTransportListeners;

	private MqttClientConfiguration m_clientConf;
	private boolean m_newSession;
	private String m_sessionId;

	PersistenceType m_persistenceType;
	MqttClientPersistence m_persistence;

	private Map<String, String> m_topicContext = new HashMap<String, String>();
	private Map<String, Object> m_properties = new HashMap<String, Object>();

	private CryptoService m_cryptoService;
	private ConfigurationService m_configurationService;

	private static final String MQTT_BROKER_URL_PROP_NAME = "broker-url";
	private static final String MQTT_USERNAME_PROP_NAME = "username";
	private static final String MQTT_PASSWORD_PROP_NAME = "password";
	private static final String MQTT_CLIENT_ID_PROP_NAME = "client-id";
	private static final String MQTT_KEEP_ALIVE_PROP_NAME = "keep-alive";
	private static final String MQTT_CLEAN_SESSION_PROP_NAME = "clean-session";
	private static final String MQTT_TIMEOUT_PROP_NAME = "timeout"; // All
	// timeouts
	private static final String MQTT_DEFAULT_VERSION_PROP_NAME = "protocol-version";

	private static final String MQTT_LWT_QOS_PROP_NAME = "lwt.qos";
	private static final String MQTT_LWT_RETAIN_PROP_NAME = "lwt.retain";
	private static final String MQTT_LWT_TOPIC_PROP_NAME = "lwt.topic";
	private static final String MQTT_LWT_PAYLOAD_PROP_NAME = "lwt.payload";

	private static final String CLOUD_ACCOUNT_NAME_PROP_NAME = "topic.context.account-name";

	private static final String PERSISTENCE_TYPE_PROP_NAME = "in-flight.persistence";

	private static final String TOPIC_ACCOUNT_NAME_CTX_NAME = "account-name";
	private static final String TOPIC_DEVICE_ID_CTX_NAME = "client-id";

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}

	public void setSslManagerService(SslManagerService sslManagerService) {
		this.m_sslManagerService = sslManagerService;
	}

	public void unsetSslManagerService(SslManagerService sslManagerService) {
		this.m_sslManagerService = null;
	}

	public void setCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = cryptoService;
	}

	public void unsetCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = null;
	}
	
	public void setConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = configurationService;
	}

	public void unsetConfigurationService(ConfigurationService cryptoService) {
		this.m_configurationService = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating...");

		// We need to catch the configuration exception and activate anyway.
		// Otherwise the ConfigurationService will not be able to track us.
		HashMap<String, Object> decryptedPropertiesMap = new HashMap<String, Object>();

		Iterator<String> keys = properties.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = properties.get(key);
			if (key.equals(MQTT_PASSWORD_PROP_NAME)) {
				try {
					char[] decryptedPassword = m_cryptoService.decryptAes(value.toString().toCharArray());
					decryptedPropertiesMap.put(key, decryptedPassword);
				} catch (Exception e) {
					// e.printStackTrace();
					decryptedPropertiesMap.put(key, value.toString().toCharArray());
				}
			} else {
				decryptedPropertiesMap.put(key, value);
			}
		}

		m_properties.putAll(decryptedPropertiesMap);
		try {
			m_clientConf = buildConfiguration(m_properties);
			setupMqttSession();
		} catch (RuntimeException e) {
			s_logger.error("Invalid client configuration. Service will not be able to connect until the configuration is updated", e);
		}

		ServiceTracker<DataTransportListener, DataTransportListener> listenersTracker = new ServiceTracker<DataTransportListener, DataTransportListener>(
				componentContext.getBundleContext(), DataTransportListener.class, null);

		// Deferred open of tracker to prevent
		// java.lang.Exception: Recursive invocation of
		// ServiceFactory.getService
		// on ProSyst
		m_dataTransportListeners = new DataTransportListeners(listenersTracker);

		// Do nothing waiting for the connect request from the upper layer.
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating...");

		// Before deactivating us, the OSGi container should have first
		// deactivated all dependent components.
		// They should be able to complete whatever is needed,
		// e.g. publishing a special last message,
		// synchronously in their deactivate method and disconnect us cleanly.
		// There shouldn't be anything to do here other then
		// perhaps forcibly disconnecting the MQTT client if not already done.
		if (isConnected()) {
			disconnect(0);
		}

		m_dataTransportListeners.close();
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("Updating...");

		m_properties.clear();

		HashMap<String, Object> decryptedPropertiesMap = new HashMap<String, Object>();

		Iterator<String> keys = properties.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = properties.get(key);
			if (key.equals(MQTT_PASSWORD_PROP_NAME)) {
				try {
					char[] decryptedPassword = m_cryptoService.decryptAes(value.toString().toCharArray());
					decryptedPropertiesMap.put(key, decryptedPassword);
				} catch (Exception e) {
					// e.printStackTrace();
					decryptedPropertiesMap.put(key, value.toString().toCharArray());
				}
			} else {
				decryptedPropertiesMap.put(key, value);
			}
		}

		m_properties.putAll(decryptedPropertiesMap);
		// m_properties.putAll(properties);

		update();

	}

	private void update() {
		boolean wasConnected = isConnected();

		// First notify the Listeners
		// We do nothing other than notifying the listeners which may later
		// request to disconnect and reconnect again.
		m_dataTransportListeners.onConfigurationUpdating(wasConnected);

		// Then update the configuration
		// Throwing a RuntimeException here is fine.
		// Listeners will not be notified of an invalid configuration update.
		s_logger.info("Building new configuration...");
		m_clientConf = buildConfiguration(m_properties);

		// We do nothing other than notifying the listeners which may later
		// request to disconnect and reconnect again.
		m_dataTransportListeners.onConfigurationUpdated(wasConnected);
	}

	// ----------------------------------------------------------------
	//
	// Service APIs
	//
	// ----------------------------------------------------------------

	public synchronized void connect() throws KuraConnectException {
		// We treat this as an application bug.
		if (isConnected()) {
			s_logger.error("Already connected");
			throw new IllegalStateException("Already connected");
			// TODO: define an KuraRuntimeException
		}

		// Attempt to setup the MQTT session
		setupMqttSession();

		if (m_mqttClient == null) {
			s_logger.error("Invalid configuration");
			throw new IllegalStateException("Invalid configuration");
			// TODO: define an KuraRuntimeException
		}

		s_logger.info("# ------------------------------------------------------------");
		s_logger.info("#  Connection Properties");
		s_logger.info("#  broker    = " + m_clientConf.getBrokerUrl());
		s_logger.info("#  clientId  = " + m_clientConf.getClientId());
		s_logger.info("#  username  = " + m_clientConf.getConnectOptions().getUserName());
		s_logger.info("#  password  = XXXXXXXXXXXXXX");
		s_logger.info("#  keepAlive = " + m_clientConf.getConnectOptions().getKeepAliveInterval());
		s_logger.info("#  timeout   = " + m_clientConf.getConnectOptions().getConnectionTimeout());
		s_logger.info("#  cleanSession    = " + m_clientConf.getConnectOptions().isCleanSession());
		s_logger.info("#  MQTT version    = " + getMqttVersionLabel(m_clientConf.getConnectOptions().getMqttVersion()));
		s_logger.info("#  willDestination = " + m_clientConf.getConnectOptions().getWillDestination());
		s_logger.info("#  willMessage     = " + m_clientConf.getConnectOptions().getWillMessage());
		s_logger.info("#");
		s_logger.info("#  Connecting...");

		//
		// connect
		try {
			IMqttToken connectToken = m_mqttClient.connect(m_clientConf.getConnectOptions());
			connectToken.waitForCompletion(getTimeToWaitMillis() * 3);
			s_logger.info("#  Connected!");
			s_logger.info("# ------------------------------------------------------------");
		} catch (MqttException e) {
			s_logger.warn("xxxxx  Connect failed. Forcing disconnect. xxxxx {}", e);
			try {
				// prevent callbacks from a zombie client
				m_mqttClient.setCallback(null);
				m_mqttClient.close();
			} catch (Exception de) {
				s_logger.warn("Forced disconnect exception.", de);
			} finally {
				m_mqttClient = null;
			}
			throw new KuraConnectException(e, "Cannot connect");
		}

		//
		// notify the listeners
		m_dataTransportListeners.onConnectionEstablished(m_newSession);
	}

	public boolean isConnected() {
		if (m_mqttClient != null) {
			return m_mqttClient.isConnected();
		}
		return false;
	}

	public String getBrokerUrl() {
		if (m_clientConf != null) {
			return m_clientConf.getBrokerUrl();
		}
		return "";
	}

	public String getAccountName() {
		if (m_clientConf != null) {
			return m_topicContext.get(TOPIC_ACCOUNT_NAME_CTX_NAME);
		}
		return "";
	}

	public String getUsername() {
		if (m_clientConf != null) {
			return m_clientConf.getConnectOptions().getUserName();
		}
		return "";
	}

	@Override
	public String getClientId() {
		if (m_clientConf != null) {
			return m_clientConf.getClientId();
		}
		return "";
	}

	// TODO: java.lang.reflect.Proxy for every listener in order to catch
	// runtime exceptions thrown by listener implementor and log them.

	public synchronized void disconnect(long quiesceTimeout) {
		// Disconnect the client if it's connected. If it fails log the
		// exception.
		// Don't throw an exception because the caller would not
		// be able to handle it.
		if (isConnected()) {
			s_logger.info("Disconnecting...");

			//
			// notify the listeners
			m_dataTransportListeners.onDisconnecting();

			try {
				IMqttToken token = m_mqttClient.disconnect(quiesceTimeout);
				token.waitForCompletion(getTimeToWaitMillis());
				s_logger.info("Disconnected");
			} catch (MqttException e) {
				s_logger.error("Disconnect failed", e);
			}

			//
			// notify the listeners
			m_dataTransportListeners.onDisconnected();
		} else {
			s_logger.warn("MQTT client already disconnected");
		}
	}

	// ---------------------------------------------------------
	//
	// Subscription Management Methods
	//
	// ---------------------------------------------------------

	@Override
	public void subscribe(String topic, int qos) throws KuraTimeoutException, KuraException, KuraNotConnectedException {

		if (m_mqttClient == null || !m_mqttClient.isConnected()) {
			throw new KuraNotConnectedException("Not connected");
		}

		topic = replaceTopicVariables(topic);

		s_logger.info("Subscribing to topic: {} with QoS: {}", topic, qos);

		try {
			IMqttToken token = m_mqttClient.subscribe(topic, qos);
			token.waitForCompletion(getTimeToWaitMillis());
		} catch (MqttException e) {
			if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_TIMEOUT) {
				s_logger.warn("Timeout subscribing to topic: {}", topic);
				throw new KuraTimeoutException("Timeout subscribing to topic: " + topic, e);
			} else {
				s_logger.error("Cannot subscribe to topic: " + topic, e);
				throw KuraException.internalError(e, "Cannot subscribe to topic: " + topic);
			}
		}
	}

	@Override
	public void unsubscribe(String topic) throws KuraTimeoutException, KuraException, KuraNotConnectedException {

		if (m_mqttClient == null || !m_mqttClient.isConnected()) {
			throw new KuraNotConnectedException("Not connected");
		}

		topic = replaceTopicVariables(topic);

		s_logger.info("Unsubscribing to topic: {}", topic);

		try {
			IMqttToken token = m_mqttClient.unsubscribe(topic);
			token.waitForCompletion(getTimeToWaitMillis());
		} catch (MqttException e) {
			if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_TIMEOUT) {
				s_logger.warn("Timeout unsubscribing to topic: {}", topic);
				throw new KuraTimeoutException("Timeout unsubscribing to topic: " + topic, e);
			} else {
				s_logger.error("Cannot unsubscribe to topic: " + topic, e);
				throw KuraException.internalError(e, "Cannot unsubscribe to topic: " + topic);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.kura.data.DataPublisherService#publish(java.lang.String
	 * , byte[], int, boolean)
	 * 
	 * DataConnectException this can be easily recovered connecting the service.
	 * TooManyInflightMessagesException the caller SHOULD retry publishing the
	 * message at a later time. RuntimeException (unchecked) all other
	 * unrecoverable faults that are not recoverable by the caller.
	 */
	@Override
	public DataTransportToken publish(String topic, byte[] payload, int qos, boolean retain) throws KuraTooManyInflightMessagesException, KuraException,
	KuraNotConnectedException {

		if (m_mqttClient == null || !m_mqttClient.isConnected()) {
			throw new KuraNotConnectedException("Not connected");
		}

		topic = replaceTopicVariables(topic);

		s_logger.info("Publishing message on topic: {} with QoS: {}", topic, qos);

		MqttMessage message = new MqttMessage();
		message.setPayload(payload);
		message.setQos(qos);
		message.setRetained(retain);

		Integer messageId = null;
		try {
			IMqttDeliveryToken token = m_mqttClient.publish(topic, message);
			// At present Paho ALWAYS allocates (gets and increments) internally
			// a message ID,
			// even for messages published with QoS == 0.
			// Of course, for QoS == 0 this "internal" message ID will not hit
			// the wire.
			// On top of that, messages published with QoS == 0 are confirmed
			// in the deliveryComplete callback.
			// Another implementation might behave differently
			// and only allocate a message ID for messages published with QoS >
			// 0.
			// We don't want to rely on this and only return and confirm IDs
			// of messages published with QoS > 0.
			s_logger.debug("Published message with ID: {}", token.getMessageId());
			if (qos > 0) {
				messageId = Integer.valueOf(token.getMessageId());
			}
		} catch (MqttPersistenceException e) {
			// This is probably an unrecoverable internal error
			s_logger.error("Cannot publish on topic: {}", topic, e);
			throw new IllegalStateException("Cannot publish on topic: " + topic, e);
		} catch (MqttException e) {
			if (e.getReasonCode() == MqttException.REASON_CODE_MAX_INFLIGHT) {
				s_logger.info("Too many inflight messages");
				throw new KuraTooManyInflightMessagesException(e, "Too many in-fligh messages");
			} else {
				s_logger.error("Cannot publish on topic: " + topic, e);
				throw KuraException.internalError(e, "Cannot publish on topic: " + topic);
			}
		}

		DataTransportToken token = null;
		if (messageId != null) {
			token = new DataTransportToken(messageId, m_sessionId);
		}

		return token;
	}

	// ---------------------------------------------------------
	//
	// MqttCallback methods
	//
	// ---------------------------------------------------------
	@Override
	public void connectionLost(final Throwable cause) {
		s_logger.warn("Connection Lost", cause);

		// notify the listeners
		m_dataTransportListeners.onConnectionLost(cause);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {

		if (token == null) {
			s_logger.error("null token");
			return;
		}

		// Weird, tokens related to messages published with QoS > 0 have a null
		// nested message

		MqttMessage msg = null;
		try {
			msg = token.getMessage();
		} catch (MqttException e) {
			s_logger.error("Cannot get message", e);
			return;
		}

		if (msg != null) {
			// Note that Paho call this also for messages published with QoS ==
			// 0.
			// We don't want to rely on that and we drop asynchronous confirms
			// for QoS == 0.
			int qos = msg.getQos();

			if (qos == 0) {
				s_logger.debug("Ignoring deliveryComplete for messages published with QoS == 0");
				return;
			}
		}

		int id = token.getMessageId();

		s_logger.debug("Delivery complete for message with ID: {}", id);

		// FIXME: We should be more selective here and only call the listener
		// that actually published the message.
		// Anyway we don't have such a mapping and so the publishers MUST track
		// their own
		// identifiers and filter confirms.

		// FIXME: it can happen that the listener that has published the message
		// has not come up yet.
		// This is the scenario:
		// * Paho has some in-flight messages.
		// * Kura gets stopped, crashes or the power is removed.
		// * Kura starts again, Paho connects and restores in-flight messages
		// from its persistence.
		// * These messages are delivered (this callback gets called) before the
		// publisher (also a DataPublisherListener)
		// * has come up (not yet tracked by the OSGi container).
		// These confirms will be lost!

		// notify the listeners
		DataTransportToken dataPublisherToken = new DataTransportToken(id, m_sessionId);
		m_dataTransportListeners.onMessageConfirmed(dataPublisherToken);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		s_logger.debug("Message arrived on topic: {}", topic);

		// FIXME: we should be more selective here and only call the listeners
		// actually subscribed to this topic.
		// Anyway we don't have such a mapping so the listeners are responsible
		// to filter messages.

		// FIXME: the same argument about lost confirms applies to arrived
		// messages.

		// notify the listeners
		m_dataTransportListeners.onMessageArrived(topic, message.getPayload(), message.getQos(), message.isRetained());
	}

	private long getTimeToWaitMillis() {
		// We use the same value for every timeout
		long timeout = m_clientConf.getConnectOptions().getConnectionTimeout() * 1000L;
		return timeout;
	}

	// ---------------------------------------------------------
	//
	// SslServiceListener Overrides
	//
	// ---------------------------------------------------------
	@Override
	public void onConfigurationUpdated() {
		// The SSL service was update, build a new socket connection
		update();
	}

	/*
	 * This method builds an internal configuration option needed by the client
	 * to connect. The configuration is assembled from various sources:
	 * component configuration, SystemService, NetworkService, etc. The returned
	 * configuration is valid so no further validation is needed. If a valid
	 * configuration cannot be assembled the method throws a RuntimeException
	 * (assuming that this error is unrecoverable).
	 */
	private MqttClientConfiguration buildConfiguration(Map<String, Object> properties) {

		MqttClientConfiguration clientConfiguration = null;
		MqttConnectOptions conOpt = new MqttConnectOptions();
		String clientId = null;
		String brokerUrl = null;
		try {
			// Configure the client ID
			clientId = (String) properties.get(MQTT_CLIENT_ID_PROP_NAME);
			if (clientId == null || clientId.trim().length() == 0) {
				clientId = m_systemService.getPrimaryMacAddress();
			}
			ValidationUtil.notEmptyOrNull(clientId, "clientId");

			// replace invalid token in the client ID as it is used as part of
			// the topicname space
			clientId = clientId.replace('/', '-');
			clientId = clientId.replace('+', '-');
			clientId = clientId.replace('#', '-');
			
			secureBrokerUrl();

			// Configure the broker URL
			brokerUrl = (String) properties.get(MQTT_BROKER_URL_PROP_NAME);
			ValidationUtil.notEmptyOrNull(brokerUrl, MQTT_BROKER_URL_PROP_NAME);

			brokerUrl = brokerUrl.trim();
			
			brokerUrl = brokerUrl.replaceAll("^" + MQTT_SCHEME, "tcp://");
			brokerUrl = brokerUrl.replaceAll("^" + MQTTS_SCHEME, "ssl://"); 
			//brokerUrl = brokerUrl.replaceAll("^" + MQTT_SCHEME, "tcp://");
			//brokerUrl = brokerUrl.replaceAll("^" + MQTTS_SCHEME, "ssl://");
			brokerUrl = brokerUrl.replaceAll("/$", "");
			ValidationUtil.notEmptyOrNull(brokerUrl, "brokerUrl");

			ValidationUtil.notEmptyOrNull((String) properties.get(MQTT_USERNAME_PROP_NAME), MQTT_USERNAME_PROP_NAME);
			ValidationUtil.notEmptyOrNull(new String((char[]) properties.get(MQTT_PASSWORD_PROP_NAME)), MQTT_PASSWORD_PROP_NAME);
			ValidationUtil.notNegative((Integer) properties.get(MQTT_KEEP_ALIVE_PROP_NAME), MQTT_KEEP_ALIVE_PROP_NAME);
			ValidationUtil.notNegative((Integer) properties.get(MQTT_TIMEOUT_PROP_NAME), MQTT_TIMEOUT_PROP_NAME);

			ValidationUtil.notNull((Boolean) properties.get(MQTT_CLEAN_SESSION_PROP_NAME), MQTT_CLEAN_SESSION_PROP_NAME);

			conOpt.setUserName((String) properties.get(MQTT_USERNAME_PROP_NAME));
			conOpt.setPassword((char[]) properties.get(MQTT_PASSWORD_PROP_NAME));
			conOpt.setKeepAliveInterval((Integer) properties.get(MQTT_KEEP_ALIVE_PROP_NAME));
			conOpt.setConnectionTimeout((Integer) properties.get(MQTT_TIMEOUT_PROP_NAME));

			conOpt.setCleanSession((Boolean) properties.get(MQTT_CLEAN_SESSION_PROP_NAME));

			conOpt.setMqttVersion((Integer) properties.get(MQTT_DEFAULT_VERSION_PROP_NAME));

			synchronized (m_topicContext) {
				m_topicContext.clear();
				if (properties.get(CLOUD_ACCOUNT_NAME_PROP_NAME) != null) {
					m_topicContext.put(TOPIC_ACCOUNT_NAME_CTX_NAME, (String) properties.get(CLOUD_ACCOUNT_NAME_PROP_NAME));
				}
				m_topicContext.put(TOPIC_DEVICE_ID_CTX_NAME, clientId);
			}

			String willTopic = (String) properties.get(MQTT_LWT_TOPIC_PROP_NAME);
			if (!(willTopic == null || willTopic.isEmpty())) {
				int willQos = 0;
				boolean willRetain = false;

				String willPayload = (String) properties.get(MQTT_LWT_PAYLOAD_PROP_NAME);
				if (properties.get(MQTT_LWT_QOS_PROP_NAME) != null) {
					willQos = (Integer) properties.get(MQTT_LWT_QOS_PROP_NAME);
				}
				if (properties.get(MQTT_LWT_RETAIN_PROP_NAME) != null) {
					willRetain = (Boolean) properties.get(MQTT_LWT_RETAIN_PROP_NAME);
				}

				willTopic = replaceTopicVariables(willTopic);

				byte[] payload = {};
				if (willPayload != null && !willPayload.isEmpty()) {
					try {
						payload = willPayload.getBytes("UTF-8");
					} catch (UnsupportedEncodingException e) {
						s_logger.error("Unsupported encoding", e);
					}
				}

				conOpt.setWill(willTopic, payload, willQos, willRetain);
			}
		} catch (KuraException e) {
			s_logger.error("Invalid configuration");
			throw new IllegalStateException("Invalid MQTT client configuration", e);
		}

		//
		// SSL
		if (brokerUrl.startsWith("ssl")) {
			try {
				String alias = m_topicContext.get(TOPIC_ACCOUNT_NAME_CTX_NAME);
				SSLSocketFactory ssf = m_sslManagerService.getSSLSocketFactory(alias);
				conOpt.setSocketFactory(ssf);
			} catch (Exception e) {
				s_logger.error("SSL setup failed", e);
				throw new IllegalStateException("SSL setup failed", e);
			}
		}

		String sType = (String) properties.get(PERSISTENCE_TYPE_PROP_NAME);
		PersistenceType persistenceType = null;
		if (sType.equals("file")) {
			persistenceType = PersistenceType.FILE;
		} else if (sType.equals("memory")) {
			persistenceType = PersistenceType.MEMORY;
		} else {
			throw new IllegalStateException("Invalid MQTT client configuration: persistenceType: " + persistenceType);
		}

		clientConfiguration = new MqttClientConfiguration(brokerUrl, clientId, persistenceType, conOpt);

		return clientConfiguration;
	}

	private boolean isSecuredEnvironment() {
		boolean result =    ENV_JAVA_SECURITY != null 
				&& ENV_OSGI_FRAMEWORK_SECURITY != null 
				&& ENV_OSGI_SIGNED_CONTENT_SUPPORT != null 
				&& ENV_OSGI_FRAMEWORK_TRUST_REPOSITORIES != null;
		return result;
	}

	private void secureBrokerUrl() {
		try{
			String brokerUrl = (String) m_properties.get(MQTT_BROKER_URL_PROP_NAME);
			ValidationUtil.notEmptyOrNull(brokerUrl, MQTT_BROKER_URL_PROP_NAME);

			brokerUrl = brokerUrl.trim();
			if( isSecuredEnvironment() && brokerUrl.contains(MQTT_SCHEME)){
				brokerUrl = brokerUrl.replaceAll("^" + MQTT_SCHEME, MQTTS_SCHEME);
				brokerUrl = brokerUrl.replaceAll(":1883", ":8883");
				m_properties.put(MQTT_BROKER_URL_PROP_NAME, brokerUrl);
				String searchedPID = (String) m_properties.get(APP_PID);
				m_configurationService.updateConfiguration(searchedPID, m_properties);
			}
			
		} catch (KuraException e) {
			s_logger.error("Invalid configuration");
			throw new IllegalStateException("Invalid MQTT client configuration", e);
		}
	}

	private String replaceTopicVariables(String topic) {
		boolean found;
		Matcher topicMatcher = s_topicPattern.matcher(topic);
		StringBuffer sb = new StringBuffer();
		do {

			found = topicMatcher.find();
			if (found) {
				// By default replace #variable-name (group 0) with itself
				String replacement = topicMatcher.group(0);

				// TODO: Try to get variable-name (group 1) from the context
				String variableName = topicMatcher.group(1);
				synchronized (m_topicContext) {
					String value = m_topicContext.get(variableName);
					if (value != null) {
						replacement = value;
					}
				}

				// Replace #variable-name with the value of the variable
				topicMatcher.appendReplacement(sb, replacement);
			}
		} while (found);

		topicMatcher.appendTail(sb);

		String replacedTopic = sb.toString();

		s_logger.debug("Replaced tokens in topic {} with: {}", topic, replacedTopic);

		return replacedTopic;
	}

	private String generateSessionId() {
		return m_clientConf.getClientId() + "-" + m_clientConf.getBrokerUrl();
	}

	private void setupMqttSession() {

		if (m_clientConf == null) {
			throw new IllegalStateException("Invalid client configuration");
		}

		// We need to construct a new client instance only if either the broker URL
		// or the client ID changes.
		// We also need to construct a new instance if the persistence type (file or memory) changes.
		// We MUST avoid to construct a new client instance every time because
		// in that case the MQTT message ID is reset to 1.
		if (m_mqttClient != null) {
			String brokerUrl = m_mqttClient.getServerURI();
			String clientId = m_mqttClient.getClientId();

			if (!(brokerUrl.equals(m_clientConf.getBrokerUrl()) && clientId.equals(m_clientConf.getClientId()) && m_persistenceType == m_clientConf
					.getPersistenceType())) {
				try {
					s_logger.info("Closing client...");
					// prevent callbacks from a zombie client
					m_mqttClient.setCallback(null);
					m_mqttClient.close();
					s_logger.info("Closed");
				} catch (MqttException e) {
					s_logger.error("Cannot close client", e);
				} finally {
					m_mqttClient = null;
				}
			}
		}

		// Connecting with Clean Session flag set to true always starts
		// a new session.
		boolean newSession = m_clientConf.getConnectOptions().isCleanSession();

		if (m_mqttClient == null) {

			s_logger.info("Creating a new client instance");

			//
			// Initialize persistence. This is only useful if the client
			// connects with
			// Clean Session flag set to false.
			//
			// Note that when using file peristence,
			// Paho creates a subdirectory persistence whose name is encoded
			// like this:
			// cristiano-tcpbroker-stageeveryware-cloudcom1883/
			// So the persistence is per client ID (cristiano) and broker URL.
			// If we are connecting to a different broker URL or with a
			// different client ID,
			// Paho will create a new subdirectory for this MQTT connection.
			// Closing the old client instance also deletes the associated
			// persistence subdirectory.
			//
			// The lesson is:
			// Reconnecting to the same broker URL with the same client ID will
			// leverage
			// Paho persistence and the MQTT message ID is always increased (up
			// to the its maximum).
			//
			// Connecting either to a different broker URL or with a different
			// client ID discards persisted
			// messages and the MQTT client ID is reset.
			//
			// We have a problem here where the DataService needs to track
			// in-flight messages, possibly
			// across different MQTT connections.
			// These messages will never be confirmed on a different connection.
			// While we can assume that the client ID never changes because it's
			// typically auto-generated,
			// we cannot safely assume that the broker URL never changes.
			//
			// The above leads to two problems:
			// The MQTT message ID alone is not sufficient to track an in-flight
			// message
			// because it can be reset on a different connection.
			//
			// On a different connection the DataService should republish the
			// in-flight messages because
			// Paho won't do that.

			PersistenceType persistenceType = m_clientConf.getPersistenceType();
			if (persistenceType == PersistenceType.MEMORY) {
				s_logger.info("Using memory persistence for in-flight messages");
				m_persistence = new MemoryPersistence();
			} else {
				StringBuffer sb = new StringBuffer();
				sb.append(m_systemService.getKuraDataDirectory()).append(m_systemService.getFileSeparator()).append("paho-persistence");

				String dir = sb.toString();

				s_logger.info("Using file persistence for in-flight messages: {}", dir);

				// Look for "Close on CONNACK timeout" FIXME in this file.
				// Make sure persistence is closed.
				// This is needed if the previous connect attempt was
				// forcibly terminated by closing the client.
				if (m_persistence != null) {
					try {
						m_persistence.close();
					} catch (MqttPersistenceException e) {
						s_logger.info("Failed to close persistence. Ignoring exception " + e.getMessage());
						s_logger.debug("Failed to close persistence. Ignoring exception.", e);
					}
				}
				m_persistence = new MqttDefaultFilePersistence(dir);
			}

			//
			// Construct the MqttClient instance
			MqttAsyncClient mqttClient = null;
			try {
				mqttClient = new MqttAsyncClient(m_clientConf.getBrokerUrl(), m_clientConf.getClientId(), m_persistence);
			} catch (MqttException e) {
				s_logger.error("Client instantiation failed", e);
				throw new IllegalStateException("Client instantiation failed", e);
			}

			mqttClient.setCallback(this);

			m_persistenceType = persistenceType;
			m_mqttClient = mqttClient;

			if (!m_clientConf.getConnectOptions().isCleanSession()) {
				// This is tricky.
				// The purpose of this code is to try to restore pending delivery tokens
				// from the MQTT client persistence and determine if the next connection
				// can be considered continuing an existing session.
				// This is needed to allow the upper layer deciding what to do with the
				// in-flight messages it is tracking (if any).
				// If pending delivery tokens are found we assume that the upper layer
				// is tracking them. In this case we set the newSession flag to false
				// and notify this in the onConnectionEstablished callback.
				// The upper layer shouldn't do anything special.
				//
				// Otherwise the next upper layer should decide what to do with the
				// in-flight messages it is tracking (if any), either to republish or
				// drop them.
				IMqttDeliveryToken[] pendingDeliveryTokens = m_mqttClient.getPendingDeliveryTokens();
				if (pendingDeliveryTokens != null && pendingDeliveryTokens.length != 0) {
					newSession = false;
				}
			}
		}

		m_newSession = newSession;
		m_sessionId = generateSessionId();
	}

	private String getMqttVersionLabel(int MqttVersion) {

		switch (MqttVersion) {
		case MqttConnectOptions.MQTT_VERSION_3_1:
			return "3.1";
		case MqttConnectOptions.MQTT_VERSION_3_1_1:
			return "3.1.1";
		default:
			return String.valueOf(MqttVersion);
		}
	}

}
