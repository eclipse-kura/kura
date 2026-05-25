/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.provider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

@Provider
public class RestExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException) {
            processWebApplicationException((WebApplicationException) exception);
        }

        if (exception instanceof JsonParseException) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                    .entity("{\"message\":\"Error parsing request body\"}").build();
        }

        return processDefaultException(exception);
    }

    private static Response processWebApplicationException(WebApplicationException exception) {
        return (exception.getResponse() == null) ? processDefaultException(exception) : exception.getResponse();
    }

    private static Response processDefaultException(Exception exception) {
        logger.error("Uncaught Exception while processing REST request", exception);

        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Something went wrong\"}").build();
    }

}
