/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.deployment.agent.api;

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
