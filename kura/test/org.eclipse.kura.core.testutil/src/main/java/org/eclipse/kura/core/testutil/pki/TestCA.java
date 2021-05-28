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
 ******************************************************************************/

package org.eclipse.kura.core.testutil.pki;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class TestCA {

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

    public TestCA(final CertificateCreationOptions options)
            throws OperatorCreationException, CertificateException, NoSuchAlgorithmException, CertIOException {
        this(options, generateKeyPair());
    }

    public TestCA(final CertificateCreationOptions options, final KeyPair keyPair)
            throws OperatorCreationException, CertificateException, CertIOException {
        this.caKeyPair = keyPair;
        this.caDn = options.getDn();

        this.certificate = buildCertificate(options, nextSerial, keyPair, options.getDn(), keyPair);

        this.nextSerial = this.nextSerial.add(BigInteger.ONE);
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public X509Certificate getCertificate() {
        return this.certificate;
    }

    private static X509Certificate buildCertificate(final CertificateCreationOptions options, final BigInteger serial,
            final KeyPair certPair, final X500Name issuerName, final KeyPair issuerPair)
            throws CertificateException, OperatorCreationException, CertIOException {
        final ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(issuerPair.getPrivate());

        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(issuerName, serial,
                options.getStartDate().orElseGet(() -> Date.from(DEFAULT_END_INSTANT)),
                options.getEndDate().orElseGet(() -> Date.from(DEFAULT_END_INSTANT)), options.getDn(),
                certPair.getPublic());

        final Optional<URI> crlDistributionPoint = options.getGetDownloadURL();

        if (crlDistributionPoint.isPresent()) {
            final GeneralName generalName = new GeneralName(6, new DERIA5String(crlDistributionPoint.get().toString()));
            final GeneralNames generalNames = new GeneralNames(generalName);
            final DistributionPointName dpn = new DistributionPointName(0, generalNames);
            CRLDistPoint crlDp = new CRLDistPoint(Collections.singletonList(new DistributionPoint(dpn, null, null))
                    .toArray(new DistributionPoint[0]));
            certBuilder.addExtension(Extension.cRLDistributionPoints, false, crlDp);
        }

        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(contentSigner));
    }

    public X509Certificate createAndSignCertificate(final CertificateCreationOptions options)
            throws OperatorCreationException, CertificateException, NoSuchAlgorithmException, CertIOException {
        return createAndSignCertificate(options, generateKeyPair());
    }

    public X509Certificate createAndSignCertificate(final CertificateCreationOptions options, final KeyPair keyPair)
            throws OperatorCreationException, CertificateException, CertIOException {
        final X509Certificate result = buildCertificate(options, nextSerial, keyPair, this.caDn, this.caKeyPair);

        nextSerial = nextSerial.add(BigInteger.ONE);

        return result;
    }

    public void revokedCertificate(final BigInteger serial) {
        if (this.revokedCertificates.stream().noneMatch(c -> c.serial.equals(serial))) {
            this.revokedCertificates.add(new RevokedCertificate(serial));
        }
    }

    public X509CRL generateCRL(final CRLCreationOptions options)
            throws CertIOException, NoSuchAlgorithmException, OperatorCreationException, CRLException {
        final Date creationDate = options.getStartDate().orElseGet(Date::new);

        final X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(this.caDn, creationDate);

        for (final RevokedCertificate revokedCertificate : this.revokedCertificates) {
            crlBuilder.addCRLEntry(revokedCertificate.serial, revokedCertificate.revocationDate, 0);
        }

        final Optional<Date> endDate = options.getEndDate();

        if (endDate.isPresent()) {
            crlBuilder.setNextUpdate(endDate.get());
        }

        crlBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(caKeyPair.getPublic()));
        crlBuilder.addExtension(Extension.cRLNumber, false, new CRLNumber(this.nextCrlNumber));

        final ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(this.caKeyPair.getPrivate());

        return new JcaX509CRLConverter().getCRL(crlBuilder.build(contentSigner));
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

}
