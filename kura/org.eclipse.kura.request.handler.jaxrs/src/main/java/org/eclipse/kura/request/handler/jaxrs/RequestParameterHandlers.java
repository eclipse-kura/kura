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
 *******************************************************************************/
package org.eclipse.kura.request.handler.jaxrs;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.request.handler.jaxrs.consumer.RequestParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public final class RequestParameterHandlers {

    private static final Logger logger = LoggerFactory.getLogger(RequestParameterHandlers.class);

    private static final Object[] EMPTY_PARAMETERS = new Object[0];

    private RequestParameterHandlers() {
    }

    public static RequestParameterHandler noArgsHandler() {
        return m -> EMPTY_PARAMETERS;
    }

    public static RequestParameterHandler inputStreamHandler() {
        return m -> {
            final KuraPayload payload = m.getPayload();
            final byte[] body = payload.getBody();

            if (body == null || body.length == 0) {
                return null;
            }

            return new Object[] { new ByteArrayInputStream(body) };
        };
    }

    public static RequestParameterHandler gsonHandler(final Class<?> type, final Gson gson) {

        return m -> {
            final KuraPayload payload = m.getPayload();
            final byte[] body = payload.getBody();

            if (body == null || body.length == 0) {
                return null;
            }

            final String asString;

            try {

                asString = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(body)).toString();
            } catch (final Exception e) {
                logger.warn("request body is not valid UTF8", e);
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }

            final Object result;

            try {
                result = gson.fromJson(asString, type);
            } catch (final Exception e) {
                logger.warn("malformed JSON request", e);
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }

            return new Object[] { result };
        };
    }
}
