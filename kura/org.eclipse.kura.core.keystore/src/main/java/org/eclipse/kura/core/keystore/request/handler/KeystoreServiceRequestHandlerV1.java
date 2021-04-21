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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.keystore.request.handler;

import static java.util.Objects.isNull;
import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.KeystoreRemoteService;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class KeystoreServiceRequestHandlerV1 extends KeystoreRemoteService implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreServiceRequestHandlerV1.class);
    public static final String APP_ID = "KEYS-V1";

    private static final String NONE_RESOURCE_FOUND_MESSAGE = "Resource not found";
    private static final String KEYSTORES = "keystores";
    private static final String ENTRIES = "entries";
    private static final String CSR = "csr";

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
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

    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------

    @Override
    public KuraMessage doGet(final RequestHandlerContext context, final KuraMessage reqMessage) throws KuraException {
        final List<String> resourcePath = extractResourcePath(reqMessage);
        KuraPayload reqPayload = reqMessage.getPayload();

        if (resourcePath.isEmpty()) {
            logger.error(NONE_RESOURCE_FOUND_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (resourcePath.size() == 1 && resourcePath.get(0).equals(KEYSTORES)) {
            return jsonResponse(listKeystoresInternal());
        } else if (resourcePath.size() == 2 && resourcePath.get(0).equals(KEYSTORES)
                && resourcePath.get(1).equals(ENTRIES)) {
            return doGetEntries(reqPayload);
        } else if (resourcePath.size() == 3 && resourcePath.get(0).equals(KEYSTORES)
                && resourcePath.get(1).equals(ENTRIES) && resourcePath.get(2).equals(CSR)) {
            return doGetCsr(reqPayload);
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    private KuraMessage doGetCsr(KuraPayload reqPayload) throws KuraException {
        String body = new String(reqPayload.getBody(), StandardCharsets.UTF_8);
        EntryInfo request = unmarshal(body, EntryInfo.class);
        if (request != null) {
            return jsonResponse(getCSRInternal(request));
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    private KuraMessage doGetEntries(KuraPayload reqPayload) {
        byte[] body = reqPayload.getBody();
        if (isNull(body) || body.length == 0) {
            return jsonResponse(getKeysInternal());
        } else {
            EntryInfo request = unmarshal(new String(reqPayload.getBody(), StandardCharsets.UTF_8), EntryInfo.class);
            String keystoreServicePid = request.getKeystoreServicePid();
            String keyAlias = request.getAlias();
            if (!isNull(keystoreServicePid) && !isNull(keyAlias)) {
                return jsonResponse(getKeyInternal(keystoreServicePid, keyAlias));
            } else if (isNull(keystoreServicePid) && !isNull(keyAlias)) {
                return jsonResponse(getKeysByAliasInternal(keyAlias));
            } else {
                return jsonResponse(getKeysByPidInternal(keystoreServicePid));
            }
        }
    }

    @Override
    public KuraMessage doPut(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        final List<String> resourcePath = extractResourcePath(reqMessage);
        KuraPayload reqPayload = reqMessage.getPayload();

        if (resourcePath.size() != 2 || reqPayload.getBody() == null || reqPayload.getBody().length == 0
                || !resourcePath.get(0).equals(KEYSTORES)) {
            logger.error(NONE_RESOURCE_FOUND_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (resourcePath.get(1).equals(ENTRIES)) {
            String body = new String(reqPayload.getBody(), StandardCharsets.UTF_8);
            EntryInfo request = unmarshal(body, EntryInfo.class);
            if (request != null) {
                storeKeyEntryInternal(request);
                return new KuraMessage(new KuraResponsePayload(200));
            } else {
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public KuraMessage doDel(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        final List<String> resourcePath = extractResourcePath(reqMessage);
        KuraPayload reqPayload = reqMessage.getPayload();

        if (resourcePath.size() != 2 || reqPayload.getBody() == null || reqPayload.getBody().length == 0
                || !resourcePath.get(0).equals(KEYSTORES)) {
            logger.error(NONE_RESOURCE_FOUND_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (resourcePath.get(1).equals(ENTRIES)) {
            String body = new String(reqPayload.getBody(), StandardCharsets.UTF_8);
            EntryInfo request = unmarshal(body, EntryInfo.class);
            if (request != null) {
                deleteKeyEntryInternal(request.getKeystoreServicePid(), request.getAlias());
                return new KuraMessage(new KuraResponsePayload(200));
            } else {
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
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
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        responsePayload.setBody(gson.toJson(response).getBytes(StandardCharsets.UTF_8));
        return new KuraMessage(responsePayload);
    }

    private ServiceReference<Unmarshaller>[] getJsonUnmarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.json.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, Unmarshaller.class, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
    }

    protected <T> T unmarshal(String jsonString, Class<T> clazz) {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getJsonUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = this.bundleContext.getService(unmarshallerSR);
                result = unmarshaller.unmarshal(jsonString, clazz);
                if (result != null) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to marshal configuration.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }
        return result;
    }
}
