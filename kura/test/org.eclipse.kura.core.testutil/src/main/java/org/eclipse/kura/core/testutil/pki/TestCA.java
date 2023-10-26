/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.core.testutil.pki;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class TestCA {

    public static final String TEST_KEYSTORE_PASSWORD = "changeit";

    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    private static final Instant DEFAULT_START_INSTANT = Instant.now();
    private static final Instant DEFAULT_END_INSTANT = DEFAULT_START_INSTANT.plus(365, ChronoUnit.DAYS);

    private BigInteger nextSerial = BigInteger.ONE;
    private BigInteger nextCrlNumber = BigInteger.ONE;
    private final KeyPair caKeyPair;
    private final X500Name caDn;
    private final List<RevokedCertificate> revokedCertificates = new ArrayList<>();
    private final X509Certificate certificate;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public TestCA(final KeyStore keyStore, final String keyAlias, final String keystorePassword)
            throws TestCAException {
        try {
            final PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry(keyAlias,
                    new PasswordProtection(keystorePassword.toCharArray()));

            this.certificate = (X509Certificate) entry.getCertificate();
            this.caKeyPair = new KeyPair(this.certificate.getPublicKey(), entry.getPrivateKey());
            this.caDn = new JcaX509CertificateHolder(this.certificate).getSubject();
        } catch (final Exception e) {
            throw new TestCAException(e);
        }
    }

    public TestCA(final CertificateCreationOptions options) throws TestCAException {
        this(options, generateKeyPair());
    }

    public TestCA(final CertificateCreationOptions options, final KeyPair keyPair) throws TestCAException {
        try {
            this.caKeyPair = keyPair;
            this.caDn = options.getDn();

            this.certificate = buildCertificate(options, nextSerial, keyPair, options.getDn(), keyPair);

            this.nextSerial = this.nextSerial.add(BigInteger.ONE);
        } catch (final TestCAException e) {
            throw e;
        } catch (final Exception e) {
            throw new TestCAException(e);
        }
    }

    public static KeyPair generateKeyPair() throws TestCAException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (final Exception e) {
            throw new TestCAException(e);
        }
    }

    public X509Certificate getCertificate() {
        return this.certificate;
    }

    private static X509Certificate buildCertificate(final CertificateCreationOptions options, final BigInteger serial,
            final KeyPair certPair, final X500Name issuerName, final KeyPair issuerPair) throws TestCAException {
        try {
            final ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                    .build(issuerPair.getPrivate());

            final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(issuerName, serial,
                    options.getStartDate().orElseGet(() -> Date.from(DEFAULT_START_INSTANT)),
                    options.getEndDate().orElseGet(() -> Date.from(DEFAULT_END_INSTANT)), options.getDn(),
                    certPair.getPublic());

            certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new BcX509ExtensionUtils()
                    .createSubjectKeyIdentifier(SubjectPublicKeyInfo.getInstance(certPair.getPublic().getEncoded())));

            if (!(issuerName.equals(options.dn))) {
                certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                        new BcX509ExtensionUtils().createAuthorityKeyIdentifier(
                                SubjectPublicKeyInfo.getInstance(issuerPair.getPublic().getEncoded())));
            } else {
                certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            }

            final Optional<URI> crlDistributionPoint = options.getGetDownloadURL();

            if (crlDistributionPoint.isPresent()) {
                final GeneralName generalName = new GeneralName(6,
                        new DERIA5String(crlDistributionPoint.get().toString()));
                final GeneralNames generalNames = new GeneralNames(generalName);
                final DistributionPointName dpn = new DistributionPointName(0, generalNames);
                CRLDistPoint crlDp = new CRLDistPoint(Collections.singletonList(new DistributionPoint(dpn, null, null))
                        .toArray(new DistributionPoint[0]));
                certBuilder.addExtension(Extension.cRLDistributionPoints, false, crlDp);
            }

            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certBuilder.build(contentSigner));
        } catch (final Exception e) {
            throw new TestCAException(e);
        }
    }

    public X509Certificate createAndSignCertificate(final CertificateCreationOptions options) throws TestCAException {
        return createAndSignCertificate(options, generateKeyPair());
    }

    public X509Certificate createAndSignCertificate(final CertificateCreationOptions options, final KeyPair keyPair)
            throws TestCAException {
        final X509Certificate result = buildCertificate(options, nextSerial, keyPair, this.caDn, this.caKeyPair);

        nextSerial = nextSerial.add(BigInteger.ONE);

        return result;
    }

    public void revokeCertificate(final X509Certificate certificate) {
        revokeCertificate(certificate.getSerialNumber());
    }

    public void revokeCertificate(final BigInteger serial) {
        if (this.revokedCertificates.stream().noneMatch(c -> c.serial.equals(serial))) {
            this.revokedCertificates.add(new RevokedCertificate(serial));
        }
    }

    public X509CRL generateCRL(final CRLCreationOptions options) throws TestCAException {
        try {
            final Date creationDate = options.getStartDate().orElseGet(Date::new);

            final X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(this.caDn, creationDate);

            for (final RevokedCertificate revokedCertificate : this.revokedCertificates) {
                crlBuilder.addCRLEntry(revokedCertificate.serial, revokedCertificate.revocationDate, 0);
            }

            final Optional<Date> endDate = options.getEndDate();

            crlBuilder.setNextUpdate(endDate.orElseGet(() -> Date.from(DEFAULT_END_INSTANT)));

            crlBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                    new BcX509ExtensionUtils().createAuthorityKeyIdentifier(
                            SubjectPublicKeyInfo.getInstance(this.caKeyPair.getPublic().getEncoded())));
            crlBuilder.addExtension(Extension.cRLNumber, false, new CRLNumber(this.nextCrlNumber));

            final ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                    .build(this.caKeyPair.getPrivate());

            return new JcaX509CRLConverter().getCRL(crlBuilder.build(contentSigner));
        } catch (final Exception e) {
            throw new TestCAException(e);
        }
    }

    public static void encodeToPEM(final X509Certificate certificate, final OutputStream out) throws IOException {
        try (final JcaPEMWriter pw = new JcaPEMWriter(new OutputStreamWriter(out))) {
            pw.writeObject(certificate);
        }
    }

    public static void encodeToPEM(final X509CRL crl, final OutputStream out) throws IOException {
        try (final JcaPEMWriter pw = new JcaPEMWriter(new OutputStreamWriter(out))) {
            pw.writeObject(crl);
        }
    }

    public static void encodeToPEM(final PrivateKey privateKey, final OutputStream out) throws IOException {
        try (final JcaPEMWriter pw = new JcaPEMWriter(new OutputStreamWriter(out))) {
            pw.writeObject(privateKey);
        }
    }

    public static void writeKeystore(final File file, final KeyStore.Entry... entries)
            throws TestCAException, IOException {
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);

            for (final KeyStore.Entry entry : entries) {
                final ProtectionParameter param;

                if (entry instanceof PrivateKeyEntry) {
                    param = new KeyStore.PasswordProtection(TEST_KEYSTORE_PASSWORD.toCharArray());

                } else {
                    param = null;
                }

                keyStore.setEntry(Integer.toString(new Random(System.nanoTime()).nextInt()), entry, param);
            }

            try (final FileOutputStream out = new FileOutputStream(file)) {
                keyStore.store(out, TEST_KEYSTORE_PASSWORD.toCharArray());
            }
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new TestCAException(e);
        }

    }

    public static File writeKeystore(final KeyStore.Entry... entries) throws TestCAException, IOException {
        final File result = Files.createTempFile(null, null).toFile();

        writeKeystore(result, entries);

        return result;
    }

    private static class RevokedCertificate {

        private final BigInteger serial;
        private final Date revocationDate;

        public RevokedCertificate(final BigInteger serial) {
            this.serial = serial;
            this.revocationDate = new Date();
        }
    }

    public static class CRLCreationOptions {

        private final Optional<Date> startDate;
        private final Optional<Date> endDate;

        private CRLCreationOptions(final Builder builder) {
            this.startDate = builder.startDate;
            this.endDate = builder.endDate;
        }

        public Optional<Date> getStartDate() {
            return startDate;
        }

        public Optional<Date> getEndDate() {
            return endDate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private Optional<Date> startDate = Optional.empty();
            private Optional<Date> endDate = Optional.empty();

            public Builder withStartDate(final Date startDate) {
                this.startDate = Optional.of(startDate);
                return this;
            }

            public Builder withEndDate(final Date endDate) {
                this.endDate = Optional.of(endDate);
                return this;
            }

            public CRLCreationOptions build() {
                return new CRLCreationOptions(this);
            }
        }
    }

    public static class CertificateCreationOptions {

        private final X500Name dn;
        private final Optional<Date> startDate;
        private final Optional<Date> endDate;
        private final Optional<URI> crlDownloadURL;

        private CertificateCreationOptions(final Builder builder) {
            this.dn = builder.dn;
            this.startDate = builder.startDate;
            this.endDate = builder.endDate;
            this.crlDownloadURL = builder.crlDownloadURL;
        }

        public static Builder builder(final X500Name dn) {
            return new Builder(dn);
        }

        public X500Name getDn() {
            return dn;
        }

        public Optional<Date> getStartDate() {
            return startDate;
        }

        public Optional<Date> getEndDate() {
            return endDate;
        }

        public Optional<URI> getGetDownloadURL() {
            return crlDownloadURL;
        }

        public static class Builder {

            private final X500Name dn;
            private Optional<Date> startDate = Optional.empty();
            private Optional<Date> endDate = Optional.empty();
            private Optional<URI> crlDownloadURL = Optional.empty();

            public Builder(final X500Name dn) {
                this.dn = dn;
            }

            public Builder withStartDate(final Date startDate) {
                this.startDate = Optional.of(startDate);
                return this;
            }

            public Builder withEndDate(final Date endDate) {
                this.endDate = Optional.of(endDate);
                return this;
            }

            public Builder withCRLDownloadURI(final URI uri) {
                this.crlDownloadURL = Optional.of(uri);
                return this;
            }

            public CertificateCreationOptions build() {
                return new CertificateCreationOptions(this);
            }
        }
    }

    public static class TestCAException extends Exception {

        /**
         * 
         */
        private static final long serialVersionUID = -8997629775984256528L;

        public TestCAException(Throwable cause) {
            super(cause);
        }

    }

}
