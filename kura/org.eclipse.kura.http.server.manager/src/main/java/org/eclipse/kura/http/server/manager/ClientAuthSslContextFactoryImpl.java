/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.http.server.manager;

import java.security.KeyStore;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.X509CertSelector;
import java.util.Collection;
import java.util.Set;

import org.eclipse.kura.security.keystore.KeystoreService;

public final class ClientAuthSslContextFactoryImpl extends BaseSslContextFactory {

    private boolean isRevocationEnabled;
    private Set<Option> revocationOptions;

    public ClientAuthSslContextFactoryImpl(final KeystoreService keystoreService, final boolean isRevocationEnabled,
            final Set<PKIXRevocationChecker.Option> revocationOptions) {
        super(keystoreService);

        this.isRevocationEnabled = isRevocationEnabled;
        this.revocationOptions = revocationOptions;
        setValidatePeerCerts(isRevocationEnabled);
    }

    @Override
    protected PKIXBuilderParameters newPKIXBuilderParameters(KeyStore trustStore,
            Collection<? extends java.security.cert.CRL> crls) throws Exception {
        PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());

        pbParams.setMaxPathLength(getMaxCertPathLength());
        pbParams.setRevocationEnabled(this.isRevocationEnabled);

        final PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) CertPathValidator.getInstance("PKIX")
                .getRevocationChecker();

        revocationChecker.setOptions(this.revocationOptions);

        pbParams.addCertPathChecker(revocationChecker);

        if (getPkixCertPathChecker() != null) {
            pbParams.addCertPathChecker(getPkixCertPathChecker());
        }

        try {
            pbParams.addCertStore(this.keystoreService.getCRLStore());
        } catch (Exception e) {
            if (crls != null && !crls.isEmpty()) {
                pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
            }
        }

        return pbParams;
    }

}
