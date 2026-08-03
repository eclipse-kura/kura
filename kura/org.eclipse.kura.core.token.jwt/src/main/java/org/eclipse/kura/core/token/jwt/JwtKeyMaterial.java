/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.token.jwt;

import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.keystore.KeystoreService;

class JwtKeyMaterial {

    record SigningKey(String alias, RSAPrivateKey rsaPrivateKey) {
    }

    private final Optional<SigningKey> signingKey;
    private final Map<String, X509Certificate> verificationCertificates;

    public JwtKeyMaterial(final Optional<String> signingKeyAlias, final Set<String> verificationKeyAliases,
            final KeystoreService keystoreService) throws KuraException {

        Set<String> allowedAliases = new HashSet<>(verificationKeyAliases);

        if (signingKeyAlias.isPresent()) {
            String alias = signingKeyAlias.get();
            Optional<RSAPrivateKey> key = findSigningRsaPrivateKey(keystoreService, alias);

            if (key.isPresent()) {
                this.signingKey = Optional.of(new SigningKey(alias, key.get()));

                if (!allowedAliases.isEmpty()) {
                    // When restricting the allowed ones, always add the signing key alias
                    allowedAliases.add(alias);
                }
            } else {
                this.signingKey = Optional.empty();
            }
        } else {
            this.signingKey = Optional.empty();
        }

        this.verificationCertificates = Collections
                .unmodifiableMap(collectVerificationCertificates(keystoreService, allowedAliases));
    }

    public Optional<SigningKey> getSigningKey() {
        return this.signingKey;
    }

    public Optional<X509Certificate> getVerificationCertificate(final String alias) {
        return Optional.ofNullable(this.verificationCertificates.get(alias));
    }

    public Map<String, X509Certificate> getVerificationCertificates() {
        return this.verificationCertificates;
    }

    private static Optional<RSAPrivateKey> findSigningRsaPrivateKey(final KeystoreService keystoreService,
            final String alias)
            throws KuraException {

        final Entry entry = keystoreService.getEntry(alias);

        if (entry == null) {
            return Optional.empty();
        }

        if (!(entry instanceof PrivateKeyEntry privateKeyEntry)) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Keystore entry '" + alias + "' is a "
                    + entry.getClass().getSimpleName() + ", a PrivateKeyEntry is required to sign tokens");
        }

        if (!(privateKeyEntry.getPrivateKey() instanceof RSAPrivateKey signingRsaPrivateKey)) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "Signing key '" + alias + "' is not a RSAPrivateKey");
        }

        return Optional.of(signingRsaPrivateKey);
    }

    private static Map<String, X509Certificate> collectVerificationCertificates(
            final KeystoreService keystoreService, final Set<String> allowedAliases) throws KuraException {

        final Map<String, X509Certificate> result = new LinkedHashMap<>();

        for (final Map.Entry<String, Entry> storeEntry : keystoreService.getEntries().entrySet()) {
            final String alias = storeEntry.getKey();

            if (!allowedAliases.isEmpty() && !allowedAliases.contains(alias)) {
                continue;
            }

            asX509Certificate(storeEntry.getValue()) //
                    .filter(JwtKeyMaterial::isX509CertificateUsable) //
                    .ifPresent(certificate -> result.put(alias, certificate));
        }

        return result;
    }

    private static boolean isX509CertificateUsable(final X509Certificate certificate) {
        return certificate.getPublicKey() instanceof RSAPublicKey;
    }

    private static Optional<X509Certificate> asX509Certificate(final Entry entry) {
        if (entry instanceof TrustedCertificateEntry trustedCertificateEntry) {
            return asX509Certificate(trustedCertificateEntry.getTrustedCertificate());
        }

        if (entry instanceof PrivateKeyEntry privateKeyEntry) {
            return asX509Certificate(privateKeyEntry.getCertificate());
        }

        return Optional.empty();
    }

    private static Optional<X509Certificate> asX509Certificate(final Certificate certificate) {
        return certificate instanceof X509Certificate x509Certificate ? Optional.of(x509Certificate)
                : Optional.empty();
    }

}
