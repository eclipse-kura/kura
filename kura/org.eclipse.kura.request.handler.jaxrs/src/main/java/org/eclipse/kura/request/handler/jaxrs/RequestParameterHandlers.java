/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.request.handler.jaxrs.consumer.RequestArgumentHandler;
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

    public static RequestParameterHandler fromArgumentHandlers(final List<RequestArgumentHandler<?>> handlers) {
        return m -> {
            final Object[] result = new Object[handlers.size()];

            for (int i = 0; i < handlers.size(); i++) {
                result[i] = handlers.get(i).buildParameter(m);
            }
            return result;
        };
    }

    public static <T> RequestArgumentHandler<T> nullArgumentHandler() {
        return m -> null;
    }

    public static RequestArgumentHandler<InputStream> inputStreamArgumentHandler() {
        return m -> {
            final KuraPayload payload = m.getPayload();
            final byte[] body = payload.getBody();

            if (body == null || body.length == 0) {
                return null;
            }

            return new ByteArrayInputStream(body);
        };
    }

    public static RequestParameterHandler inputStreamHandler() {
        return m -> {
            final RequestArgumentHandler<InputStream> handler = inputStreamArgumentHandler();

            return new Object[] { handler.buildParameter(m) };
        };
    }

    public static <T> RequestArgumentHandler<T> gsonArgumentHandler(final Class<T> type, final Gson gson) {
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

            final T result;

            try {
                result = gson.fromJson(asString, type);
            } catch (final Exception e) {
                logger.warn("malformed JSON request", e);
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }

            return result;
        };
    }

    public static RequestParameterHandler gsonHandler(final Class<?> type, final Gson gson) {

        return m -> {

            final RequestArgumentHandler<?> handler = gsonArgumentHandler(type, gson);

            return new Object[] { handler.buildParameter(m) };
        };
    }
}
