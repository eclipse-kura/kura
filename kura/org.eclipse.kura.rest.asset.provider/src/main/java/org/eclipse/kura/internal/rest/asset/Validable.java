/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.asset;

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
