/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the CloudClient interface.
 */
public class CloudClientImpl implements CloudClient, CloudClientListener {

    @SuppressWarnings("unused")
    private static final Logger s_logger = LoggerFactory.getLogger(CloudClientImpl.class);

    private final String m_applicationId;
    private final DataService m_dataService;
    private final CloudServiceImpl m_cloudServiceImpl;

    private final List<CloudClientListenerAdapter> m_listeners;

    protected CloudClientImpl(String applicationId, DataService dataService, CloudServiceImpl cloudServiceImpl) {
        this.m_applicationId = applicationId;
        this.m_dataService = dataService;
        this.m_cloudServiceImpl = cloudServiceImpl;
        this.m_listeners = new CopyOnWriteArrayList<CloudClientListenerAdapter>();
    }

    /**
     * Returns the applicationId of this CloudApplicationClient
     *
     * @return applicationId
     */
    @Override
    public String getApplicationId() {
        return this.m_applicationId;
    }

    /**
     * Releases this CloudClient handle. This instance should no longer be used.
     * Note: CloudClient does not unsubscribes all subscriptions incurred by this client,
     * this responsibility is left to the application developer
     */
    @Override
    public void release() {
        // remove this from being a callback handler
        this.m_cloudServiceImpl.removeCloudClient(this);
    }

    // --------------------------------------------------------------------
    //
    // CloudCallbackHandler API
    //
    // --------------------------------------------------------------------

    @Override
    public void addCloudClientListener(CloudClientListener cloudClientListener) {
        this.m_listeners.add(new CloudClientListenerAdapter(cloudClientListener));
    }

    @Override
    public void removeCloudClientListener(CloudClientListener cloudClientListener) {
        // create a copy to avoid concurrent modification exceptions
        List<CloudClientListenerAdapter> adapters = new ArrayList<CloudClientListenerAdapter>(this.m_listeners);
        for (CloudClientListenerAdapter adapter : adapters) {
            if (adapter.getCloudClientListenerAdapted() == cloudClientListener) {
                this.m_listeners.remove(adapter);
                break;
            }
        }
    }

    // --------------------------------------------------------------------
    //
    // CloudClient API
    //
    // --------------------------------------------------------------------

    @Override
    public boolean isConnected() {
        return this.m_dataService.isConnected();
    }

