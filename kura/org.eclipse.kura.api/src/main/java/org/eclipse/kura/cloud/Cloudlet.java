/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.cloud;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

/**
 * Cloudlet is an abstract class that can be extended by services that wants to implement remote resource management.
 * The Cloudlet abstracts the detailed of the communication with the remote clients providing easy to use template
 * methods to be implemented by subclasses to handle CRUD operations on local resources.
 * <ul>
 * <li>{@link Cloudlet#doGet} is used to implement a READ request for a resource identified by the supplied
 * {@link CloudletTopic#getResources()}
 * <li>{@link Cloudlet#doPut} is used to implement a CREATE or UPDATE request for a resource identified by the supplied
 * {@link CloudletTopic#getResources()}
 * <li>{@link Cloudlet#doDel} is used to implement a DELETE request for a resource identified by the supplied
 * {@link CloudletTopic#getResources()}
 * <li>{@link Cloudlet#doPost} is used to implement other operations on a resource identified by the supplied
 * {@link CloudletTopic#getResources()}
 * <li>{@link Cloudlet#doExec} is used to perform applicatioon operation not necessary tied to a given resource.
 * </ul>
 *
 * @deprecated Please consider using {@link org.eclipse.kura.cloudconnection.request.RequestHandler}
 */
@ConsumerType
@Deprecated
public abstract class Cloudlet implements CloudClientListener {

    private static final Logger logger = LogManager.getLogger(Cloudlet.class);

    protected static final int DFLT_PUB_QOS = 0;
    protected static final boolean DFLT_RETAIN = false;
    protected static final int DFLT_PRIORITY = 1;

    private static final int NUM_CONCURRENT_CALLBACKS = 2;
    private static ExecutorService callbackExecutor = Executors.newFixedThreadPool(NUM_CONCURRENT_CALLBACKS);

    private CloudService cloudService;
    private CloudClient cloudClient;

    private ComponentContext ctx;

    private final String applicationId;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        // get the mqtt client for this application
        try {

            logger.info("Getting CloudApplicationClient for {}...", this.applicationId);
            this.cloudClient = this.cloudService.newCloudClient(this.applicationId);
            this.cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default subscriptions and we don't want to get messages
            // twice
            this.ctx = componentContext;
        } catch (KuraException e) {
            logger.error("Cannot activate", e);
            throw new ComponentException(e);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        // close the application client.
        // this will unsubscribe all open subscriptions
        logger.info("Releasing CloudApplicationClient for {}...", this.applicationId);
        if (this.cloudClient != null) {
            this.cloudClient.release();
        }
    }

    protected Cloudlet(String appId) {
        this.applicationId = appId;
    }

    public String getAppId() {
        return this.applicationId;
    }

    protected CloudService getCloudService() {
        return this.cloudService;
    }

    protected CloudClient getCloudApplicationClient() {
        return this.cloudClient;
    }

    protected ComponentContext getComponentContext() {
        return this.ctx;
    }

    // ----------------------------------------------------------------
    //
    // Default handlers
    //
    // ----------------------------------------------------------------

    protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        logger.info("Default GET handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doPut(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        logger.info("Default PUT handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doPost(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        logger.info("Default POST handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doDel(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        logger.info("Default DEL handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        logger.info("Default EXEC handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        try {

            logger.debug("Control Arrived on topic: {}", appTopic);

            StringBuilder sb = new StringBuilder(this.applicationId).append("/").append("REPLY");

            if (appTopic.startsWith(sb.toString())) {
                // Ignore replies
                return;
            }

            // Handle the message asynchronously to not block the master client
            callbackExecutor.submit(new MessageHandlerCallable(this, deviceId, appTopic, msg, qos, retain));
        } catch (Throwable t) {
            logger.error("Unexpected throwable: {}", t);
        }
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.error("Unexpected message arrived on topic: " + appTopic);
    }

    @Override
    public void onConnectionLost() {
        logger.warn("Cloud Client Connection Lost!");
    }

    @Override
    public void onConnectionEstablished() {
        logger.info("Cloud Client Connection Restored");
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        logger.debug("Message Confirmed (" + messageId + ")");
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        logger.debug("Message Published (" + messageId + ")");
    }
}

class MessageHandlerCallable implements Callable<Void> {

    private static final Logger logger = LogManager.getLogger(MessageHandlerCallable.class);

    private final Cloudlet cloudApp;
    @SuppressWarnings("unused")
    private final String deviceId;
    private final String appTopic;
    private final KuraPayload msg;
    @SuppressWarnings("unused")
    private final int qos;
    @SuppressWarnings("unused")
    private final boolean retain;

    public MessageHandlerCallable(Cloudlet cloudApp, String deviceId, String appTopic, KuraPayload msg, int qos,
            boolean retain) {
        super();
        this.cloudApp = cloudApp;
        this.deviceId = deviceId;
        this.appTopic = appTopic;
        this.msg = msg;
        this.qos = qos;
        this.retain = retain;
    }

    @Override
    public Void call() throws Exception {
        logger.debug("Control Arrived on topic: {}", this.appTopic);

        // Prepare the default response
        KuraRequestPayload reqPayload = KuraRequestPayload.buildFromKuraPayload(this.msg);
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        try {

            CloudletTopic reqTopic = CloudletTopic.parseAppTopic(this.appTopic);
            CloudletTopic.Method method = reqTopic.getMethod();
            switch (method) {
            case GET:
                logger.debug("Handling GET request topic: {}", this.appTopic);
                this.cloudApp.doGet(reqTopic, reqPayload, respPayload);
                break;

            case PUT:
                logger.debug("Handling PUT request topic: {}", this.appTopic);
                this.cloudApp.doPut(reqTopic, reqPayload, respPayload);
                break;

            case POST:
                logger.debug("Handling POST request topic: {}", this.appTopic);
                this.cloudApp.doPost(reqTopic, reqPayload, respPayload);
                break;

            case DEL:
                logger.debug("Handling DEL request topic: {}", this.appTopic);
                this.cloudApp.doDel(reqTopic, reqPayload, respPayload);
                break;

            case EXEC:
                logger.debug("Handling EXEC request topic: {}", this.appTopic);
                this.cloudApp.doExec(reqTopic, reqPayload, respPayload);
                break;

            default:
                logger.error("Bad request topic: {}", this.appTopic);
                respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
                break;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Bad request topic: {}", this.appTopic);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
        } catch (KuraException e) {
            logger.error("Error handling request topic: {}\n{}", this.appTopic, e);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            respPayload.setException(e);
        }

        try {

            CloudClient cloudClient = this.cloudApp.getCloudApplicationClient();
            respPayload.setTimestamp(new Date());

            StringBuilder sb = new StringBuilder("REPLY").append("/").append(reqPayload.getRequestId());

            String requesterClientId = reqPayload.getRequesterClientId();

            logger.debug("Publishing response topic: {}", sb.toString());
            cloudClient.controlPublish(requesterClientId, sb.toString(), respPayload, Cloudlet.DFLT_PUB_QOS,
                    Cloudlet.DFLT_RETAIN, Cloudlet.DFLT_PRIORITY);
        } catch (KuraException e) {
            logger.error("Error publishing response for topic: {}\n{}", this.appTopic, e);
        }

        return null;
    }

}
