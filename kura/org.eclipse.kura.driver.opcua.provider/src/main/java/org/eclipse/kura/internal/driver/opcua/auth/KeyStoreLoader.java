/**
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
 */
package org.eclipse.kura.internal.driver.opcua.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.milo.opcua.stack.client.security.ClientCertificateValidator;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;

public class KeyStoreLoader {

    private static final Set<ValidationCheck> OPTIONAL_CHECKS = EnumSet.of(ValidationCheck.APPLICATION_URI,
            ValidationCheck.VALIDITY);

    private final Optional<PrivateKeyEntry> privateKeyEntry;
    private final ClientCertificateValidator certificateValidator;

    public KeyStoreLoader(final String keyStorePath, final String keyStoreType,
            final char[] keyStorePassword, final String clientAlias, final boolean authenticateServer)
            throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, CertificateException,
            IOException {

        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(Files.newInputStream(Paths.get(keyStorePath)), keyStorePassword);

        final Entry entry = keyStore.getEntry(clientAlias, new PasswordProtection(keyStorePassword));

        if (entry instanceof PrivateKeyEntry) {
            this.privateKeyEntry = Optional.of((PrivateKeyEntry) entry);
        } else {
            this.privateKeyEntry = Optional.empty();
        }

        if (!authenticateServer) {
            this.certificateValidator = new ClientCertificateValidator() {

                @Override
                public void validateCertificateChain(List<X509Certificate> arg0) throws UaException {
                    // accept all
                }

                @Override
                public void validateCertificateChain(List<X509Certificate> arg0, String arg1, String... arg2)
                        throws UaException {
                    // accept all
                }
            };
        } else {
            this.certificateValidator = new DefaultClientCertificateValidator(new TrustListManagerImpl(keyStore),
                    OPTIONAL_CHECKS);
        }
    }

    public Optional<PrivateKeyEntry> getPrivateKeyEntry() {
        return privateKeyEntry;
    }

    public ClientCertificateValidator getCertificateValidator() {
        return certificateValidator;
    }

}