    @Override
    public int publish(String appTopic, KuraPayload payload, int qos, boolean retain) throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        return publish(options.getTopicClientIdToken(), appTopic, payload, qos, retain);
    }

    @Override
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain)
            throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        byte[] appPayload = this.m_cloudServiceImpl.encodePayload(payload);
        return this.m_dataService.publish(fullTopic, appPayload, qos, retain, 5);
    }

    @Override
    public int publish(String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        return publish(options.getTopicClientIdToken(), appTopic, payload, qos, retain, priority);
    }

    @Override
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        byte[] appPayload = this.m_cloudServiceImpl.encodePayload(payload);
        return this.m_dataService.publish(fullTopic, appPayload, qos, retain, priority);
    }

    @Override
    public int publish(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        return publish(options.getTopicClientIdToken(), payload, qos, retain, priority);
    }

    @Override
    public int publish(String deviceId, String appTopic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        return this.m_dataService.publish(fullTopic, payload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String topic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        return controlPublish(options.getTopicClientIdToken(), topic, payload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String topic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        boolean isControl = true;
        String fullTopic = encodeTopic(deviceId, topic, isControl);
        byte[] appPayload = this.m_cloudServiceImpl.encodePayload(payload);
        return this.m_dataService.publish(fullTopic, appPayload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException {
        boolean isControl = true;
        String appTopic = encodeTopic(deviceId, topic, isControl);
        return this.m_dataService.publish(appTopic, payload, qos, retain, priority);
    }

    @Override
    public void subscribe(String appTopic, int qos) throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        subscribe(options.getTopicClientIdToken(), appTopic, qos);
    }

    @Override
    public void subscribe(String deviceId, String appTopic, int qos) throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.m_dataService.subscribe(fullTopic, qos);
    }

    @Override
    public void controlSubscribe(String appTopic, int qos) throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        controlSubscribe(options.getTopicClientIdToken(), appTopic, qos);
    }

    @Override
    public void controlSubscribe(String deviceId, String appTopic, int qos) throws KuraException {
        boolean isControl = true;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.m_dataService.subscribe(fullTopic, qos);
    }

    @Override
    public void unsubscribe(String appTopic) throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        unsubscribe(options.getTopicClientIdToken(), appTopic);
    }

    @Override
    public void unsubscribe(String deviceId, String appTopic) throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.m_dataService.unsubscribe(fullTopic);
    }

    @Override
    public void controlUnsubscribe(String appTopic) throws KuraException {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        controlUnsubscribe(options.getTopicClientIdToken(), appTopic);
    }

    @Override
    public void controlUnsubscribe(String deviceId, String appTopic) throws KuraException {
        boolean isControl = true;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.m_dataService.unsubscribe(fullTopic);
    }

    @Override
    public List<Integer> getUnpublishedMessageIds() throws KuraException {
        String topicRegex = getAppTopicRegex();
        return this.m_dataService.getUnpublishedMessageIds(topicRegex);
    }

    @Override
    public List<Integer> getInFlightMessageIds() throws KuraException {
        String topicRegex = getAppTopicRegex();
        return this.m_dataService.getInFlightMessageIds(topicRegex);
    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
        String topicRegex = getAppTopicRegex();
        return this.m_dataService.getDroppedInFlightMessageIds(topicRegex);
    }

    // --------------------------------------------------------------------
    //
    // CloudCallbackHandler API
    //
    // --------------------------------------------------------------------

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain) {
        for (CloudClientListener listener : this.m_listeners) {
            listener.onMessageArrived(deviceId, appTopic, payload, qos, retain);
        }
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos,
            boolean retain) {
        for (CloudClientListener listener : this.m_listeners) {
            listener.onControlMessageArrived(deviceId, appTopic, payload, qos, retain);
        }
    }

    @Override
    public void onMessageConfirmed(int pubId, String appTopic) {
        for (CloudClientListener listener : this.m_listeners) {
            listener.onMessageConfirmed(pubId, appTopic);
        }
    }

    @Override
    public void onMessagePublished(int pubId, String appTopic) {
        for (CloudClientListener listener : this.m_listeners) {
            listener.onMessagePublished(pubId, appTopic);
        }
    }

    @Override
    public void onConnectionEstablished() {
        for (CloudClientListener listener : this.m_listeners) {
            listener.onConnectionEstablished();
        }
    }

    @Override
    public void onConnectionLost() {
        for (CloudClientListener listener : this.m_listeners) {
            listener.onConnectionLost();
        }
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private String encodeTopic(String deviceId, String topic, boolean isControl) {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        StringBuilder sb = new StringBuilder();
        if (isControl) {
            sb.append(options.getTopicControlPrefix()).append(options.getTopicSeparator());
        }

        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(deviceId)
                .append(options.getTopicSeparator()).append(this.m_applicationId);

        if (topic != null && !topic.isEmpty()) {
            sb.append(options.getTopicSeparator()).append(topic);
        }

        return sb.toString();
    }

    private String getAppTopicRegex() {
        CloudServiceOptions options = this.m_cloudServiceImpl.getCloudServiceOptions();
        StringBuilder sb = new StringBuilder();

        // String regexExample = "^(\\$EDC/)?eurotech/.+/conf-v1(/.+)?";

        // Optional control prefix
        sb.append("^(")
                // .append(options.getTopicControlPrefix())
                .append("\\$EDC").append(options.getTopicSeparator()).append(")?")

        .append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(".+") // Any device ID
                .append(options.getTopicSeparator()).append(this.m_applicationId).append("(/.+)?");

        return sb.toString();
    }
}
