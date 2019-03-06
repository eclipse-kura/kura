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
package org.eclipse.kura.core.cloud;

import static java.util.Objects.isNull;
import static org.eclipse.kura.cloud.CloudPayloadEncoding.KURA_PROTOBUF;
import static org.eclipse.kura.cloud.CloudPayloadEncoding.SIMPLE_JSON;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.eclipse.kura.core.message.MessageConstants.APP_ID;
import static org.eclipse.kura.core.message.MessageConstants.APP_TOPIC;
import static org.eclipse.kura.core.message.MessageConstants.CONTROL;
import static org.eclipse.kura.core.message.MessageConstants.FULL_TOPIC;
import static org.eclipse.kura.core.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.core.message.MessageConstants.QOS;
import static org.eclipse.kura.core.message.MessageConstants.RETAIN;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloud.CloudPayloadEncoding;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.cloud.publisher.NotificationPublisherImpl;
import org.eclipse.kura.core.cloud.subscriber.CloudSubscriptionRecord;
import org.eclipse.kura.core.data.DataServiceImpl;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraApplicationTopic;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServiceImpl
        implements CloudService, DataServiceListener, ConfigurableComponent, EventHandler, CloudPayloadProtoBufEncoder,
        CloudPayloadProtoBufDecoder, RequestHandlerRegistry, CloudConnectionManager, CloudEndpoint {

    private static final String NOTIFICATION_PUBLISHER_PID = "org.eclipse.kura.cloud.publisher.CloudNotificationPublisher";

    private static final Logger logger = LoggerFactory.getLogger(CloudServiceImpl.class);

    private static final String TOPIC_BA_APP = "BA";
    private static final String TOPIC_MQTT_APP = "MQTT";

    private static final String CONNECTION_EVENT_PID_PROPERTY_KEY = "cloud.service.pid";

    private static final int NUM_CONCURRENT_CALLBACKS = 2;

    private static ExecutorService callbackExecutor = Executors.newFixedThreadPool(NUM_CONCURRENT_CALLBACKS);

    private ComponentContext ctx;

    private CloudServiceOptions options;

    private DataService dataService;
    private SystemService systemService;
    private SystemAdminService systemAdminService;
    private NetworkService networkService;
    private PositionService positionService;
    private EventAdmin eventAdmin;
    private CertificatesService certificatesService;
    private Unmarshaller jsonUnmarshaller;
    private Marshaller jsonMarshaller;

    // use a synchronized implementation for the list
    private final List<CloudClientImpl> cloudClients;
    private final Set<CloudConnectionListener> registeredCloudConnectionListeners;
    private final Set<CloudPublisherDeliveryListener> registeredCloudPublisherDeliveryListeners;
    private final Set<CloudDeliveryListener> registeredCloudDeliveryListeners;
    private final Map<CloudSubscriptionRecord, List<CloudSubscriberListener>> registeredSubscribers;

    // package visibility for LyfeCyclePayloadBuilder
    String imei;
    String iccid;
    String imsi;
    String rssi;

    private boolean subscribed;
    private boolean birthPublished;

    private final AtomicInteger messageId;

    private ServiceRegistration<?> cloudServiceRegistration;

    private final Map<String, RequestHandler> registeredRequestHandlers;

    private ServiceRegistration<?> notificationPublisherRegistration;
    private final CloudNotificationPublisher notificationPublisher;

    public CloudServiceImpl() {
        this.cloudClients = new CopyOnWriteArrayList<>();
        this.messageId = new AtomicInteger();
        this.registeredRequestHandlers = new HashMap<>();
        this.registeredSubscribers = new ConcurrentHashMap<>();
        this.registeredCloudConnectionListeners = new CopyOnWriteArraySet<>();
        this.registeredCloudPublisherDeliveryListeners = new CopyOnWriteArraySet<>();
        this.registeredCloudDeliveryListeners = new CopyOnWriteArraySet<>();
        this.notificationPublisher = new NotificationPublisherImpl(this);
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

    public void setJsonUnmarshaller(Unmarshaller jsonUnmarshaller) {
        this.jsonUnmarshaller = jsonUnmarshaller;
    }

    public void unsetJsonUnmarshaller(Unmarshaller jsonUnmarshaller) {
        this.jsonUnmarshaller = null;
    }

    public void setJsonMarshaller(Marshaller jsonMarshaller) {
        this.jsonMarshaller = jsonMarshaller;
    }

    public void unsetJsonMarshaller(Marshaller jsonMarshaller) {
        this.jsonMarshaller = null;
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
        this.options = new CloudServiceOptions(properties, this.systemService);
        //
        // install event listener for GPS locked event
        Dictionary<String, Object> props = new Hashtable<>();
        String[] eventTopics = { PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC,
                ModemReadyEvent.MODEM_EVENT_READY_TOPIC };
        props.put(EventConstants.EVENT_TOPIC, eventTopics);
        this.cloudServiceRegistration = this.ctx.getBundleContext().registerService(EventHandler.class.getName(), this,
                props);

        this.dataService.addDataServiceListener(this);

        Dictionary<String, Object> notificationPublisherProps = new Hashtable<>();
        notificationPublisherProps.put(KURA_SERVICE_PID, NOTIFICATION_PUBLISHER_PID);
        notificationPublisherProps.put(SERVICE_PID, NOTIFICATION_PUBLISHER_PID);
        this.notificationPublisherRegistration = this.ctx.getBundleContext().registerService(
                CloudNotificationPublisher.class.getName(), this.notificationPublisher, notificationPublisherProps);

        //
        // Usually the cloud connection is setup in the
        // onConnectionEstablished callback.
        // Since the callback may be lost if we are activated
        // too late (the DataService is already connected) we
        // setup the cloud connection here.
        if (isConnected()) {
            logger.warn("DataService is already connected. Publish BIRTH certificate");
            try {
                setupCloudConnection(true);
            } catch (KuraException e) {
                logger.warn("Cannot setup cloud service connection", e);
            }
        }
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        // Update properties and re-publish Birth certificate
        this.options = new CloudServiceOptions(properties, this.systemService);
        if (isConnected()) {
            try {
                setupCloudConnection(false);
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

        // no need to release the cloud clients as the updated app
        // certificate is already published due the missing dependency
        // we only need to empty our CloudClient list
        this.cloudClients.clear();

        this.dataService = null;
        this.systemService = null;
        this.systemAdminService = null;
        this.networkService = null;
        this.positionService = null;
        this.eventAdmin = null;
        this.certificatesService = null;

        this.cloudServiceRegistration.unregister();
        this.notificationPublisherRegistration.unregister();
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

            if (this.dataService.isConnected() && this.options.getRepubBirthCertOnModemDetection()) {
                if (!((this.imei == null || this.imei.length() == 0 || this.imei.equals("ERROR"))
                        && (this.imsi == null || this.imsi.length() == 0 || this.imsi.equals("ERROR"))
                        && (this.iccid == null || this.iccid.length() == 0 || this.iccid.equals("ERROR")))) {
                    logger.debug("handleEvent() :: publishing BIRTH certificate ...");
                    try {
                        publishBirthCertificate();
                    } catch (KuraException e) {
                        logger.warn("Cannot publish birth certificate", e);
                    }
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
    public CloudClient newCloudClient(String applicationId) throws KuraException {
        // create new instance
        CloudClientImpl cloudClient = new CloudClientImpl(applicationId, this.dataService, this);
        this.cloudClients.add(cloudClient);

        // publish updated birth certificate with list of active apps
        if (isConnected()) {
            publishAppCertificate();
        }

        // return
        return cloudClient;
    }

    @Override
    public String[] getCloudApplicationIdentifiers() {
        List<String> appIds = new ArrayList<>();
        for (CloudClientImpl cloudClient : this.cloudClients) {
            appIds.add(cloudClient.getApplicationId());
        }

        for (Entry<String, RequestHandler> entry : this.registeredRequestHandlers.entrySet()) {
            appIds.add(entry.getKey());
        }
        return appIds.toArray(new String[0]);
    }

    @Override
    public boolean isConnected() {
        return this.dataService != null && this.dataService.isConnected();
    }

    // ----------------------------------------------------------------
    //
    // Package APIs
    //
    // ----------------------------------------------------------------

    public CloudServiceOptions getCloudServiceOptions() {
        return this.options;
    }

    public void removeCloudClient(CloudClientImpl cloudClient) {
        // remove the client
        this.cloudClients.remove(cloudClient);

        // publish updated birth certificate with updated list of active apps
        if (isConnected()) {
            try {
                publishAppCertificate();
            } catch (KuraException e) {
                logger.warn("Cannot publish app certificate");
            }
        }
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
            setupCloudConnection(true);
        } catch (KuraException e) {
            logger.warn("Cannot setup cloud service connection");
        }

        this.registeredSubscribers.keySet().forEach(this::subscribe);

        postConnectionStateChangeEvent(true);

        this.cloudClients.forEach(CloudClientImpl::onConnectionEstablished);

        this.registeredCloudConnectionListeners.forEach(CloudConnectionListener::onConnectionEstablished);
    }

    private void setupDeviceSubscriptions(boolean subscribe) throws KuraException {
        StringBuilder sbDeviceSubscription = new StringBuilder();
        sbDeviceSubscription.append(this.options.getTopicControlPrefix())
                .append(CloudServiceOptions.getTopicSeparator()).append(CloudServiceOptions.getTopicAccountToken())
                .append(CloudServiceOptions.getTopicSeparator()).append(CloudServiceOptions.getTopicClientIdToken())
                .append(CloudServiceOptions.getTopicSeparator()).append(CloudServiceOptions.getTopicWildCard());

        // restore or remove default subscriptions
        if (subscribe) {
            this.dataService.subscribe(sbDeviceSubscription.toString(), 1);
        } else {
            this.dataService.unsubscribe(sbDeviceSubscription.toString());
        }
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

        this.cloudClients.forEach(CloudClientImpl::onConnectionLost);

        this.registeredCloudConnectionListeners.forEach(CloudConnectionListener::onConnectionLost);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        logger.info("Message arrived on topic: {}", topic);

        // notify listeners
        KuraTopicImpl kuraTopic = new KuraTopicImpl(topic, this.options.getTopicControlPrefix());
        if (TOPIC_MQTT_APP.equals(kuraTopic.getApplicationId()) || TOPIC_BA_APP.equals(kuraTopic.getApplicationId())) {
            logger.info("Ignoring feedback message from {}", topic);
        } else {
            KuraPayload kuraPayload = encodeKuraPayload(topic, payload);

            try {
                if (this.options.getTopicControlPrefix().equals(kuraTopic.getPrefix())) {
                    boolean validMessage = isValidMessage(kuraTopic, kuraPayload);

                    if (validMessage) {
                        dispatchControlMessage(qos, retained, kuraTopic, kuraPayload);
                    } else {
                        logger.warn("Message verification failed! Not valid signature or message not signed.");
                    }
                } else {
                    dispatchDataMessage(qos, retained, kuraTopic, kuraPayload);
                }
            } catch (Exception e) {
                logger.error("Error during CloudClientListener notification.", e);
            }
        }

    }

    private KuraPayload encodeKuraPayload(String topic, byte[] payload) {
        KuraPayload kuraPayload = null;
        if (this.options.getPayloadEncoding() == SIMPLE_JSON) {
            try {
                kuraPayload = createKuraPayloadFromJson(payload);
            } catch (KuraException e) {
                logger.warn("Error creating Kura Payload from Json", e);
            }
        } else if (this.options.getPayloadEncoding() == KURA_PROTOBUF) {
            kuraPayload = createKuraPayloadFromProtoBuf(topic, payload);
        }
        return kuraPayload;
    }

    private void dispatchControlMessage(int qos, boolean retained, KuraTopicImpl kuraTopic, KuraPayload kuraPayload) {
        String applicationId = kuraTopic.getApplicationId();

        RequestHandler cloudlet = this.registeredRequestHandlers.get(applicationId);
        if (cloudlet != null) {
            StringBuilder sb = new StringBuilder(applicationId).append("/").append("REPLY");

            if (kuraTopic.getApplicationTopic().startsWith(sb.toString())) {
                // Ignore replies
                return;
            }

            callbackExecutor.submit(new MessageHandlerCallable(cloudlet, applicationId, kuraTopic.getApplicationTopic(),
                    kuraPayload, this));
        }
        this.cloudClients.stream()
                .filter(cloudClient -> cloudClient.getApplicationId().equals(kuraTopic.getApplicationId()))
                .forEach(cloudClient -> cloudClient.onControlMessageArrived(kuraTopic.getDeviceId(),
                        kuraTopic.getApplicationTopic(), kuraPayload, qos, retained));

        Map<String, Object> properties = new HashMap<>();
        properties.put("deviceId", kuraTopic.getDeviceId());
        properties.put("appTopic", kuraTopic.getApplicationTopic());

        KuraMessage receivedMessage = new KuraMessage(kuraPayload, properties);

        this.registeredSubscribers.entrySet().stream()
                .filter(cloudSubscriberEntry -> cloudSubscriberEntry.getKey().matches(kuraTopic.getFullTopic()))
                .forEach(e -> dispatchMessage(receivedMessage, e.getValue()));
    }

    private void dispatchDataMessage(int qos, boolean retained, KuraTopicImpl kuraTopic, KuraPayload kuraPayload) {
        this.cloudClients.stream()
                .filter(cloudClient -> cloudClient.getApplicationId().equals(kuraTopic.getApplicationId()))
                .forEach(cloudClient -> cloudClient.onMessageArrived(kuraTopic.getDeviceId(),
                        kuraTopic.getApplicationTopic(), kuraPayload, qos, retained));

        Map<String, Object> properties = new HashMap<>();
        properties.put("deviceId", kuraTopic.getDeviceId());
        properties.put("appTopic", kuraTopic.getApplicationTopic());

        KuraMessage receivedMessage = new KuraMessage(kuraPayload, properties);

        this.registeredSubscribers.entrySet().stream()
                .filter(cloudSubscriberEntry -> cloudSubscriberEntry.getKey().matches(kuraTopic.getFullTopic()))
                .forEach(e -> dispatchMessage(receivedMessage, e.getValue()));
    }

    private static void dispatchMessage(final KuraMessage message, final List<CloudSubscriberListener> listeners) {
        for (final CloudSubscriberListener listener : listeners) {
            try {
                listener.onMessageArrived(message);
            } catch (final Exception e) {
                logger.warn("unhandled exception in CloudSubscriberListener", e);
            }
        }
    }

    private boolean isValidMessage(KuraApplicationTopic kuraTopic, KuraPayload kuraPayload) {
        if (this.certificatesService == null) {
            ServiceReference<CertificatesService> sr = this.ctx.getBundleContext()
                    .getServiceReference(CertificatesService.class);
            if (sr != null) {
                this.certificatesService = this.ctx.getBundleContext().getService(sr);
            }
        }
        boolean validMessage = false;
        if (this.certificatesService == null || this.certificatesService.verifySignature(kuraTopic, kuraPayload)) {
            validMessage = true;
        }
        return validMessage;
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        synchronized (this.messageId) {
            if (this.messageId.get() != -1 && this.messageId.get() == messageId) {
                if (CloudServiceOptions.getLifeCycleMessageQos() == 0) {
                    this.messageId.set(-1);
                }
                this.messageId.notifyAll();
                return;
            }
        }

        // notify listeners
        KuraApplicationTopic kuraTopic = new KuraTopicImpl(topic, this.options.getTopicControlPrefix());
        this.cloudClients.stream()
                .filter(cloudClient -> cloudClient.getApplicationId().equals(kuraTopic.getApplicationId()))
                .collect(Collectors.toList())
                .forEach(cloudClient -> cloudClient.onMessagePublished(messageId, kuraTopic.getApplicationTopic()));
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        synchronized (this.messageId) {
            if (this.messageId.get() != -1 && this.messageId.get() == messageId) {
                this.messageId.set(-1);
                this.messageId.notifyAll();
                return;
            }
        }

        // notify listeners
        KuraApplicationTopic kuraTopic = new KuraTopicImpl(topic, this.options.getTopicControlPrefix());
        this.cloudClients.stream()
                .filter(cloudClient -> cloudClient.getApplicationId().equals(kuraTopic.getApplicationId()))
                .collect(Collectors.toList())
                .forEach(cloudClient -> cloudClient.onMessageConfirmed(messageId, kuraTopic.getApplicationTopic()));

        this.registeredCloudPublisherDeliveryListeners
                .forEach(deliveryListener -> deliveryListener.onMessageConfirmed(String.valueOf(messageId), topic));

        this.registeredCloudDeliveryListeners
                .forEach(deliveryListener -> deliveryListener.onMessageConfirmed(String.valueOf(messageId)));
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

    private void setupCloudConnection(boolean onConnect) throws KuraException {
        // assume we are not yet subscribed
        if (onConnect) {
            this.subscribed = false;
        }

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

        // restore or remove default subscriptions
        if (this.options.getEnableDefaultSubscriptions()) {
            if (!this.subscribed) {
                setupDeviceSubscriptions(true);
                this.subscribed = true;
            }
        } else {
            logger.info("Default subscriptions are disabled in configuration");
            if (this.subscribed) {
                setupDeviceSubscriptions(false);
                this.subscribed = false;
            }
        }
    }

    private void publishBirthCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append(this.options.getTopicControlPrefix()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicAccountToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicClientIdToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicBirthSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createBirthPayload();
        publishLifeCycleMessage(topic, payload);
    }

    private void publishDisconnectCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append(this.options.getTopicControlPrefix()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicAccountToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicClientIdToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicDisconnectSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createDisconnectPayload();
        publishLifeCycleMessage(topic, payload);
    }

    private void publishAppCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append(this.options.getTopicControlPrefix()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicAccountToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicClientIdToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicAppsSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createBirthPayload();
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
            // add a timestamp to the message
            payload.setTimestamp(new Date());
            byte[] encodedPayload = encodePayload(payload);
            int messageId = this.dataService.publish(topic, encodedPayload,
                    CloudServiceOptions.getLifeCycleMessageQos(), CloudServiceOptions.getLifeCycleMessageRetain(),
                    CloudServiceOptions.getLifeCycleMessagePriority());
            this.messageId.set(messageId);
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

    private byte[] encodeJsonPayload(KuraPayload payload) throws KuraException {
        return this.jsonMarshaller.marshal(payload).getBytes(StandardCharsets.UTF_8);
    }

    private KuraPayload createKuraPayloadFromJson(byte[] payload) throws KuraException {
        return this.jsonUnmarshaller.unmarshal(new String(payload), KuraPayload.class);
    }

    private KuraPayload createKuraPayloadFromProtoBuf(String topic, byte[] payload) {
        KuraPayload kuraPayload;
        try {
            // try to decode the message into an KuraPayload
            kuraPayload = new CloudPayloadProtoBufDecoderImpl(payload).buildFromByteArray();
        } catch (Exception e) {
            // Wrap the received bytes payload into an KuraPayload
            logger.debug("Received message on topic {} that could not be decoded. Wrapping it into an KuraPayload.",
                    topic);
            kuraPayload = new KuraPayload();
            kuraPayload.setBody(payload);
        }
        return kuraPayload;
    }

    private void postConnectionStateChangeEvent(final boolean isConnected) {

        final Map<String, Object> eventProperties = Collections.singletonMap(CONNECTION_EVENT_PID_PROPERTY_KEY,
                (String) this.ctx.getProperties().get(ConfigurationService.KURA_SERVICE_PID));

        final Event event = isConnected ? new CloudConnectionEstablishedEvent(eventProperties)
                : new CloudConnectionLostEvent(eventProperties);
        this.eventAdmin.postEvent(event);
    }

    @Override
    public void registerRequestHandler(String appId, RequestHandler requestHandler) {
        this.registeredRequestHandlers.put(appId, requestHandler);
        if (isConnected()) {
            try {
                publishAppCertificate();
            } catch (KuraException e) {
                logger.warn("Unable to publish updated App Certificate");
            }
        }
    }

    @Override
    public void unregister(String appId) {
        this.registeredRequestHandlers.remove(appId);
        if (isConnected()) {
            try {
                publishAppCertificate();
            } catch (KuraException e) {
                logger.warn("Unable to publish updated App Certificate");
            }
        }
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

    public String getNotificationPublisherPid() {
        return NOTIFICATION_PUBLISHER_PID;
    }

    public CloudNotificationPublisher getNotificationPublisher() {
        return this.notificationPublisher;
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
        int qos = (Integer) messageProps.get(QOS.name());
        boolean retain = (Boolean) messageProps.get(RETAIN.name());
        int priority = (Integer) messageProps.get(PRIORITY.name());

        String fullTopic = (String) messageProps.get(FULL_TOPIC.name());

        if (isNull(fullTopic)) {
            String appId = (String) messageProps.get(APP_ID.name());
            String appTopic = (String) messageProps.get(APP_TOPIC.name());
            boolean isControl = (Boolean) messageProps.get(CONTROL.name());

            String deviceId = CloudServiceOptions.getTopicClientIdToken();

            fullTopic = encodeTopic(appId, deviceId, appTopic, isControl);
        }

        byte[] appPayload = encodePayload(message.getPayload());

        int id = this.dataService.publish(fullTopic, appPayload, qos, retain, priority);

        if (qos == 0) {
            return null;
        }
        return String.valueOf(id);
    }

    @Override
    public void registerSubscriber(Map<String, Object> subscriptionProperties, CloudSubscriberListener subscriber) {
        String appId = (String) subscriptionProperties.get(APP_ID.name());
        String appTopic = (String) subscriptionProperties.get(APP_TOPIC.name());
        int qos = (Integer) subscriptionProperties.get(QOS.name());
        boolean isControl = (Boolean) subscriptionProperties.get(CONTROL.name());

        if (isNull(appId) || isNull(appTopic)) {
            return;
        }

        String fullTopic = encodeTopic(appId, CloudServiceOptions.getTopicClientIdToken(), appTopic, isControl);

        CloudSubscriptionRecord subscriptionRecord = new CloudSubscriptionRecord(fullTopic, qos);

        final List<CloudSubscriberListener> subscribers;

        synchronized (this) {
            subscribers = this.registeredSubscribers.compute(subscriptionRecord, (t, list) -> {
                if (list == null) {
                    return new CopyOnWriteArrayList<>(Collections.singletonList(subscriber));
                }
                list.add(subscriber);
                return list;
            });
        }

        if (subscribers.size() == 1) {
            subscribe(subscriptionRecord);
        }
    }

    @Override
    public void unregisterSubscriber(CloudSubscriberListener subscriber) {

        final List<CloudSubscriptionRecord> toUnsubscribe = new ArrayList<>();

        synchronized (this) {
            this.registeredSubscribers.entrySet().removeIf(e -> {
                final List<CloudSubscriberListener> subscribers = e.getValue();

                subscribers.removeIf(s -> s == subscriber);

                if (subscribers.isEmpty()) {
                    toUnsubscribe.add(e.getKey());
                    return true;
                } else {
                    return false;
                }
            });
        }

        for (final CloudSubscriptionRecord subscription : toUnsubscribe) {
            unsubscribe(subscription);
        }
    }

    private synchronized void subscribe(CloudSubscriptionRecord subscriptionRecord) {
        String fullTopic = subscriptionRecord.getTopic();

        int qos = subscriptionRecord.getQos();

        try {
            this.dataService.subscribe(fullTopic, qos);
        } catch (KuraException e) {
            logger.info("Failed to subscribe");
        }
    }

    private synchronized void unsubscribe(CloudSubscriptionRecord subscriptionRecord) {
        String fullTopic = subscriptionRecord.getTopic();

        try {
            this.dataService.unsubscribe(fullTopic);
        } catch (KuraException e) {
            logger.info("Failed to unsubscribe");
        }
    }

    private String encodeTopic(String appId, String deviceId, String appTopic, boolean isControl) {
        StringBuilder sb = new StringBuilder();
        if (isControl) {
            sb.append(this.options.getTopicControlPrefix()).append(CloudServiceOptions.getTopicSeparator());
        }

        sb.append(CloudServiceOptions.getTopicAccountToken()).append(CloudServiceOptions.getTopicSeparator())
                .append(deviceId).append(CloudServiceOptions.getTopicSeparator()).append(appId);

        if (appTopic != null && !appTopic.isEmpty()) {
            sb.append(CloudServiceOptions.getTopicSeparator()).append(appTopic);
        }

        return sb.toString();
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.registeredCloudDeliveryListeners.add(cloudDeliveryListener);

    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.registeredCloudDeliveryListeners.remove(cloudDeliveryListener);
    }
}
