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
package org.eclipse.kura.core.keystore.rest.provider;

import static org.eclipse.kura.rest.utils.Validable.validate;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.core.keystore.request.PrivateKeyWriteRequest;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;

@Path("/keystores/v2")
public class KeystoreRestServiceV2 extends KeystoreRestService {

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = BAD_REQUEST_MESSAGE
            + "expected request format: {\"keystoreServicePid\": \"MyKeystoreName\", \"alias\": "
            + "\"MyAlias\", \"certificateChain\": \"...\", \"privateKey\": \"...\"}";

    @POST
    @Path("/entries/privatekey")
    @RolesAllowed("keystores")
    @Consumes(MediaType.APPLICATION_JSON)
    public void storeKeypairEntry(PrivateKeyWriteRequest writeRequest) {
        validate(writeRequest, BAD_WRITE_REQUEST_ERROR_MESSAGE);
        try {
            storePrivateKeyEntryInternal(writeRequest);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }
}
