/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraTopic;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServiceImpl implements CloudService, DataServiceListener, ConfigurableComponent, EventHandler,
        CloudPayloadProtoBufEncoder, CloudPayloadProtoBufDecoder {

    private static final Logger logger = LoggerFactory.getLogger(CloudServiceImpl.class);

    private static final String TOPIC_BA_APP = "BA";
    private static final String TOPIC_MQTT_APP = "MQTT";

    private ComponentContext ctx;

    private CloudServiceOptions options;

    private DataService dataService;
    private SystemService systemService;
    private SystemAdminService systemAdminService;
    private NetworkService networkService;
    private PositionService positionService;
    private EventAdmin eventAdmin;
    private CertificatesService certificatesService;

    // use a synchronized implementation for the list
    private final List<CloudClientImpl> cloudClients;

    // package visibility for LyfeCyclePayloadBuilder
    String imei;
    String iccid;
    String imsi;
    String rssi;

    private boolean subscribed;
    private boolean birthPublished;

    private final AtomicInteger messageId;

    public CloudServiceImpl() {
        this.cloudClients = new CopyOnWriteArrayList<CloudClientImpl>();
        this.messageId = new AtomicInteger();
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
        this.options = new CloudServiceOptions(properties, this.systemService);

        //
        // install event listener for GPS locked event
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        String[] eventTopics = { PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC,
                ModemReadyEvent.MODEM_EVENT_READY_TOPIC };
        props.put(EventConstants.EVENT_TOPIC, eventTopics);
        this.ctx.getBundleContext().registerService(EventHandler.class.getName(), this, props);

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
                setupCloudConnection(true);
            } catch (KuraException e) {
                logger.warn("Cannot setup cloud service connection", e);
            }
        }
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated {}...: {}", properties.get(ConfigurationService.KURA_SERVICE_PID), properties);

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
        List<String> appIds = new ArrayList<String>();
        for (CloudClientImpl cloudClient : this.cloudClients) {
            appIds.add(cloudClient.getApplicationId());
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

    byte[] encodePayload(KuraPayload payload) throws KuraException {
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
            return bytes;
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, e);
        }
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

        // raise event
        this.eventAdmin.postEvent(new CloudConnectionEstablishedEvent(new HashMap<String, Object>()));

        // notify listeners
        for (CloudClientImpl cloudClient : this.cloudClients) {
            cloudClient.onConnectionEstablished();
        }
    }

    private void setupDeviceSubscriptions(boolean subscribe) throws KuraException {
        StringBuilder sbDeviceSubscription = new StringBuilder();
        sbDeviceSubscription.append(this.options.getTopicControlPrefix()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicAccountToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicClientIdToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicWildCard());

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
        this.eventAdmin.postEvent(new CloudConnectionLostEvent(new HashMap<String, Object>()));
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        // raise event
        this.eventAdmin.postEvent(new CloudConnectionLostEvent(new HashMap<String, Object>()));

        // notify listeners
        for (CloudClientImpl cloudClient : this.cloudClients) {
            cloudClient.onConnectionLost();
        }
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        logger.info("Message arrived on topic: {}", topic);

        // notify listeners
        KuraTopic kuraTopic = new KuraTopic(topic, this.options.getTopicControlPrefix());
        if (TOPIC_MQTT_APP.equals(kuraTopic.getApplicationId()) || TOPIC_BA_APP.equals(kuraTopic.getApplicationId())) {
            logger.info("Ignoring feedback message from " + topic);
        } else {
            KuraPayload kuraPayload = null;
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

            for (CloudClientImpl cloudClient : this.cloudClients) {
                if (cloudClient.getApplicationId().equals(kuraTopic.getApplicationId())) {
                    try {
                        if (this.options.getTopicControlPrefix().equals(kuraTopic.getPrefix())) {
                            if (this.certificatesService == null) {
                                ServiceReference<CertificatesService> sr = this.ctx.getBundleContext()
                                        .getServiceReference(CertificatesService.class);
                                if (sr != null) {
                                    this.certificatesService = this.ctx.getBundleContext().getService(sr);
                                }
                            }
                            boolean validMessage = false;
                            if (this.certificatesService == null) {
                                validMessage = true;
                            } else if (this.certificatesService.verifySignature(kuraTopic, kuraPayload)) {
                                validMessage = true;
                            }

                            if (validMessage) {
                                cloudClient.onControlMessageArrived(kuraTopic.getDeviceId(),
                                        kuraTopic.getApplicationTopic(), kuraPayload, qos, retained);
                            } else {
                                logger.warn("Message verification failed! Not valid signature or message not signed.");
                            }
                        } else {
                            cloudClient.onMessageArrived(kuraTopic.getDeviceId(), kuraTopic.getApplicationTopic(),
                                    kuraPayload, qos, retained);
                        }
                    } catch (Exception e) {
                        logger.error("Error during CloudClientListener notification.", e);
                    }
                }

            }
        }
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        synchronized (this.messageId) {
            if (this.messageId.get() != -1 && this.messageId.get() == messageId) {
                if (this.options.getLifeCycleMessageQos() == 0) {
                    this.messageId.set(-1);
                }
                this.messageId.notifyAll();
                return;
            }
        }

        // notify listeners
        KuraTopic kuraTopic = new KuraTopic(topic, this.options.getTopicControlPrefix());
        for (CloudClientImpl cloudClient : this.cloudClients) {
            if (cloudClient.getApplicationId().equals(kuraTopic.getApplicationId())) {
                cloudClient.onMessagePublished(messageId, kuraTopic.getApplicationTopic());
            }
        }
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
        KuraTopic kuraTopic = new KuraTopic(topic, this.options.getTopicControlPrefix());
        for (CloudClientImpl cloudClient : this.cloudClients) {
            if (cloudClient.getApplicationId().equals(kuraTopic.getApplicationId())) {
                cloudClient.onMessageConfirmed(messageId, kuraTopic.getApplicationTopic());
            }
        }
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
        } catch (KuraInvalidMessageException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, e);
        } catch (IOException e) {
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
        sbTopic.append(this.options.getTopicControlPrefix()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicAccountToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicClientIdToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicBirthSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createBirthPayload();
        publishLifeCycleMessage(topic, payload);
    }

    private void publishDisconnectCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append(this.options.getTopicControlPrefix()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicAccountToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicClientIdToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicDisconnectSuffix());

        String topic = sbTopic.toString();
        KuraPayload payload = createDisconnectPayload();
        publishLifeCycleMessage(topic, payload);
    }

    private void publishAppCertificate() throws KuraException {
        if (this.options.isLifecycleCertsDisabled()) {
            return;
        }

        StringBuilder sbTopic = new StringBuilder();
        sbTopic.append(this.options.getTopicControlPrefix()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicAccountToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicClientIdToken()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicAppsSuffix());

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
            byte[] encodedPayload = encodePayload(payload);
            int messageId = this.dataService.publish(topic, encodedPayload, this.options.getLifeCycleMessageQos(),
                    this.options.getLifeCycleMessageRetain(), this.options.getLifeCycleMessagePriority());
            this.messageId.set(messageId);
            try {
                this.messageId.wait(1000);
            } catch (InterruptedException e) {
                logger.info("Interrupted while waiting for the message to be published", e);
            }
        }
    }
}
