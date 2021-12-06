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

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.eclipse.kura.request.handler.jaxrs.consumer.ResponseBodyHandler;

import com.google.gson.Gson;

public final class ResponseBodyHandlers {

    private ResponseBodyHandlers() {
    }

    public static ResponseBodyHandler voidHandler() {
        return r -> Optional.empty();
    }

    public static ResponseBodyHandler responseHandler(final Gson gson) {
        return r -> {
            final Response response = (Response) r;

            if (response == null) {
                return Optional.empty();
            }

            final Object entity = response.getEntity();

            if (entity == null) {
                return Optional.empty();
            } else if (entity instanceof String) {
                return Optional.of(((String) entity).getBytes(StandardCharsets.UTF_8));
            } else if (entity instanceof byte[]) {
                return Optional.of((byte[]) entity);
            } else {
                final String asJson = gson.toJson(entity);
                return Optional.of(asJson.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    public static ResponseBodyHandler gsonHandler(final Gson gson) {
        return r -> {
            if (r == null) {
                return Optional.empty();
            }

            return Optional.of(gson.toJson(r).getBytes(StandardCharsets.UTF_8));
        };
    }
}
