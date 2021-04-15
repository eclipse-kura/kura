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
package org.eclipse.kura.core.keystore.rest.provider;

import static org.eclipse.kura.rest.utils.Validable.validate;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.KeystoreServiceRemoteService;
import org.eclipse.kura.security.keystore.KeystoreInfo;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

@Path("/keystores")
public class KeystoreRestService extends KeystoreServiceRemoteService {

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = "Bad request, "
            + "expected request format: {\"keystoreName\": \"MyKeystoreName\", \"alias\": "
            + "\"MyAlias\", \"type\": \"TrustedCertificate\", \"certificate\": \"...\"}";
    private static final String BAD_DELETE_REQUEST_ERROR_MESSAGE = "Bad request, "
            + "expected request format: {\"keystoreName\": \"MyKeystoreName\", \"alias\": " + "\"MyAlias\"}";

    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole("kura.permission.rest.keystores", Role.GROUP);
    }

    @GET
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    public List<KeystoreInfo> listKeystores() {
        return listKeystoresInternal();
    }

    @GET
    @Path("/keys")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EntryInfo> getKeys() {
        return getKeysInternal();
    }

    @GET
    @Path("/id/{id}/keys")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EntryInfo> getKeys(@PathParam("id") final String id) {
        return getKeysInternal(id);
    }

    @GET
    @Path("/id/{id}/keys/{alias}")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    public EntryInfo getKey(@PathParam("id") final String id, @PathParam("alias") final String alias) {
        return getKeyInternal(id, alias);
    }

    @POST
    @Path("/keys/_store")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String storeKeyEntry(WriteRequest writeRequest) {
        validate(writeRequest, BAD_WRITE_REQUEST_ERROR_MESSAGE);
        return storeKeyEntryInternal(writeRequest);
    }

    @DELETE
    @Path("/keys/_delete")
    @RolesAllowed("keystores")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteKeyEntry(DeleteRequest deleteRequest) {
        validate(deleteRequest, BAD_DELETE_REQUEST_ERROR_MESSAGE);
        deleteKeyEntryInternal(deleteRequest.getKeystoreName(), deleteRequest.getAlias());
    }

}
