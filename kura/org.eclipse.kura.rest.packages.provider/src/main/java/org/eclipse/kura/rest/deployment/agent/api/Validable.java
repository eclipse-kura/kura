/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package com.eurotech.framework.rest.deployment.agent.api;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public interface Validable {

    public boolean isValid();

    public static boolean isValid(Validable validable) {
        if (validable == null) {
            return false;
        }
        return validable.isValid();
    }

    public static void validate(Validable validable, String exceptionMessage) {
        if (!isValid(validable)) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(exceptionMessage).type(MediaType.TEXT_PLAIN).build());
        }
    }
}
