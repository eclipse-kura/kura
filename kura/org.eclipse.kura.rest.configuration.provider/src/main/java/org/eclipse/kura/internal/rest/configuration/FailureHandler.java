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
package org.eclipse.kura.internal.rest.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.rest.configuration.api.Failure;
import org.eclipse.kura.rest.configuration.api.SubtaskFailure;
import org.eclipse.kura.rest.configuration.api.SubtaskFailureList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(FailureHandler.class);

    private final List<SubtaskFailure> failures = new ArrayList<>();

    public void runFallibleSubtask(final String id, final FallibleTask fallibleTask) {
        try {
            fallibleTask.run();
        } catch (final Exception e) {
            logger.warn("task failed {}", id, e);
            failures.add(
                    new SubtaskFailure(id, e.getMessage() != null ? e.getMessage() : "An internal error occurred"));
        }
    }

    public void checkStatus() {
        if (!failures.isEmpty()) {
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON).entity(new SubtaskFailureList(failures)).build());
        }
    }

    @FunctionalInterface
    public interface FallibleTask {

        public void run() throws KuraException;
    }

    public static WebApplicationException toWebApplicationException(final Exception e) {

        final String message = e.getMessage();

        if (message == null) {
            return new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
        } else {
            final Response response = Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                    .entity(new Failure(message)).build();
            return new WebApplicationException(response);
        }
    }

    public static WebApplicationException toWebApplicationException(final Status status, final String message) {

        if (message == null) {
            return new WebApplicationException(status);
        } else {
            final Response response = Response.status(status).type(MediaType.APPLICATION_JSON)
                    .entity(new Failure(message)).build();
            return new WebApplicationException(response);
        }
    }

    public static WebApplicationException parameterRequired(final String parameter) {
        final Response response = Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity(new Failure("parameter \"" + parameter + "\" is required")).build();
        return new WebApplicationException(response);
    }

    public static void requireParameter(final Object value, final String name) {
        if (value == null) {
            throw parameterRequired(name);
        }
    }
}
