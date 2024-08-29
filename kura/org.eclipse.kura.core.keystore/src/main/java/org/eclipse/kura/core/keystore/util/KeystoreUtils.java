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
 *
 *******************************************************************************/
package org.eclipse.kura.core.keystore.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class KeystoreUtils {

    private KeystoreUtils() {
        // Empty constructor
    }

    public static PrivateKeyEntry createPrivateKey(String privateKey, String publicKey)
            throws IOException, GeneralSecurityException {
        // Works with RSA and DSA. EC is not supported since the certificate is encoded
        // with ECDSA while the corresponding private key with EC.
        // This cause an error when the PrivateKeyEntry is generated.
        Certificate[] certs = parsePublicCertificates(publicKey);

        Security.addProvider(new BouncyCastleProvider());
        PEMParser pemParser = new PEMParser(new StringReader(privateKey));
        Object object = pemParser.readObject();
        pemParser.close();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PrivateKey privkey = null;
        if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
            privkey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        } else if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
            privkey = converter.getKeyPair((org.bouncycastle.openssl.PEMKeyPair) object).getPrivate();
        } else {
            throw new IOException("PrivateKey not recognized.");
        }
        return new PrivateKeyEntry(privkey, certs);
    }

    public static X509Certificate[] parsePublicCertificates(String certificates) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certificates.getBytes(StandardCharsets.UTF_8));

        final Collection<? extends Certificate> decodedCertificates = certFactory.generateCertificates(is);

        final ArrayList<X509Certificate> result = new ArrayList<>();

        for (final Certificate cert : decodedCertificates) {
            if (!(cert instanceof X509Certificate)) {
                throw new CertificateException("Provided certificate is not a X509Certificate");
            }

            result.add((X509Certificate) cert);
        }

        return result.toArray(new X509Certificate[result.size()]);
    }

    public static TrustedCertificateEntry createCertificateEntry(String certificate) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(is);
        return new TrustedCertificateEntry(cert);
    }

}
