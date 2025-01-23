/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.keystore.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class CertificateUtil {

    private CertificateUtil() {

    }

    public static java.security.cert.X509Certificate toJavaX509Certificate(Object o) throws Exception {
        CertificateFactory fac = CertificateFactory.getInstance("X509");
        if (o instanceof X509CertificateHolder) {
            return (java.security.cert.X509Certificate) fac
                    .generateCertificate(new ByteArrayInputStream(((X509CertificateHolder) o).getEncoded()));
        } else if (o instanceof X509Certificate) {
            return (java.security.cert.X509Certificate) fac
                    .generateCertificate(new ByteArrayInputStream(((X509Certificate) o).getEncoded()));
        } else if (o instanceof java.security.cert.X509Certificate) {
            return (java.security.cert.X509Certificate) o;
        } else if (o instanceof PemObject) {
            return (java.security.cert.X509Certificate) fac
                    .generateCertificate(new ByteArrayInputStream(((PemObject) o).getContent()));
        }
        throw new IllegalArgumentException("Object not one of X509CertificateHolder, X509Certificate or PemObject.");
    }

    public static Set<TrustAnchor> toTrustAnchor(List<X509Certificate> certificates) throws Exception {

        Set<TrustAnchor> out = new HashSet<>();
        for (X509Certificate c : certificates) {
            out.add(new TrustAnchor(c, null));
        }

        return out;
    }

    public static String x509CertificateToPem(final Certificate cert) throws IOException {
        final StringWriter writer = new StringWriter();
        final JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(cert);
        pemWriter.flush();
        pemWriter.close();
        return writer.toString();
    }

    public static List<X509Certificate> readPemCertificates(String pemString) throws Exception {
        List<X509Certificate> certs = new ArrayList<>();
        try (Reader r = new StringReader(pemString); PemReader reader = new PemReader(r);) {

            PemObject o;

            while ((o = reader.readPemObject()) != null) {
                certs.add(toJavaX509Certificate(o));
            }
        }
        return certs;
    }

    /**
     *
     * @param pemString
     *            String in PEM format with PKCS#8 syntax
     * @param password
     * @return
     * @throws IOException
     * @throws PKCSException
     */
    public static PrivateKey readEncryptedPrivateKey(String pemString, String password)
            throws IOException, PKCSException {
        try (Reader r = new StringReader(pemString); PEMParser pemParser = new PEMParser(r);) {

            PrivateKeyInfo privateKeyInfo;
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            Object o = pemParser.readObject();

            if (o instanceof PKCS8EncryptedPrivateKeyInfo) {

                PKCS8EncryptedPrivateKeyInfo epki = (PKCS8EncryptedPrivateKeyInfo) o;

                JcePKCSPBEInputDecryptorProviderBuilder builder = new JcePKCSPBEInputDecryptorProviderBuilder()
                        .setProvider("BC");

                InputDecryptorProvider idp = builder.build(password.toCharArray());
                privateKeyInfo = epki.decryptPrivateKeyInfo(idp);
            } else if (o instanceof PEMEncryptedKeyPair) {

                PEMEncryptedKeyPair epki = (PEMEncryptedKeyPair) o;
                PEMKeyPair pkp = epki.decryptKeyPair(new BcPEMDecryptorProvider(password.toCharArray()));

                privateKeyInfo = pkp.getPrivateKeyInfo();
            } else {
                throw new PKCSException("Invalid encrypted private key class: " + o.getClass().getName());
            }
            return converter.getPrivateKey(privateKeyInfo);
        }
    }

    public static PublicKey readPublicKey(String pemString, String algorithm) throws Exception {
        KeyFactory factory = KeyFactory.getInstance(algorithm);

        try (Reader r = new StringReader(pemString); PemReader pemReader = new PemReader(r)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
            return factory.generatePublic(pubKeySpec);
        }
    }

    public static X509Certificate[] generateCertificateChain(KeyPair keyPair, String signatureAlgorithm,
            String attributes, Date startDate, Date endDate) throws OperatorCreationException, CertificateException {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
        X500Name dnName = new X500Name(attributes);

        // Use the timestamp as serial number
        BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName, certSerialNumber, startDate,
                endDate, dnName, subjectPublicKeyInfo);
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(bcProvider)
                .build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        return new X509Certificate[] { new JcaX509CertificateConverter().getCertificate(certificateHolder) };
    }

    @SuppressWarnings("unchecked")
    public static Store<X509CertificateHolder> toX509CertificateHolderStore(String pemString) throws Exception {

        List<X509Certificate> certs = readPemCertificates(pemString);

        List<X509CertificateHolder> x509CertificateHolderCerts = certs.stream().map(cert -> {
            try {
                return new X509CertificateHolder(cert.getEncoded());
            } catch (CertificateEncodingException | IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return new JcaCertStore(x509CertificateHolderCerts);
    }

    public static CertStore toCertStore(Store<?> store) throws Exception {
        List<?> certificatesX509 = store.getMatches(null).stream().map(t -> {
            try {
                return toJavaX509Certificate(t);
            } catch (Exception ignore) {
            }
            return null;
        }).collect(Collectors.toList());

        try {
            return CertStore.getInstance("Collection", new CollectionCertStoreParameters(certificatesX509));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new Exception(e);
        }
    }

}
