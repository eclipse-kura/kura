/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtCertificate;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RequiredPermissions(KuraPermission.ADMIN)
@RemoteServiceRelativePath("certificate")
public interface GwtCertificatesService extends RemoteService {

    @Audit(componentName = "UI Certificate", description = "Store SSL key")
    public Integer storeSSLPublicPrivateKeys(GwtXSRFToken xsrfToken, String privateCert, String publicCert,
            String password, String alias) throws GwtKuraException;

    @Audit(componentName = "UI Certificate", description = "Store public chain")
    public Integer storeSSLPublicChain(GwtXSRFToken xsrfToken, String publicCert, String alias) throws GwtKuraException;

    @Audit(componentName = "UI Certificate", description = "Store application public chain")
    public Integer storeApplicationPublicChain(GwtXSRFToken xsrfToken, String publicCert, String alias)
            throws GwtKuraException;

    @Audit(componentName = "UI Certificate", description = "Store login public chain")
    public Integer storeLoginPublicChain(GwtXSRFToken xsrfToken, String publicCert) throws GwtKuraException;

    @Audit(componentName = "UI Certificate", description = "Store UI HTTPS key")
    public Integer storeLoginPublicPrivateKeys(GwtXSRFToken xsrfToken, String privateKey, String publicCert,
            String password, String alias) throws GwtKuraException;

    @Audit(componentName = "UI Certificate", description = "List stored certificates")
    public List<GwtCertificate> listCertificates() throws GwtKuraException;

    @Audit(componentName = "UI Certificate", description = "Remove certificate")
    public void removeCertificate(GwtXSRFToken xsrfToken, GwtCertificate certificate) throws GwtKuraException;
}