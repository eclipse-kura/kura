/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerContextConstants.NOTIFICATION_PUBLISHER_PID;
import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageType;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandlerCallable implements Callable<Void> {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");
    private static final Logger logger = LoggerFactory.getLogger(MessageHandlerCallable.class);

    private static final Pattern RESOURCES_DELIM = Pattern.compile("/");

    public static final String METRIC_REQUEST_ID = "request.id";
    public static final String REQUESTER_CLIENT_ID = "requester.client.id";

    public static final String METRIC_RESPONSE_CODE = "response.code";
    public static final String METRIC_EXCEPTION_MSG = "response.exception.message";
    public static final String METRIC_EXCEPTION_STACK = "response.exception.stack";

    public static final int RESPONSE_CODE_OK = 200;
    public static final int RESPONSE_CODE_BAD_REQUEST = 400;
    public static final int RESPONSE_CODE_NOTFOUND = 404;
    public static final int RESPONSE_CODE_ERROR = 500;

    protected static final int DFLT_PUB_QOS = 0;
    protected static final boolean DFLT_RETAIN = false;
    protected static final int DFLT_PRIORITY = 1;

    private final RequestHandler cloudApp;
    private final String appTopic;
    private final KuraPayload kuraMessage;
    private final CloudConnectionManagerImpl cloudConnectionManager;
    private final RequestHandlerContext requestHandlerContext;

    public MessageHandlerCallable(RequestHandler cloudApp, String appTopic, KuraPayload msg,
            CloudConnectionManagerImpl cloudConnectionManager) {
        super();
        this.cloudApp = cloudApp;
        this.appTopic = appTopic;
        this.kuraMessage = msg;
        this.cloudConnectionManager = cloudConnectionManager;

        String notificationPublisherPid = null; // TODO: this.cloudConnectionManager.getNotificationPublisherPid();
        CloudNotificationPublisher notificationPublisher = null; // TODO:
                                                                 // this.cloudConnectionManager.getNotificationPublisher();
        Map<String, String> contextProperties = new HashMap<>();
        contextProperties.put(NOTIFICATION_PUBLISHER_PID.name(), notificationPublisherPid);

        this.requestHandlerContext = new RequestHandlerContext(notificationPublisher, contextProperties);
    }

    @Override
    public Void call() throws Exception {
        logger.debug("Control Arrived on topic: {}", this.appTopic);

        String requestId = (String) this.kuraMessage.getMetric(METRIC_REQUEST_ID);
        if (requestId == null) {
        if(logger.isDebugEnabled()) {
            logger.debug("Request Id is null"); 
        }
            throw new ParseException("Not a valid request payload", 0);
        }

        // Prepare the default response
        KuraPayload reqPayload = this.kuraMessage;
        KuraMessage response;

        final Map<String, String> auditProperties = new HashMap<>();

        auditProperties.put(AuditConstants.KEY_ENTRY_POINT.getValue(), "EclipseIoTCloudConnectionService");
        auditProperties.put("cloud.app.topic", appTopic);
        auditProperties.put("cloud.connection.pid", cloudConnectionManager.getOwnPid());

        try (final Scope scope = AuditContext.openScope(new AuditContext(auditProperties))) {
            try {
                Iterator<String> resources = RESOURCES_DELIM.splitAsStream(this.appTopic).iterator();

                if (!resources.hasNext()) {
                    throw new IllegalArgumentException();
                }

                String method = resources.next();

                Map<String, Object> reqResources = getMessageResources(resources);

                KuraMessage reqMessage = new KuraMessage(reqPayload, reqResources);

                switch (method) {
                case "GET":
                    logger.debug("Handling GET request topic: {}", this.appTopic);
                    response = this.cloudApp.doGet(this.requestHandlerContext, reqMessage);
                    break;

                case "PUT":
                    logger.debug("Handling PUT request topic: {}", this.appTopic);
                    response = this.cloudApp.doPut(this.requestHandlerContext, reqMessage);
                    break;

                case "POST":
                    logger.debug("Handling POST request topic: {}", this.appTopic);
                    response = this.cloudApp.doPost(this.requestHandlerContext, reqMessage);
                    break;

                case "DEL":
                    logger.debug("Handling DEL request topic: {}", this.appTopic);
                    response = this.cloudApp.doDel(this.requestHandlerContext, reqMessage);
                    break;

                case "EXEC":
                    logger.debug("Handling EXEC request topic: {}", this.appTopic);
                    response = this.cloudApp.doExec(this.requestHandlerContext, reqMessage);
                    break;

                default:
                    logger.error("Bad request topic: {}", this.appTopic);
                    KuraPayload payload = new KuraPayload();
                    response = setResponseCode(payload, RESPONSE_CODE_BAD_REQUEST);
                    break;
                }
            } catch (IllegalArgumentException e) {
                logger.error("Bad request topic: {}", this.appTopic);
                KuraPayload payload = new KuraPayload();
                response = setResponseCode(payload, RESPONSE_CODE_BAD_REQUEST);
            } catch (KuraException e) {
                logger.error("Error handling request topic: {}", this.appTopic, e);
                response = manageException(e);
            }

            final Object responseCode = response.getPayload().getMetric(METRIC_RESPONSE_CODE);
            final boolean isSuccessful = responseCode instanceof Integer && ((Integer) responseCode) / 200 == 1;

            if (isSuccessful) {
                auditLogger.info("{} CloudCall - Success - Execute RequestHandler call", AuditContext.currentOrInternal());
            } else {
                auditLogger.warn("{} CloudCall - Failure - Execute RequestHandler call", AuditContext.currentOrInternal());
            }

            buildResponseMessage(requestId, response);
        }
        return null;
    }

    private void buildResponseMessage(String requestId, KuraMessage response) {
        try {
            response.getPayload().setTimestamp(new Date());

            DataService dataService = this.cloudConnectionManager.getDataService();
            String fullTopic = encodeTopic(requestId,
                    String.valueOf(response.getPayload().getMetric(METRIC_RESPONSE_CODE)));
            byte[] appPayload = this.cloudConnectionManager.encodePayload(response.getPayload());
            dataService.publish(fullTopic, appPayload, DFLT_PUB_QOS, DFLT_RETAIN, DFLT_PRIORITY);
        } catch (KuraException e) {
            logger.error("Error publishing response for topic: {}\n{}", this.appTopic, e);
        }
    }

    private KuraMessage manageException(KuraException e) {
        KuraMessage message;
        KuraPayload payload = new KuraPayload();
        setException(payload, e);
        if (e.getCode().equals(KuraErrorCode.BAD_REQUEST)) {
            message = setResponseCode(payload, RESPONSE_CODE_BAD_REQUEST);
        } else if (e.getCode().equals(KuraErrorCode.NOT_FOUND)) {
            message = setResponseCode(payload, RESPONSE_CODE_NOTFOUND);
        } else {
            message = setResponseCode(payload, RESPONSE_CODE_ERROR);
        }
        return message;
    }

    private Map<String, Object> getMessageResources(Iterator<String> iter) {
        List<String> resourcesList = new ArrayList<>();
        while (iter.hasNext()) {
            resourcesList.add(iter.next());
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(ARGS_KEY.value(), resourcesList);
        return properties;
    }

    public KuraMessage setResponseCode(KuraPayload payload, int responseCode) {
        payload.addMetric(METRIC_RESPONSE_CODE, Integer.valueOf(responseCode));
        return new KuraMessage(payload);
    }

    public void setException(KuraPayload payload, Throwable t) {
        if (t != null) {
            payload.addMetric(METRIC_EXCEPTION_MSG, t.getMessage());
            payload.addMetric(METRIC_EXCEPTION_STACK, stackTraceAsString(t));
        }
    }

    private String stackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private String encodeTopic(String requestId, String responseCode) {
        CloudConnectionManagerOptions options = this.cloudConnectionManager.getCloudConnectionManagerOptions();
        String topicSeparator = options.getTopicSeparator();
        StringBuilder sb = new StringBuilder();

        // fixed response topic subsection
        sb.append(MessageType.CONTROL.getTopicPrefix()).append(topicSeparator).append(topicSeparator)
                .append(topicSeparator).append("res");

        // variable response topic part
        sb.append(topicSeparator).append(requestId).append(topicSeparator).append(responseCode);

        return sb.toString();
    }
}
