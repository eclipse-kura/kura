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
package org.eclipse.kura.cloud;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
 * @deprecated Please consider using {@link RequestHandler}
 */
@ConsumerType
@Deprecated
public abstract class Cloudlet implements CloudClientListener {

    private static final Logger s_logger = LogManager.getLogger(Cloudlet.class);

    protected static final int DFLT_PUB_QOS = 0;
    protected static final boolean DFLT_RETAIN = false;
    protected static final int DFLT_PRIORITY = 1;

    private static int NUM_CONCURRENT_CALLBACKS = 2;
    private static ExecutorService m_callbackExecutor = Executors.newFixedThreadPool(NUM_CONCURRENT_CALLBACKS);

    private CloudService m_cloudService;
    private CloudClient m_cloudClient;

    private ComponentContext m_ctx;

    private final String m_applicationId;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        // get the mqtt client for this application
        try {

            s_logger.info("Getting CloudApplicationClient for {}...", this.m_applicationId);
            this.m_cloudClient = this.m_cloudService.newCloudClient(this.m_applicationId);
            this.m_cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default subscriptions and we don't want to get messages
            // twice
            this.m_ctx = componentContext;
        } catch (KuraException e) {
            s_logger.error("Cannot activate", e);
            throw new ComponentException(e);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        // close the application client.
        // this will unsubscribe all open subscriptions
        s_logger.info("Releasing CloudApplicationClient for {}...", this.m_applicationId);
        if (this.m_cloudClient != null) {
            this.m_cloudClient.release();
        }
    }

    protected Cloudlet(String appId) {
        this.m_applicationId = appId;
    }

    public String getAppId() {
        return this.m_applicationId;
    }

    protected CloudService getCloudService() {
        return this.m_cloudService;
    }

    protected CloudClient getCloudApplicationClient() {
        return this.m_cloudClient;
    }

    protected ComponentContext getComponentContext() {
        return this.m_ctx;
    }

    // ----------------------------------------------------------------
    //
    // Default handlers
    //
    // ----------------------------------------------------------------

    protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        s_logger.info("Default GET handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doPut(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        s_logger.info("Default PUT handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doPost(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        s_logger.info("Default POST handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doDel(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        s_logger.info("Default DEL handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        s_logger.info("Default EXEC handler");
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        try {

            s_logger.debug("Control Arrived on topic: {}", appTopic);

            StringBuilder sb = new StringBuilder(this.m_applicationId).append("/").append("REPLY");

            if (appTopic.startsWith(sb.toString())) {
                // Ignore replies
                return;
            }

            // Handle the message asynchronously to not block the master client
            m_callbackExecutor.submit(new MessageHandlerCallable(this, deviceId, appTopic, msg, qos, retain));
        } catch (Throwable t) {
            s_logger.error("Unexpected throwable: {}", t);
        }
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.error("Unexpected message arrived on topic: " + appTopic);
    }

    @Override
    public void onConnectionLost() {
        s_logger.warn("Cloud Client Connection Lost!");
    }

    @Override
    public void onConnectionEstablished() {
        s_logger.info("Cloud Client Connection Restored");
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        s_logger.debug("Message Confirmed (" + messageId + ")");
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        s_logger.debug("Message Published (" + messageId + ")");
    }
}

class MessageHandlerCallable implements Callable<Void> {

    private static final Logger s_logger = LogManager.getLogger(MessageHandlerCallable.class);

    private final Cloudlet m_cloudApp;
    @SuppressWarnings("unused")
    private final String m_deviceId;
    private final String m_appTopic;
    private final KuraPayload m_msg;
    @SuppressWarnings("unused")
    private final int m_qos;
    @SuppressWarnings("unused")
    private final boolean m_retain;

    public MessageHandlerCallable(Cloudlet cloudApp, String deviceId, String appTopic, KuraPayload msg, int qos,
            boolean retain) {
        super();
        this.m_cloudApp = cloudApp;
        this.m_deviceId = deviceId;
        this.m_appTopic = appTopic;
        this.m_msg = msg;
        this.m_qos = qos;
        this.m_retain = retain;
    }

    @Override
    public Void call() throws Exception {
        s_logger.debug("Control Arrived on topic: {}", this.m_appTopic);

        // Prepare the default response
        KuraRequestPayload reqPayload = KuraRequestPayload.buildFromKuraPayload(this.m_msg);
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        try {

            CloudletTopic reqTopic = CloudletTopic.parseAppTopic(this.m_appTopic);
            CloudletTopic.Method method = reqTopic.getMethod();
            switch (method) {
            case GET:
                s_logger.debug("Handling GET request topic: {}", this.m_appTopic);
                this.m_cloudApp.doGet(reqTopic, reqPayload, respPayload);
                break;

            case PUT:
                s_logger.debug("Handling PUT request topic: {}", this.m_appTopic);
                this.m_cloudApp.doPut(reqTopic, reqPayload, respPayload);
                break;

            case POST:
                s_logger.debug("Handling POST request topic: {}", this.m_appTopic);
                this.m_cloudApp.doPost(reqTopic, reqPayload, respPayload);
                break;

            case DEL:
                s_logger.debug("Handling DEL request topic: {}", this.m_appTopic);
                this.m_cloudApp.doDel(reqTopic, reqPayload, respPayload);
                break;

            case EXEC:
                s_logger.debug("Handling EXEC request topic: {}", this.m_appTopic);
                this.m_cloudApp.doExec(reqTopic, reqPayload, respPayload);
                break;

            default:
                s_logger.error("Bad request topic: {}", this.m_appTopic);
                respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
                break;
            }
        } catch (IllegalArgumentException e) {
            s_logger.error("Bad request topic: {}", this.m_appTopic);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
        } catch (KuraException e) {
            s_logger.error("Error handling request topic: {}\n{}", this.m_appTopic, e);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            respPayload.setException(e);
        }

        try {

            CloudClient cloudClient = this.m_cloudApp.getCloudApplicationClient();
            respPayload.setTimestamp(new Date());

            StringBuilder sb = new StringBuilder("REPLY").append("/").append(reqPayload.getRequestId());

            String requesterClientId = reqPayload.getRequesterClientId();

            s_logger.debug("Publishing response topic: {}", sb.toString());
            cloudClient.controlPublish(requesterClientId, sb.toString(), respPayload, Cloudlet.DFLT_PUB_QOS,
                    Cloudlet.DFLT_RETAIN, Cloudlet.DFLT_PRIORITY);
        } catch (KuraException e) {
            s_logger.error("Error publishing response for topic: {}\n{}", this.m_appTopic, e);
        }

        return null;
    }

}
