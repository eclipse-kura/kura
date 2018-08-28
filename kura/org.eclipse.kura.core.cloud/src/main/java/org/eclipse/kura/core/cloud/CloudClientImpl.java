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
    private static final Logger logger = LoggerFactory.getLogger(CloudClientImpl.class);

    private final String applicationId;
    private final DataService dataService;
    private final CloudServiceImpl cloudServiceImpl;

    private final List<CloudClientListenerAdapter> listeners;

    protected CloudClientImpl(String applicationId, DataService dataService, CloudServiceImpl cloudServiceImpl) {
        this.applicationId = applicationId;
        this.dataService = dataService;
        this.cloudServiceImpl = cloudServiceImpl;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Returns the applicationId of this CloudApplicationClient
     *
     * @return applicationId
     */
    @Override
    public String getApplicationId() {
        return this.applicationId;
    }

    /**
     * Releases this CloudClient handle. This instance should no longer be used.
     * Note: CloudClient does not unsubscribes all subscriptions incurred by this client,
     * this responsibility is left to the application developer
     */
    @Override
    public void release() {
        // remove this from being a callback handler
        this.cloudServiceImpl.removeCloudClient(this);
    }

    // --------------------------------------------------------------------
    //
    // CloudCallbackHandler API
    //
    // --------------------------------------------------------------------

    @Override
    public void addCloudClientListener(CloudClientListener cloudClientListener) {
        this.listeners.add(new CloudClientListenerAdapter(cloudClientListener));
    }

    @Override
    public void removeCloudClientListener(CloudClientListener cloudClientListener) {
        // create a copy to avoid concurrent modification exceptions
        List<CloudClientListenerAdapter> adapters = new ArrayList<>(this.listeners);
        for (CloudClientListenerAdapter adapter : adapters) {
            if (adapter.getCloudClientListenerAdapted() == cloudClientListener) {
                this.listeners.remove(adapter);
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
        return this.dataService.isConnected();
    }

    @Override
    public int publish(String appTopic, KuraPayload payload, int qos, boolean retain) throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        return publish(options.getTopicClientIdToken(), appTopic, payload, qos, retain);
    }

    @Override
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain)
            throws KuraException {
        return publish(deviceId, appTopic, payload, qos, retain, 5);
    }

    @Override
    public int publish(String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        return publish(options.getTopicClientIdToken(), appTopic, payload, qos, retain, priority);
    }

    @Override
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        byte[] appPayload = this.cloudServiceImpl.encodePayload(payload);
        return publish(deviceId, appTopic, appPayload, qos, retain, priority);
    }

    @Override
    public int publish(String appTopic, byte[] payload, int qos, boolean retain, int priority) throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        return publish(options.getTopicClientIdToken(), appTopic, payload, qos, retain, priority);
    }

    @Override
    public int publish(String deviceId, String appTopic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        return this.dataService.publish(fullTopic, payload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        return controlPublish(options.getTopicClientIdToken(), appTopic, payload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain,
            int priority) throws KuraException {
        byte[] appPayload = this.cloudServiceImpl.encodePayload(payload);
        return controlPublish(deviceId, appTopic, appPayload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String appTopic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException {
        boolean isControl = true;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        return this.dataService.publish(fullTopic, payload, qos, retain, priority);
    }

    @Override
    public void subscribe(String appTopic, int qos) throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        subscribe(options.getTopicClientIdToken(), appTopic, qos);
    }

    @Override
    public void subscribe(String deviceId, String appTopic, int qos) throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.dataService.subscribe(fullTopic, qos);
    }

    @Override
    public void controlSubscribe(String appTopic, int qos) throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        controlSubscribe(options.getTopicClientIdToken(), appTopic, qos);
    }

    @Override
    public void controlSubscribe(String deviceId, String appTopic, int qos) throws KuraException {
        boolean isControl = true;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.dataService.subscribe(fullTopic, qos);
    }

    @Override
    public void unsubscribe(String appTopic) throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        unsubscribe(options.getTopicClientIdToken(), appTopic);
    }

    @Override
    public void unsubscribe(String deviceId, String appTopic) throws KuraException {
        boolean isControl = false;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.dataService.unsubscribe(fullTopic);
    }

    @Override
    public void controlUnsubscribe(String appTopic) throws KuraException {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        controlUnsubscribe(options.getTopicClientIdToken(), appTopic);
    }

    @Override
    public void controlUnsubscribe(String deviceId, String appTopic) throws KuraException {
        boolean isControl = true;
        String fullTopic = encodeTopic(deviceId, appTopic, isControl);
        this.dataService.unsubscribe(fullTopic);
    }

    @Override
    public List<Integer> getUnpublishedMessageIds() throws KuraException {
        String topicRegex = getAppTopicRegex();
        return this.dataService.getUnpublishedMessageIds(topicRegex);
    }

    @Override
    public List<Integer> getInFlightMessageIds() throws KuraException {
        String topicRegex = getAppTopicRegex();
        return this.dataService.getInFlightMessageIds(topicRegex);
    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
        String topicRegex = getAppTopicRegex();
        return this.dataService.getDroppedInFlightMessageIds(topicRegex);
    }

    // --------------------------------------------------------------------
    //
    // CloudCallbackHandler API
    //
    // --------------------------------------------------------------------

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain) {
        for (CloudClientListener listener : this.listeners) {
            listener.onMessageArrived(deviceId, appTopic, payload, qos, retain);
        }
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos,
            boolean retain) {
        for (CloudClientListener listener : this.listeners) {
            listener.onControlMessageArrived(deviceId, appTopic, payload, qos, retain);
        }
    }

    @Override
    public void onMessageConfirmed(int pubId, String appTopic) {
        for (CloudClientListener listener : this.listeners) {
            listener.onMessageConfirmed(pubId, appTopic);
        }
    }

    @Override
    public void onMessagePublished(int pubId, String appTopic) {
        for (CloudClientListener listener : this.listeners) {
            listener.onMessagePublished(pubId, appTopic);
        }
    }

    @Override
    public void onConnectionEstablished() {
        for (CloudClientListener listener : this.listeners) {
            listener.onConnectionEstablished();
        }
    }

    @Override
    public void onConnectionLost() {
        for (CloudClientListener listener : this.listeners) {
            listener.onConnectionLost();
        }
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private String encodeTopic(String deviceId, String appTopic, boolean isControl) {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        StringBuilder sb = new StringBuilder();
        if (isControl) {
            sb.append(options.getTopicControlPrefix()).append(options.getTopicSeparator());
        }

        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(deviceId)
                .append(options.getTopicSeparator()).append(this.applicationId);

        if (appTopic != null && !appTopic.isEmpty()) {
            sb.append(options.getTopicSeparator()).append(appTopic);
        }

        return sb.toString();
    }

    private String getAppTopicRegex() {
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        StringBuilder sb = new StringBuilder();

        // String regexExample = "^(\\$EDC/)?eurotech/.+/conf-v1(/.+)?";

        // Optional control prefix
        sb.append("^(")
                // .append(options.getTopicControlPrefix())
                .append("\\$EDC").append(options.getTopicSeparator()).append(")?")

                .append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(".+") // Any device
                                                                                                         // ID
                .append(options.getTopicSeparator()).append(this.applicationId).append("(/.+)?");

        return sb.toString();
    }
}
