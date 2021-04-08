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
package org.eclipse.kura.core.certificates;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.certificate.KuraCertificate;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

@Path("/certificates")
public class CertificatesRestService {

    private CertificatesService certificatesService;

    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole("kura.permission.rest.certificates", Role.GROUP);
    }

    public void setCertificatesService(CertificatesService certificatesService) {
        this.certificatesService = certificatesService;
    }

    public void unsetCertificatesService(CertificatesService certificatesService) {
        if (this.certificatesService == certificatesService) {
            this.certificatesService = null;
        }
    }

    @GET
    @Path("/list")
    @RolesAllowed("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<KuraCertificate> listCertificates() throws KuraException {
        return this.certificatesService.getCertificates();
    }
}
