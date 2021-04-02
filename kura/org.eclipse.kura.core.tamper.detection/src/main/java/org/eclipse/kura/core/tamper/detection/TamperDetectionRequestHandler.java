/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.tamper.detection;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.core.tamper.detection.util.TamperDetectionRemoteService;
import org.eclipse.kura.message.KuraResponsePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class TamperDetectionRequestHandler extends TamperDetectionRemoteService implements RequestHandler {

    private static final String APP_ID = "TAMPER-V1";
    private static final KuraMessage EMPTY_RESPONSE = new KuraMessage(new KuraResponsePayload(200));

    private static final String LIST_PATH = "list";
    private static final String PID_PATH = "pid";

    private static final String RESET_PATH = "_reset";

    private static final Logger logger = LoggerFactory.getLogger(TamperDetectionRequestHandler.class);

    public void setRequestHandlerRegistry(final RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.registerRequestHandler(APP_ID, this);
        } catch (KuraException e) {
            logger.info("Unable to register cloudlet {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    public void unsetRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.info("Unable to register cloudlet {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    @Override
    public KuraMessage doGet(final RequestHandlerContext context, final KuraMessage reqMessage) throws KuraException {

        final List<String> resourcePath = extractResourcePath(reqMessage);

        if (resourcePath.size() == 1 && resourcePath.get(0).equals(LIST_PATH)) {
            return jsonResponse(listTamperDetectionServicesInternal());
        } else if (resourcePath.size() == 2 && resourcePath.get(0).equals(PID_PATH)) {
            return jsonResponse(getTamperStatusInternal(resourcePath.get(1)));
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public KuraMessage doExec(final RequestHandlerContext context, final KuraMessage reqMessage) throws KuraException {

        final List<String> resourcePath = extractResourcePath(reqMessage);

        if (resourcePath.size() != 3) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (!resourcePath.get(0).equals(PID_PATH) || !resourcePath.get(2).equals(RESET_PATH)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        resetTamperStatusInternal(resourcePath.get(1));

        return EMPTY_RESPONSE;
    }

    @SuppressWarnings("unchecked")
    private static final List<String> extractResourcePath(final KuraMessage message) throws KuraException {
        Object requestObject = message.getProperties().get(ARGS_KEY.value());
        if (requestObject instanceof List) {
            return (List<String>) requestObject;
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    private static final KuraMessage jsonResponse(final Object response) {
        final KuraResponsePayload responsePayload = new KuraResponsePayload(200);

        final Gson gson = new Gson();

        responsePayload.setBody(gson.toJson(response).getBytes(StandardCharsets.UTF_8));

        return new KuraMessage(responsePayload);
    }
}
