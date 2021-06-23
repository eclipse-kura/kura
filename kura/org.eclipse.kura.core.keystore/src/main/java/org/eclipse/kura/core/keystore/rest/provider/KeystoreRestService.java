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

import static java.util.Objects.isNull;
import static org.eclipse.kura.rest.utils.Validable.validate;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.KeystoreRemoteService;
import org.eclipse.kura.security.keystore.KeystoreInfo;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

@Path("/keystores/v1")
public class KeystoreRestService extends KeystoreRemoteService {

    private static final String BAD_REQUEST_MESSAGE = "Bad request, ";

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = BAD_REQUEST_MESSAGE
            + "expected request format: {\"keystoreServicePid\": \"MyKeystoreName\", \"alias\": "
            + "\"MyAlias\", \"type\": \"TrustedCertificate\", \"certificate\": \"...\"}";
    private static final String BAD_GET_CSR_REQUEST_ERROR_MESSAGE = BAD_REQUEST_MESSAGE
            + "expected request format: {\"keystoreServicePid\": \"MyKeystoreName\", \"alias\": \"MyAlias\", "
            + "\"signatureAlgorithm\": \"...\", \"attributes\": \"...\"}";
    private static final String BAD_DELETE_REQUEST_ERROR_MESSAGE = BAD_REQUEST_MESSAGE
            + "expected request format: {\"keystoreServicePid\": \"MyKeystoreName\", \"alias\": \"MyAlias\"}";

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
    @Path("/entries")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EntryInfo> getEntries(@QueryParam("keystoreServicePid") String keystoreServicePid,
            @QueryParam("alias") String alias) {
        if (isNull(keystoreServicePid) && isNull(alias)) {
            return getKeysInternal();
        } else if (!isNull(keystoreServicePid)) {
            return getKeysByPidInternal(keystoreServicePid);
        } else {
            return getKeysByAliasInternal(alias);
        }
    }

    @GET
    @Path("/entries/entry")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    public EntryInfo getEntry(@QueryParam("keystoreServicePid") String keystoreServicePid,
            @QueryParam("alias") String alias) {
        return getKeyInternal(keystoreServicePid, alias);
    }

    @POST
    @Path("/entries/csr")
    @RolesAllowed("keystores")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CsrResponse getCSR(CsrReadRequest csrReadRequest) {
        validate(csrReadRequest, BAD_GET_CSR_REQUEST_ERROR_MESSAGE);
        return new CsrResponse(getCSRInternal(csrReadRequest));
    }

    @POST
    @Path("/entries/certificate")
    @RolesAllowed("keystores")
    @Consumes(MediaType.APPLICATION_JSON)
    public void storeTrustedCertificateEntry(TrustedCertificateWriteRequest writeRequest) {
        validate(writeRequest, BAD_WRITE_REQUEST_ERROR_MESSAGE);
        storeTrustedCertificateEntryInternal(writeRequest);
    }

    @POST
    @Path("/entries/keypair")
    @RolesAllowed("keystores")
    @Consumes(MediaType.APPLICATION_JSON)
    public void storeKeypairEntry(KeyPairWriteRequest writeRequest) {
        validate(writeRequest, BAD_WRITE_REQUEST_ERROR_MESSAGE);
        storeKeyPairEntryInternal(writeRequest);
    }

    @DELETE
    @Path("/entries")
    @RolesAllowed("keystores")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteKeyEntry(DeleteRequest deleteRequest) {
        validate(deleteRequest, BAD_DELETE_REQUEST_ERROR_MESSAGE);
        deleteKeyEntryInternal(deleteRequest.getKeystoreServicePid(), deleteRequest.getAlias());
    }

}
