/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.data.transport.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.data.transport.mqtt.MqttClientConfiguration.PersistenceType;
import org.eclipse.kura.core.ssl.SslManagerServiceOptions;
import org.eclipse.kura.core.util.ValidationUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.ssl.SslServiceListener;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttDataTransport implements DataTransportService, MqttCallback, ConfigurableComponent, SslServiceListener,
        CloudConnectionStatusComponent {

    private static final String NOT_CONNECTED_MESSAGE = "Not connected";

    private static final String ALREADY_CONNECTED_MESSAGE = "Already connected";

    private static final String INVALID_CONFIGURATION_MESSAGE = "Invalid configuration";

    private static final Logger logger = LoggerFactory.getLogger(MqttDataTransport.class);

    private static final String MQTT_SCHEME = "mqtt://";
    private static final String MQTTS_SCHEME = "mqtts://";
    // TODO: add mqtt+ssl for secure mqtt

    // '#' followed by one or more non-whitespace but not the '/'
    private static final String TOPIC_PATTERN_STRING = "#([^\\s/]+)";

    private static final Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);

    private static final String MQTT_BROKER_URL_PROP_NAME = "broker-url";
    private static final String MQTT_USERNAME_PROP_NAME = "username";
    private static final String MQTT_PASSWORD_PROP_NAME = "password";
    private static final String MQTT_CLIENT_ID_PROP_NAME = "client-id";
    private static final String MQTT_KEEP_ALIVE_PROP_NAME = "keep-alive";
    private static final String MQTT_CLEAN_SESSION_PROP_NAME = "clean-session";

    // All timeouts
    private static final String MQTT_TIMEOUT_PROP_NAME = "timeout";

    private static final String MQTT_DEFAULT_VERSION_PROP_NAME = "protocol-version";

    private static final String MQTT_LWT_QOS_PROP_NAME = "lwt.qos";
    private static final String MQTT_LWT_RETAIN_PROP_NAME = "lwt.retain";
    private static final String MQTT_LWT_TOPIC_PROP_NAME = "lwt.topic";
    private static final String MQTT_LWT_PAYLOAD_PROP_NAME = "lwt.payload";

    private static final String CLOUD_ACCOUNT_NAME_PROP_NAME = "topic.context.account-name";

    private static final String PERSISTENCE_TYPE_PROP_NAME = "in-flight.persistence";

    private static final String TOPIC_ACCOUNT_NAME_CTX_NAME = "account-name";
    private static final String TOPIC_DEVICE_ID_CTX_NAME = "client-id";

    private static final String SSL_PROTOCOL = SslManagerServiceOptions.PROP_PROTOCOL;
    private static final String SSL_CIPHERS = SslManagerServiceOptions.PROP_CIPHERS;
    private static final String SSL_HN_VERIFY = SslManagerServiceOptions.PROP_HN_VERIFY;
    private static final String SSL_CERT_ALIAS = "ssl.certificate.alias";
    private static final String SSL_DEFAULT_HN_VERIFY = "use-ssl-service-config";

    private SystemService systemService;
    private SslManagerService sslManagerService;
    private CloudConnectionStatusService cloudConnectionStatusService;

    private CloudConnectionStatusEnum notificationStatus = CloudConnectionStatusEnum.OFF;

    private MqttAsyncClient mqttClient;

    private DataTransportListenerS dataTransportListeners;

    private MqttClientConfiguration clientConf;
    private boolean newSession;
    private String sessionId;

    private PersistenceType persistenceType;
    private MqttClientPersistence persistence;

    private final Map<String, String> topicContext = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();

    private CryptoService cryptoService;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    public void unsetSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = null;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    public void setCloudConnectionStatusService(CloudConnectionStatusService cloudConnectionStatusService) {
        this.cloudConnectionStatusService = cloudConnectionStatusService;
    }

    public void unsetCloudConnectionStatusService(CloudConnectionStatusService cloudConnectionStatusService) {
        this.cloudConnectionStatusService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        // We need to catch the configuration exception and activate anyway.
        // Otherwise the ConfigurationService will not be able to track us.
        HashMap<String, Object> decryptedPropertiesMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals(MQTT_PASSWORD_PROP_NAME)) {
                try {
                    Password decryptedPassword = new Password(
                            this.cryptoService.decryptAes(((String) value).toCharArray()));
                    decryptedPropertiesMap.put(key, decryptedPassword);
                } catch (Exception e) {
                    logger.info("Password is not encrypted");
                    decryptedPropertiesMap.put(key, new Password((String) value));
                }
            } else {
                decryptedPropertiesMap.put(key, value);
            }
        }

        this.properties.putAll(decryptedPropertiesMap);
        try {
            this.clientConf = buildConfiguration(this.properties);
        } catch (RuntimeException e) {
            logger.error(
                    "Invalid client configuration. Service will not be able to connect until the configuration is updated",
                    e);
        }

        this.dataTransportListeners = new DataTransportListenerS(componentContext);

        // Do nothing waiting for the connect request from the upper layer.
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating {}...", this.properties.get(ConfigurationService.KURA_SERVICE_PID));

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
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        this.properties.clear();

        HashMap<String, Object> decryptedPropertiesMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals(MQTT_PASSWORD_PROP_NAME)) {
                try {
                    Password decryptedPassword = new Password(
                            this.cryptoService.decryptAes(((String) value).toCharArray()));
                    decryptedPropertiesMap.put(key, decryptedPassword);
                } catch (Exception e) {
                    logger.info("Password is not encrypted");
                    decryptedPropertiesMap.put(key, new Password((String) value));
                }
            } else {
                decryptedPropertiesMap.put(key, value);
            }
        }

        this.properties.putAll(decryptedPropertiesMap);

        update();
    }

    private void update() {
        boolean wasConnected = isConnected();

        // First notify the Listeners
        // We do nothing other than notifying the listeners which may later
        // request to disconnect and reconnect again.
        this.dataTransportListeners.onConfigurationUpdating(wasConnected);

        // Then update the configuration
        // Throwing a RuntimeException here is fine.
        // Listeners will not be notified of an invalid configuration update.
        logger.info("Building new configuration...");
        this.clientConf = buildConfiguration(this.properties);

        // We do nothing other than notifying the listeners which may later
        // request to disconnect and reconnect again.
        this.dataTransportListeners.onConfigurationUpdated(wasConnected);
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public synchronized void connect() throws KuraConnectException {
        // We treat this as an application bug.
        if (isConnected()) {
            logger.error(ALREADY_CONNECTED_MESSAGE);
            throw new IllegalStateException(ALREADY_CONNECTED_MESSAGE);
        }

        // Attempt to setup the MQTT session
        setupMqttSession();

        if (this.mqttClient == null) {
            logger.error(INVALID_CONFIGURATION_MESSAGE);
            throw new IllegalStateException(INVALID_CONFIGURATION_MESSAGE);
        }

        logger.info("# ------------------------------------------------------------");
        logger.info("#  Connection Properties");
        logger.info("#  broker    = {}", this.clientConf.getBrokerUrl());
        logger.info("#  clientId  = {}", this.clientConf.getClientId());
        logger.info("#  username  = {}", this.clientConf.getConnectOptions().getUserName());
        logger.info("#  password  = XXXXXXXXXXXXXX");
        logger.info("#  keepAlive = {}", this.clientConf.getConnectOptions().getKeepAliveInterval());
        logger.info("#  timeout   = {}", this.clientConf.getConnectOptions().getConnectionTimeout());
        logger.info("#  cleanSession    = {}", this.clientConf.getConnectOptions().isCleanSession());
        logger.info("#  MQTT version    = {}",
                getMqttVersionLabel(this.clientConf.getConnectOptions().getMqttVersion()));
        logger.info("#  willDestination = {}", this.clientConf.getConnectOptions().getWillDestination());
        logger.info("#  willMessage     = {}", this.clientConf.getConnectOptions().getWillMessage());
        logger.info("#");
        logger.info("#  Connecting...");

        // Register the component in the CloudConnectionStatus service
        this.cloudConnectionStatusService.register(this);
        // Update status notification service
        this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.FAST_BLINKING);

        //
        // connect
        try {
            IMqttToken connectToken = this.mqttClient.connect(this.clientConf.getConnectOptions());
            connectToken.waitForCompletion(getTimeToWaitMillis() * 3);
            logger.info("#  Connected!");
            logger.info("# ------------------------------------------------------------");

            // Update status notification service
            this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.ON);

        } catch (MqttException e) {
            logger.warn("xxxxx  Connect failed. Forcing disconnect. xxxxx");
            closeMqttClient();

            // Update status notification service
            this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.OFF);

            throw new KuraConnectException(e, "Cannot connect");
        } finally {
            // Always unregister from CloudConnectionStatus service so to switch to the previous state
            this.cloudConnectionStatusService.unregister(this);
        }

        // notify the listeners
        this.dataTransportListeners.onConnectionEstablished(this.newSession);
    }

    @Override
    public boolean isConnected() {
        if (this.mqttClient != null) {
            return this.mqttClient.isConnected();
        }
        return false;
    }

    @Override
    public String getBrokerUrl() {
        if (this.clientConf != null) {
            String brokerUrl = this.clientConf.getBrokerUrl();
            if (brokerUrl != null) {
                return brokerUrl;
            }
        }
        return "";
    }

    @Override
    public String getAccountName() {
        if (this.clientConf != null) {
            String accountName = this.topicContext.get(TOPIC_ACCOUNT_NAME_CTX_NAME);
            if (accountName != null) {
                return accountName;
            }
        }
        return "";
    }

    @Override
    public String getUsername() {
        if (this.clientConf != null) {
            String username = this.clientConf.getConnectOptions().getUserName();
            if (username != null) {
                return username;
            }
        }
        return "";
    }

    @Override
    public String getClientId() {
        if (this.clientConf != null) {
            String clientId = this.clientConf.getClientId();
            if (clientId != null) {
                return clientId;
            }
        }
        return "";
    }

    // TODO: java.lang.reflect.Proxy for every listener in order to catch
    // runtime exceptions thrown by listener implementor and log them.

    @Override
    public synchronized void disconnect(long quiesceTimeout) {
        // Disconnect the client if it's connected. If it fails log the
        // exception.
        // Don't throw an exception because the caller would not
        // be able to handle it.
        if (isConnected()) {
            logger.info("Disconnecting...");

            //
            // notify the listeners
            this.dataTransportListeners.onDisconnecting();

            try {
                this.mqttClient.disconnect(quiesceTimeout).waitForCompletion(getTimeToWaitMillis());
                logger.info("Disconnected");
            } catch (MqttException e) {
                logger.error("Disconnect failed", e);
            }

            //
            // notify the listeners
            this.dataTransportListeners.onDisconnected();
        } else {
            logger.warn("MQTT client already disconnected");
        }
    }

    // ---------------------------------------------------------
    //
    // Subscription Management Methods
    //
    // ---------------------------------------------------------

    @Override
    public void subscribe(String topic, int qos) throws KuraException {

        if (this.mqttClient == null || !this.mqttClient.isConnected()) {
            throw new KuraNotConnectedException(NOT_CONNECTED_MESSAGE);
        }

        topic = replaceTopicVariables(topic);

        logger.info("Subscribing to topic: {} with QoS: {}", topic, qos);

        try {
            IMqttToken token = this.mqttClient.subscribe(topic, qos);
            token.waitForCompletion(getTimeToWaitMillis());
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_TIMEOUT) {
                logger.warn("Timeout subscribing to topic: {}", topic);
                throw new KuraTimeoutException("Timeout subscribing to topic: " + topic, e);
            } else {
                logger.error("Cannot subscribe to topic: " + topic, e);
                throw KuraException.internalError(e, "Cannot subscribe to topic: " + topic);
            }
        }
    }

    @Override
    public void unsubscribe(String topic) throws KuraException {

        if (this.mqttClient == null || !this.mqttClient.isConnected()) {
            throw new KuraNotConnectedException(NOT_CONNECTED_MESSAGE);
        }

        topic = replaceTopicVariables(topic);

        logger.info("Unsubscribing to topic: {}", topic);

        try {
            IMqttToken token = this.mqttClient.unsubscribe(topic);
            token.waitForCompletion(getTimeToWaitMillis());
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_TIMEOUT) {
                logger.warn("Timeout unsubscribing to topic: {}", topic);
                throw new KuraTimeoutException("Timeout unsubscribing to topic: " + topic, e);
            } else {
                logger.error("Cannot unsubscribe to topic: " + topic, e);
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
    public DataTransportToken publish(String topic, byte[] payload, int qos, boolean retain) throws KuraException {

        if (this.mqttClient == null || !this.mqttClient.isConnected()) {
            throw new KuraNotConnectedException(NOT_CONNECTED_MESSAGE);
        }

        topic = replaceTopicVariables(topic);

        logger.info("Publishing message on topic: {} with QoS: {}", topic, qos);

        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        message.setQos(qos);
        message.setRetained(retain);

        Integer messageId = null;
        try {
            IMqttDeliveryToken token = this.mqttClient.publish(topic, message);
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
            logger.debug("Published message with ID: {}", token.getMessageId());
            if (qos > 0) {
                messageId = Integer.valueOf(token.getMessageId());
            }
        } catch (MqttPersistenceException e) {
            // This is probably an unrecoverable internal error
            logger.error("Cannot publish on topic: {}", topic, e);
            throw new IllegalStateException("Cannot publish on topic: " + topic);
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_MAX_INFLIGHT) {
                logger.info("Too many inflight messages");
                throw new KuraTooManyInflightMessagesException(e, "Too many in-fligh messages");
            } else {
                logger.error("Cannot publish on topic: " + topic, e);
                throw KuraException.internalError(e, "Cannot publish on topic: " + topic);
            }
        }

        DataTransportToken token = null;
        if (messageId != null) {
            token = new DataTransportToken(messageId, this.sessionId);
        }

        return token;
    }

    @Override
    public void addDataTransportListener(DataTransportListener listener) {
        this.dataTransportListeners.add(listener);
    }

    @Override
    public void removeDataTransportListener(DataTransportListener listener) {
        this.dataTransportListeners.remove(listener);
    }

    // ---------------------------------------------------------
    //
    // MqttCallback methods
    //
    // ---------------------------------------------------------
    @Override
    public void connectionLost(final Throwable cause) {
        logger.warn("Connection Lost", cause);

        // notify the listeners
        this.dataTransportListeners.onConnectionLost(cause);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

        if (token == null) {
            logger.error("null token");
            return;
        }

        // Weird, tokens related to messages published with QoS > 0 have a null
        // nested message

        MqttMessage msg = null;
        try {
            msg = token.getMessage();
        } catch (MqttException e) {
            logger.error("Cannot get message", e);
            return;
        }

        if (msg != null) {
            // Note that Paho call this also for messages published with QoS ==
            // 0.
            // We don't want to rely on that and we drop asynchronous confirms
            // for QoS == 0.
            int qos = msg.getQos();

            if (qos == 0) {
                logger.debug("Ignoring deliveryComplete for messages published with QoS == 0");
                return;
            }
        }

        int id = token.getMessageId();

        logger.debug("Delivery complete for message with ID: {}", id);

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
        DataTransportToken dataPublisherToken = new DataTransportToken(id, this.sessionId);
        this.dataTransportListeners.onMessageConfirmed(dataPublisherToken);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        logger.debug("Message arrived on topic: {}", topic);

        // FIXME: we should be more selective here and only call the listeners
        // actually subscribed to this topic.
        // Anyway we don't have such a mapping so the listeners are responsible
        // to filter messages.

        // FIXME: the same argument about lost confirms applies to arrived
        // messages.

        // notify the listeners
        this.dataTransportListeners.onMessageArrived(topic, message.getPayload(), message.getQos(),
                message.isRetained());
    }

    private long getTimeToWaitMillis() {
        // We use the same value for every timeout
        return this.clientConf.getConnectOptions().getConnectionTimeout() * 1000L;
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

    // ---------------------------------------------------------
    //
    // CloudConnectionStatus Overrides
    //
    // ---------------------------------------------------------
    @Override
    public int getNotificationPriority() {
        return CloudConnectionStatusService.PRIORITY_MEDIUM;
    }

    @Override
    public CloudConnectionStatusEnum getNotificationStatus() {
        return this.notificationStatus;
    }

    @Override
    public void setNotificationStatus(CloudConnectionStatusEnum status) {
        this.notificationStatus = status;
    }

    // ---------------------------------------------------------
    //
    // Private methods
    //
    // ---------------------------------------------------------

    /*
     * This method builds an internal configuration option needed by the client
     * to connect. The configuration is assembled from various sources:
     * component configuration, SystemService, NetworkService, etc. The returned
     * configuration is valid so no further validation is needed. If a valid
     * configuration cannot be assembled the method throws a RuntimeException
     * (assuming that this error is unrecoverable).
     */
    private MqttClientConfiguration buildConfiguration(Map<String, Object> properties) {

        MqttClientConfiguration clientConfiguration;
        MqttConnectOptions conOpt = new MqttConnectOptions();
        String clientId = null;
        String brokerUrl = null;
        try {
            // Configure the client ID
            clientId = (String) properties.get(MQTT_CLIENT_ID_PROP_NAME);
            if (clientId == null || clientId.trim().length() == 0) {
                clientId = this.systemService.getPrimaryMacAddress();
            }
            ValidationUtil.notEmptyOrNull(clientId, "clientId");

            // replace invalid token in the client ID as it is used as part of
            // the topicname space
            clientId = clientId.replace('/', '-');
            clientId = clientId.replace('+', '-');
            clientId = clientId.replace('#', '-');
            clientId = clientId.replace('.', '-');

            // Configure the broker URL
            brokerUrl = (String) properties.get(MQTT_BROKER_URL_PROP_NAME);
            ValidationUtil.notEmptyOrNull(brokerUrl, MQTT_BROKER_URL_PROP_NAME);
            brokerUrl = brokerUrl.trim();

            brokerUrl = brokerUrl.replaceAll("^" + MQTT_SCHEME, "tcp://");
            brokerUrl = brokerUrl.replaceAll("^" + MQTTS_SCHEME, "ssl://");

            brokerUrl = brokerUrl.replaceAll("/$", "");
            ValidationUtil.notEmptyOrNull(brokerUrl, "brokerUrl");

            ValidationUtil.notNegative((Integer) properties.get(MQTT_KEEP_ALIVE_PROP_NAME), MQTT_KEEP_ALIVE_PROP_NAME);
            ValidationUtil.notNegative((Integer) properties.get(MQTT_TIMEOUT_PROP_NAME), MQTT_TIMEOUT_PROP_NAME);

            ValidationUtil.notNull(properties.get(MQTT_CLEAN_SESSION_PROP_NAME), MQTT_CLEAN_SESSION_PROP_NAME);

            String userName = (String) properties.get(MQTT_USERNAME_PROP_NAME);
            if (userName != null && !userName.isEmpty()) {
                conOpt.setUserName(userName);
            }

            Password password = (Password) properties.get(MQTT_PASSWORD_PROP_NAME);
            if (password != null && password.toString().length() != 0) {
                conOpt.setPassword(password.getPassword());
            }

            conOpt.setKeepAliveInterval((Integer) properties.get(MQTT_KEEP_ALIVE_PROP_NAME));
            conOpt.setConnectionTimeout((Integer) properties.get(MQTT_TIMEOUT_PROP_NAME));

            conOpt.setCleanSession((Boolean) properties.get(MQTT_CLEAN_SESSION_PROP_NAME));

            conOpt.setMqttVersion((Integer) properties.get(MQTT_DEFAULT_VERSION_PROP_NAME));

            synchronized (this.topicContext) {
                this.topicContext.clear();
                if (properties.get(CLOUD_ACCOUNT_NAME_PROP_NAME) != null) {
                    this.topicContext.put(TOPIC_ACCOUNT_NAME_CTX_NAME,
                            (String) properties.get(CLOUD_ACCOUNT_NAME_PROP_NAME));
                }
                this.topicContext.put(TOPIC_DEVICE_ID_CTX_NAME, clientId);
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
                    payload = willPayload.getBytes(StandardCharsets.UTF_8);
                }

                conOpt.setWill(willTopic, payload, willQos, willRetain);
            }
        } catch (KuraException e) {
            logger.error(INVALID_CONFIGURATION_MESSAGE);
            throw new IllegalStateException("Invalid MQTT client configuration", e);
        }

        //
        // SSL
        if (brokerUrl.startsWith("ssl") || brokerUrl.startsWith("wss")) {
            try {
                String alias = (String) this.properties.get(SSL_CERT_ALIAS);
                if (alias == null || "".equals(alias.trim())) {
                    alias = this.topicContext.get(TOPIC_ACCOUNT_NAME_CTX_NAME);
                }

                String protocol = (String) this.properties.get(SSL_PROTOCOL);
                String ciphers = (String) this.properties.get(SSL_CIPHERS);
                String hnVerification = (String) this.properties.get(SSL_HN_VERIFY);

                SSLSocketFactory ssf;
                if (SSL_DEFAULT_HN_VERIFY.equals(hnVerification)) {
                    ssf = this.sslManagerService.getSSLSocketFactory(protocol, ciphers, null, null, null, alias);
                } else {
                    ssf = this.sslManagerService.getSSLSocketFactory(protocol, ciphers, null, null, null, alias,
                            Boolean.valueOf(hnVerification));
                }

                conOpt.setSocketFactory(ssf);
            } catch (Exception e) {
                logger.error("SSL setup failed", e);
                throw new IllegalStateException("SSL setup failed");
            }
        }

        String sType = (String) properties.get(PERSISTENCE_TYPE_PROP_NAME);
        PersistenceType persistenceType = null;
        if ("file".equals(sType)) {
            persistenceType = PersistenceType.FILE;
        } else if ("memory".equals(sType)) {
            persistenceType = PersistenceType.MEMORY;
        } else {
            throw new IllegalStateException("Invalid MQTT client configuration: persistenceType: " + persistenceType);
        }

        clientConfiguration = new MqttClientConfiguration(brokerUrl, clientId, persistenceType, conOpt);

        return clientConfiguration;
    }

    private String replaceTopicVariables(String topic) {
        boolean found;
        Matcher topicMatcher = TOPIC_PATTERN.matcher(topic);
        StringBuffer sb = new StringBuffer();
        do {

            found = topicMatcher.find();
            if (found) {
                // By default replace #variable-name (group 0) with itself
                String replacement = topicMatcher.group(0);

                // TODO: Try to get variable-name (group 1) from the context
                String variableName = topicMatcher.group(1);
                synchronized (this.topicContext) {
                    String value = this.topicContext.get(variableName);
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

        logger.debug("Replaced tokens in topic {} with: {}", topic, replacedTopic);

        return replacedTopic;
    }

    private String generateSessionId() {
        return this.clientConf.getClientId() + "-" + this.clientConf.getBrokerUrl();
    }

    private void setupMqttSession() {

        if (this.clientConf == null) {
            throw new IllegalStateException("Invalid client configuration");
        }

        // We need to construct a new client instance only if either the broker URL
        // or the client ID changes.
        // We also need to construct a new instance if the persistence type (file or memory) changes.
        // We MUST avoid to construct a new client instance every time because
        // in that case the MQTT message ID is reset to 1.
        if (this.mqttClient != null) {
            String brokerUrl = this.mqttClient.getServerURI();
            String clientId = this.mqttClient.getClientId();

            if (!(brokerUrl.equals(this.clientConf.getBrokerUrl()) && clientId.equals(this.clientConf.getClientId())
                    && this.persistenceType == this.clientConf.getPersistenceType())) {
                closeMqttClient();
            }
        }

        // Connecting with Clean Session flag set to true always starts
        // a new session.
        boolean newSession = this.clientConf.getConnectOptions().isCleanSession();

        if (this.mqttClient == null) {

            logger.info("Creating a new client instance");

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

            PersistenceType persistenceType = this.clientConf.getPersistenceType();
            if (persistenceType == PersistenceType.MEMORY) {
                logger.info("Using memory persistence for in-flight messages");
                this.persistence = new MemoryPersistence();
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append(this.systemService.getKuraDataDirectory()).append(this.systemService.getFileSeparator())
                        .append("paho-persistence");

                String dir = sb.toString();

                logger.info("Using file persistence for in-flight messages: {}", dir);

                // Look for "Close on CONNACK timeout" FIXME in this file.
                // Make sure persistence is closed.
                // This is needed if the previous connect attempt was
                // forcibly terminated by closing the client.
                if (this.persistence != null) {
                    try {
                        this.persistence.close();
                    } catch (MqttPersistenceException e) {
                        logger.warn("Failed to close persistence. Ignoring exception.", e);
                    }
                }
                this.persistence = new MqttDefaultFilePersistence(dir);
            }

            //
            // Construct the MqttClient instance

            try {
                MqttAsyncClient newMqttClient = new MqttAsyncClient(this.clientConf.getBrokerUrl(),
                        this.clientConf.getClientId(), this.persistence);
                newMqttClient.setCallback(this);
                this.mqttClient = newMqttClient;
            } catch (MqttException e) {
                logger.error("Client instantiation failed", e);
                throw new IllegalStateException("Client instantiation failed");
            }

            this.persistenceType = persistenceType;

            if (!this.clientConf.getConnectOptions().isCleanSession()) {
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
                IMqttDeliveryToken[] pendingDeliveryTokens = this.mqttClient.getPendingDeliveryTokens();
                if (pendingDeliveryTokens != null && pendingDeliveryTokens.length != 0) {
                    newSession = false;
                }
            }
        }

        this.newSession = newSession;
        this.sessionId = generateSessionId();
    }

    private void closeMqttClient() {
        try {
            logger.info("Closing client...");
            // prevent callbacks from a zombie client
            this.mqttClient.setCallback(null);
            this.mqttClient.close();
            logger.info("Closed");
        } catch (MqttException e) {
            logger.warn("Cannot close client", e);
        } finally {
            this.mqttClient = null;
        }
    }

    private static String getMqttVersionLabel(int mqttVersion) {

        switch (mqttVersion) {
        case MqttConnectOptions.MQTT_VERSION_3_1:
            return "3.1";
        case MqttConnectOptions.MQTT_VERSION_3_1_1:
            return "3.1.1";
        default:
            return String.valueOf(mqttVersion);
        }
    }
}
