/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.keystore.provider;

import static org.eclipse.kura.rest.utils.Validable.validate;

import org.eclipse.kura.internal.rest.keystore.request.PrivateKeyWriteRequest;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

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
