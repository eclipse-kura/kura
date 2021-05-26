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
package org.eclipse.kura.core.keystore.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CRLUtil {

    private static final Logger logger = LoggerFactory.getLogger(CRLUtil.class);

    private CRLUtil() {
    }

    public static Set<URI> getCrlURIs(final X509Certificate cert) throws IOException {
        final byte[] crlDistributionPoints = cert.getExtensionValue(Extension.cRLDistributionPoints.getId());

        if (crlDistributionPoints == null) {
            return Collections.emptySet();
        }

        try (final ASN1InputStream in0 = new ASN1InputStream(crlDistributionPoints);
                final ASN1InputStream in = new ASN1InputStream(((DEROctetString) in0.readObject()).getOctets())) {

            final DistributionPoint[] distributionPoints = CRLDistPoint.getInstance(in.readObject())
                    .getDistributionPoints();

            if (distributionPoints == null) {
                return Collections.emptySet();
            }

            final Set<URI> result = new HashSet<>();

            for (final DistributionPoint distributionPoint : distributionPoints) {

                final DistributionPointName distributionPointName = distributionPoint.getDistributionPoint();

                if (distributionPointName == null
                        || distributionPointName.getType() != DistributionPointName.FULL_NAME) {
                    logger.warn("failed to get distribution point name");
                    continue;
                }

                final GeneralName[] generalNames = GeneralNames.getInstance(distributionPointName.getName()).getNames();

                for (final GeneralName name : generalNames) {
                    if (name.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        final String uriString = DERIA5String.getInstance(name.getName()).getString();
                        parseURI(uriString).ifPresent(result::add);
                    }
                }
            }

            return result;
        }
    }

    public static CompletableFuture<X509CRL> fetchCRL(final Set<URI> uris, final ExecutorService executor) {
        final CompletableFuture<X509CRL> result = new CompletableFuture<>();

        executor.execute(() -> {
            for (final URI uri : uris) {
                logger.info("fetching CRL from: {}...", uri);
                try (final InputStream in = uri.toURL().openStream()) {
                    final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    final X509CRL crl = (X509CRL) cf.generateCRL(in);
                    logger.info("fetching CRL from: {}...done", uri);
                    result.complete(crl);
                    return;
                } catch (final Exception e) {
                    logger.warn("failed to fetch CRL from {}", uri, e);
                }

                if (Thread.interrupted()) {
                    logger.warn("interrupted");
                    result.completeExceptionally(new InterruptedException());
                    return;
                }
            }
            result.completeExceptionally(new IOException("failed to download CRL from: " + uris));
        });

        return result;
    }

    private static Optional<URI> parseURI(final String value) {
        try {
            return Optional.of(new URI(value));
        } catch (final Exception e) {
            logger.warn("failed to parse distribution point URL", e);
            return Optional.empty();
        }
    }
}
