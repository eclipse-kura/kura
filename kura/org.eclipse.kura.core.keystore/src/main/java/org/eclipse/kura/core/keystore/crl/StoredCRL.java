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
package org.eclipse.kura.core.keystore.crl;

import java.io.IOException;
import java.net.URI;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class StoredCRL {

    private static final Logger logger = LoggerFactory.getLogger(StoredCRL.class);

    private static final String DISTRIBUTION_POINTS_KEY = "dps";
    private static final String BODY_KEY = "body";

    private final Set<URI> distributionPoints;
    private final X509CRL crl;

    public StoredCRL(final Set<URI> distributionPoints, final X509CRL crl) {
        this.distributionPoints = distributionPoints;
        this.crl = crl;
    }

    public Set<URI> getDistributionPoints() {
        return distributionPoints;
    }

    public X509CRL getCrl() {
        return crl;
    }

    public boolean isExpired() {
        final long now = System.currentTimeMillis();
        final Date nextUpdate = crl.getNextUpdate();

        return nextUpdate != null && nextUpdate.getTime() < now;
    }

    public static StoredCRL fromJson(final JsonObject object) throws IOException, CRLException {
        final Set<URI> dps = new HashSet<>();

        final JsonArray dpsArray = object.get(DISTRIBUTION_POINTS_KEY).asArray();

        for (final JsonValue value : dpsArray) {
            try {
                dps.add(new URI(value.asString()));
            } catch (final Exception e) {
                logger.warn("failed to parse URI", e);
            }
        }

        final String body = object.get(BODY_KEY).asString();

        final Decoder decoder = Base64.getDecoder();

        final byte[] decoded = decoder.decode(body);

        final X509CRLHolder holder = new X509CRLHolder(decoded);
        return new StoredCRL(dps, new JcaX509CRLConverter().getCRL(holder));
    }

    public JsonObject toJson() throws CRLException {
        final JsonObject result = new JsonObject();

        final JsonArray dpsArray = new JsonArray();

        for (final URI uri : this.distributionPoints) {
            dpsArray.add(uri.toString());
        }

        result.add(DISTRIBUTION_POINTS_KEY, dpsArray);

        final Encoder encoder = Base64.getEncoder();

        final String body = encoder.encodeToString(crl.getEncoded());

        result.add(BODY_KEY, body);

        return result;
    }
}
