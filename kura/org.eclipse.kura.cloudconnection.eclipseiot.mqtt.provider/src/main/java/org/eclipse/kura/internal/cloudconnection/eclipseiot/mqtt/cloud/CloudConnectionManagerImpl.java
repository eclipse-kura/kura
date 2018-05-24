/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import static org.eclipse.kura.cloud.CloudPayloadEncoding.KURA_PROTOBUF;
import static org.eclipse.kura.cloud.CloudPayloadEncoding.SIMPLE_JSON;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.FULL_TOPIC;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.QOS;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.RETAIN;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloud.CloudPayloadEncoding;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.data.DataServiceImpl;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionManagerImpl implements DataServiceListener, ConfigurableComponent, EventHandler,
        CloudPayloadProtoBufEncoder, CloudPayloadProtoBufDecoder, CloudConnectionManager, CloudEndpoint {

    private static final String ERROR = "ERROR";

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionManagerImpl.class);

    private static final String CONNECTION_EVENT_PID_PROPERTY_KEY = "cloud.service.pid";

    private ComponentContext ctx;

    private CloudConnectionManagerOptions options;

    private DataService dataService;
    private SystemService systemService;
    private SystemAdminService systemAdminService;
    private NetworkService networkService;
    private PositionService positionService;
    private EventAdmin eventAdmin;

    // package visibility for LifeCyclePayloadBuilder
    String imei;
    String iccid;
    String imsi;
    String rssi;

    private boolean birthPublished;

    private final AtomicInteger messageId;

    private ServiceRegistration<?> cloudServiceRegistration;

    private final Set<CloudConnectionListener> registeredCloudConnectionListeners;
    private final Set<CloudPublisherDeliveryListener> registeredCloudPublisherDeliveryListeners;

    public CloudConnectionManagerImpl() {
        this.messageId = new AtomicInteger();
        this.registeredCloudConnectionListeners = new CopyOnWriteArraySet<>();
        this.registeredCloudPublisherDeliveryListeners = new CopyOnWriteArraySet<>();
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public void unsetDataService(DataService dataService) {
        this.dataService = null;
    }

    public DataService getDataService() {
        return this.dataService;
    }

    public void setSystemAdminService(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
    }

    public void unsetSystemAdminService(SystemAdminService systemAdminService) {
        this.systemAdminService = null;
    }

    public SystemAdminService getSystemAdminService() {
        return this.systemAdminService;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    public SystemService getSystemService() {
        return this.systemService;
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void unsetNetworkService(NetworkService networkService) {
        this.networkService = null;
    }

    public NetworkService getNetworkService() {
        return this.networkService;
    }

    public void setPositionService(PositionService positionService) {
        this.positionService = positionService;
    }

    public void unsetPositionService(PositionService positionService) {
        this.positionService = null;
    }

    public PositionService getPositionService() {
        return this.positionService;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activate {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        //
        // save the bundle context and the properties
        this.ctx = componentContext;
        this.options = new CloudConnectionManagerOptions(properties, this.systemService);
        //
        // install event listener for GPS locked event
        Dictionary<String, Object> props = new Hashtable<>();
        String[] eventTopics = { PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC,
                ModemReadyEvent.MODEM_EVENT_READY_TOPIC };
        props.put(EventConstants.EVENT_TOPIC, eventTopics);
        this.cloudServiceRegistration = this.ctx.getBundleContext().registerService(EventHandler.class.getName(), this,
                props);

        this.dataService.addDataServiceListener(this);

        //
        // Usually the cloud connection is setup in the
        // onConnectionEstablished callback.
        // Since the callback may be lost if we are activated
        // too late (the DataService is already connected) we
        // setup the cloud connection here.
        if (isConnected()) {
            logger.warn("DataService is already connected. Publish BIRTH certificate");
            try {
                setupCloudConnection();
            } catch (KuraException e) {
                logger.warn("Cannot setup cloud service connection", e);
            }
        }
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated {}...: {}", properties.get(ConfigurationService.KURA_SERVICE_PID), properties);

        // Update properties and re-publish Birth certificate
        this.options = new CloudConnectionManagerOptions(properties, this.systemService);
        if (isConnected()) {
            try {
                setupCloudConnection();
            } catch (KuraException e) {
                logger.warn("Cannot setup cloud service connection");
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivate {}...", componentContext.getProperties().get(ConfigurationService.KURA_SERVICE_PID));

        if (isConnected()) {
            try {
                publishDisconnectCertificate();
            } catch (KuraException e) {
                logger.warn("Cannot publish disconnect certificate");
            }
        }

        this.dataService.removeDataServiceListener(this);

        this.dataService = null;
        this.systemService = null;
        this.systemAdminService = null;
        this.networkService = null;
        this.positionService = null;
        this.eventAdmin = null;

        this.cloudServiceRegistration.unregister();
    }

    @Override
    public void handleEvent(Event event) {
        if (PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC.contains(event.getTopic())) {
            // if we get a position locked event,
            // republish the birth certificate only if we are configured to
            logger.info("Handling PositionLockedEvent");
            if (this.dataService.isConnected() && this.options.getRepubBirthCertOnGpsLock()) {
                try {
                    publishBirthCertificate();
                } catch (KuraException e) {
                    logger.warn("Cannot publish birth certificate", e);
                }
            }
        } else if (ModemReadyEvent.MODEM_EVENT_READY_TOPIC.contains(event.getTopic())) {
            logger.info("Handling ModemReadyEvent");
            ModemReadyEvent modemReadyEvent = (ModemReadyEvent) event;
            // keep these identifiers around until we can publish the certificate
            this.imei = (String) modemReadyEvent.getProperty(ModemReadyEvent.IMEI);
            this.imsi = (String) modemReadyEvent.getProperty(ModemReadyEvent.IMSI);
            this.iccid = (String) modemReadyEvent.getProperty(ModemReadyEvent.ICCID);
            this.rssi = (String) modemReadyEvent.getProperty(ModemReadyEvent.RSSI);
            logger.trace("handleEvent() :: IMEI={}", this.imei);
            logger.trace("handleEvent() :: IMSI={}", this.imsi);
            logger.trace("handleEvent() :: ICCID={}", this.iccid);
            logger.trace("handleEvent() :: RSSI={}", this.rssi);

            if (this.dataService.isConnected() && this.options.getRepubBirthCertOnModemDetection()
                    && !((this.imei == null || this.imei.length() == 0 || ERROR.equals(this.imei))
                            && (this.imsi == null || this.imsi.length() == 0 || ERROR.equals(this.imsi))
                            && (this.iccid == null || this.iccid.length() == 0 || ERROR.equals(this.iccid)))) {
                logger.debug("handleEvent() :: publishing BIRTH certificate ...");
                try {
                    publishBirthCertificate();
                } catch (KuraException e) {
                    logger.warn("Cannot publish birth certificate", e);
                }
            }

        }
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public boolean isConnected() {
        return this.dataService != null && this.dataService.isConnected();
    }

    // ----------------------------------------------------------------
    //
    // Package APIs
    //
    // ----------------------------------------------------------------

    public CloudConnectionManagerOptions getCloudServiceOptions() {
        return this.options;
    }

    public byte[] encodePayload(KuraPayload payload) throws KuraException {
        byte[] bytes;
        CloudPayloadEncoding preferencesEncoding = this.options.getPayloadEncoding();

        if (preferencesEncoding == KURA_PROTOBUF) {
            bytes = encodeProtobufPayload(payload);
        } else if (preferencesEncoding == SIMPLE_JSON) {
            bytes = encodeJsonPayload(payload);
        } else {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR);
        }
        return bytes;
    }

    // ----------------------------------------------------------------
    //
    // DataServiceListener API
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnectionEstablished() {
        try {
            setupCloudConnection();
        } catch (KuraException e) {
            logger.warn("Cannot setup cloud service connection");
        }

        postConnectionStateChangeEvent(true);

        this.registeredCloudConnectionListeners.forEach(CloudConnectionListener::onConnectionEstablished);
    }

    @Override
    public void onDisconnecting() {
        // publish disconnect certificate
        try {
            publishDisconnectCertificate();
        } catch (KuraException e) {
            logger.warn("Cannot publish disconnect certificate");
        }

        this.birthPublished = false;
    }

    @Override
    public void onDisconnected() {
        // raise event
        postConnectionStateChangeEvent(false);

        this.registeredCloudConnectionListeners.forEach(CloudConnectionListener::onDisconnected);
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        // raise event
        postConnectionStateChangeEvent(false);

        this.registeredCloudConnectionListeners.forEach(CloudConnectionListener::onConnectionLost);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        logger.info("Message arrived on topic: {}", topic);

    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        synchronized (this.messageId) {
            if (this.messageId.get() != -1 && this.messageId.get() == messageId) {
                if (this.options.getLifeCycleMessageQos() == 0) {
                    this.messageId.set(-1);
                }
                this.messageId.notifyAll();
            }
        }
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        synchronized (this.messageId) {
            if (this.messageId.get() != -1 && this.messageId.get() == messageId) {
                this.messageId.set(-1);
                this.messageId.notifyAll();
            }
        }

        this.registeredCloudPublisherDeliveryListeners
                .forEach(deliveryListener -> deliveryListener.onMessageConfirmed(String.valueOf(messageId), topic));
    }

    // ----------------------------------------------------------------
    //
    // CloudPayloadProtoBufEncoder API
    //
    // ----------------------------------------------------------------

    @Override
    public byte[] getBytes(KuraPayload kuraPayload, boolean gzipped) throws KuraException {
        CloudPayloadEncoder encoder = new CloudPayloadProtoBufEncoderImpl(kuraPayload);
        if (gzipped) {
            encoder = new CloudPayloadGZipEncoder(encoder);
        }

        byte[] bytes;
        try {
            bytes = encoder.getBytes();
            return bytes;
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, e);
        }
    }

    // ----------------------------------------------------------------
    //
    // CloudPayloadProtoBufDecoder API
    //
    // ----------------------------------------------------------------

    @Override
    public KuraPayload buildFromByteArray(byte[] payload) throws KuraException {
        CloudPayloadProtoBufDecoderImpl encoder = new CloudPayloadProtoBufDecoderImpl(payload);
        KuraPayload kuraPayload;

        try {
            kuraPayload = encoder.buildFromByteArray();
            return kuraPayload;
        } catch (KuraInvalidMessageException | IOException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, e);
        }
    }

    // ----------------------------------------------------------------
    //
    // Birth and Disconnect Certificates
    //
    // ----------------------------------------------------------------

    private void setupCloudConnection() throws KuraException {
        // publish birth certificate unless it has already been published
        // and republish is disabled
        boolean publishBirth = true;
        if (this.birthPublished && !this.options.getRepubBirthCertOnReconnect()) {
            publishBirth = false;
            logger.info("Birth certificate republish is disabled in configuration");
        }

        // publish birth certificate
        if (publishBirth) {
            publishBirthCertificate();
            this.birthPublished = true;
        }
    }

    private void publishBirthCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append("e").append(this.options.getTopicSeparator()).append(this.options.getTopicAccountToken())
                .append(this.options.getTopicSeparator()).append(this.options.getTopicClientIdToken())
                .append(this.options.getTopicSeparator()).append(this.options.getTopicBirthSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createBirthPayload();
        publishLifeCycleMessage(topic, payload);
    }

    private void publishDisconnectCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append("e").append(this.options.getTopicSeparator()).append(this.options.getTopicAccountToken())
                .append(this.options.getTopicSeparator()).append(this.options.getTopicClientIdToken())
                .append(this.options.getTopicSeparator()).append(this.options.getTopicDisconnectSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createDisconnectPayload();
        publishLifeCycleMessage(topic, payload);
    }

    private KuraPayload createBirthPayload() {
        LifeCyclePayloadBuilder payloadBuilder = new LifeCyclePayloadBuilder(this);
        return payloadBuilder.buildBirthPayload();
    }

    private KuraPayload createDisconnectPayload() {
        LifeCyclePayloadBuilder payloadBuilder = new LifeCyclePayloadBuilder(this);
        return payloadBuilder.buildDisconnectPayload();
    }

    private void publishLifeCycleMessage(String topic, KuraPayload payload) throws KuraException {
        // track the message ID and block until the message
        // has been published (i.e. written to the socket).
        synchronized (this.messageId) {
            this.messageId.set(-1);
            byte[] encodedPayload = encodePayload(payload);
            int localMessageId = this.dataService.publish(topic, encodedPayload, this.options.getLifeCycleMessageQos(),
                    this.options.getLifeCycleMessageRetain(), this.options.getLifeCycleMessagePriority());
            this.messageId.set(localMessageId);
            try {
                this.messageId.wait(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Interrupted while waiting for the message to be published", e);
            }
        }
    }

    private byte[] encodeProtobufPayload(KuraPayload payload) throws KuraException {
        byte[] bytes = new byte[0];
        if (payload == null) {
            return bytes;
        }

        CloudPayloadEncoder encoder = new CloudPayloadProtoBufEncoderImpl(payload);
        if (this.options.getEncodeGzip()) {
            encoder = new CloudPayloadGZipEncoder(encoder);
        }

        try {
            bytes = encoder.getBytes();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, e);
        }
        return bytes;
    }

    private byte[] encodeJsonPayload(KuraPayload payload) {
        return CloudPayloadJsonEncoder.getBytes(payload);
    }

    private void postConnectionStateChangeEvent(final boolean isConnected) {

        final Map<String, Object> eventProperties = Collections.singletonMap(CONNECTION_EVENT_PID_PROPERTY_KEY,
                (String) this.ctx.getProperties().get(ConfigurationService.KURA_SERVICE_PID));

        final Event event = isConnected ? new CloudConnectionEstablishedEvent(eventProperties)
                : new CloudConnectionLostEvent(eventProperties);
        this.eventAdmin.postEvent(event);
    }

    @Override
    public void connect() throws KuraConnectException {
        if (this.dataService != null) {
            this.dataService.connect();
        }
    }

    @Override
    public void disconnect() {
        if (this.dataService != null) {
            this.dataService.disconnect(10);
        }
    }

    @Override
    public Map<String, String> getInfo() {
        DataServiceImpl dataServiceImpl = (DataServiceImpl) this.dataService;
        return dataServiceImpl.getConnectionInfo();
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.registeredCloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.registeredCloudConnectionListeners.remove(cloudConnectionListener);
    }

    public void registerCloudPublisherDeliveryListener(CloudPublisherDeliveryListener cloudPublisherDeliveryListener) {
        this.registeredCloudPublisherDeliveryListeners.add(cloudPublisherDeliveryListener);
    }

    public void unregisterCloudPublisherDeliveryListener(
            CloudPublisherDeliveryListener cloudPublisherDeliveryListener) {
        this.registeredCloudPublisherDeliveryListeners.remove(cloudPublisherDeliveryListener);
    }

    @Override
    public String publish(KuraMessage message) throws KuraException {
        Map<String, Object> messageProps = message.getProperties();
        String fullTopic = (String) messageProps.get(FULL_TOPIC.name());
        int qos = (Integer) messageProps.get(QOS.name());
        boolean retain = (Boolean) messageProps.get(RETAIN.name());
        int priority = (Integer) messageProps.get(PRIORITY.name());

        byte[] appPayload = encodePayload(message.getPayload());

        int id = this.dataService.publish(fullTopic, appPayload, qos, retain, priority);

        if (qos == 0) {
            return null;
        }
        return String.valueOf(id);
    }

    @Override
    public void registerSubscriber(Map<String, Object> subscriptionProperties, CloudSubscriberListener subscriber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterSubscriber(Map<String, Object> subscriptionProperties) {
        throw new UnsupportedOperationException();
    }
}
