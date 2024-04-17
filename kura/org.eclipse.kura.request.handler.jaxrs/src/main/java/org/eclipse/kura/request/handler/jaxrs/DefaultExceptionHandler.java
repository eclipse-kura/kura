/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class DefaultExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    private DefaultExceptionHandler() {
    }

    public static WebApplicationException toWebApplicationException(final Throwable e) {
        if (e instanceof KuraException) {
            return toWebApplicationException((KuraException) e);
        } else if (e instanceof WebApplicationException) {
            return (WebApplicationException) e;
        } else if (e instanceof InvocationTargetException && e.getCause() != null) {
            return toWebApplicationException(e.getCause());
        } else {
            return buildWebApplicationException(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public static WebApplicationException toWebApplicationException(final KuraException e) {
        if (e.getCode() == KuraErrorCode.NOT_FOUND) {
            return buildWebApplicationException(Status.NOT_FOUND, e.getMessage());
        } else if (e.getCode() == KuraErrorCode.BAD_REQUEST || e.getCode() == KuraErrorCode.CONFIGURATION_ERROR) {
            return buildWebApplicationException(Status.BAD_REQUEST, e.getMessage());
        } else {
            return buildWebApplicationException(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public static KuraMessage toKuraMessage(final WebApplicationException e, final Optional<Gson> gson) {
        final Response response = e.getResponse();

        final KuraPayload responsePayload = new KuraResponsePayload(response.getStatus());

        try {
            ResponseBodyHandlers.responseHandler(gson.orElseGet(Gson::new)).buildBody(response)
                    .ifPresent(responsePayload::setBody);
        } catch (final Exception ex) {
            logger.warn("failed to serialize WebApplicationException entity", ex);
        }

        return new KuraMessage(responsePayload);
    }

    public static WebApplicationException buildWebApplicationException(final Status status, final String message) {

        final String actualMessage = message != null ? message : "An internal error occurred";

        return new WebApplicationException(
                Response.status(status).type(MediaType.APPLICATION_JSON).entity(new Failure(actualMessage)).build());
    }

    private static class Failure {

        private final String message;

        public Failure(String message) {
            this.message = message;
        }

        @SuppressWarnings("unused")
        public String getMessage() {
            return this.message;
        }
    }
}
