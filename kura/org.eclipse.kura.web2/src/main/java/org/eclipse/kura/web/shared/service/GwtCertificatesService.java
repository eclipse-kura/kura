/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCertificate;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("certificate")
public interface GwtCertificatesService extends RemoteService {

    public Integer storeSSLPublicPrivateKeys(GwtXSRFToken xsrfToken, String privateCert, String publicCert,
            String password, String alias) throws GwtKuraException;

    public Integer storeSSLPublicChain(GwtXSRFToken xsrfToken, String publicCert, String alias) throws GwtKuraException;

    public Integer storeApplicationPublicChain(GwtXSRFToken xsrfToken, String publicCert, String alias)
            throws GwtKuraException;

    public Integer storeLoginPublicChain(GwtXSRFToken xsrfToken, String publicCert) throws GwtKuraException;

    public Integer storeLoginPublicPrivateKeys(GwtXSRFToken xsrfToken, String privateKey, String publicCert,
            String password, String alias) throws GwtKuraException;

    public List<GwtCertificate> listCertificates() throws GwtKuraException;

    public void removeCertificate(GwtXSRFToken xsrfToken, GwtCertificate certificate) throws GwtKuraException;
}