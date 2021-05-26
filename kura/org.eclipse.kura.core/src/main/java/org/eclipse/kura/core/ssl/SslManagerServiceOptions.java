/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.ssl;

import static java.util.Objects.isNull;

import java.util.Map;

import org.eclipse.kura.util.configuration.Property;

public class SslManagerServiceOptions {

    public enum RevocationCheckMode {
        PREFER_OCSP,
        PREFER_CRL,
        CRL_ONLY
    }

    public static final String PROP_PROTOCOL = "ssl.default.protocol";
    public static final String PROP_CIPHERS = "ssl.default.cipherSuites";
    public static final String PROP_HN_VERIFY = "ssl.hostname.verification";

    public static final Boolean PROP_DEFAULT_HN_VERIFY = true;
    public static final String PROP_DEFAULT_TRUST_PASSWORD = "changeit";

    private static final Property<String> SELECTED_SSL_PROTOCOL = new Property<>(PROP_PROTOCOL, "");
    private static final Property<String> SELECTED_SSL_CIPHERS = new Property<>(PROP_CIPHERS, "");
    private static final Property<Boolean> SELECTED_SSL_HN_VERIFICATION = new Property<>(PROP_HN_VERIFY,
            PROP_DEFAULT_HN_VERIFY);
    private static final Property<Boolean> SSL_REVOCATION_CHECK_ENABLED = new Property<>("ssl.revocation.check.enabled",
            false);
    private static final Property<Boolean> SSL_REVOCATION_SOFT_FAIL = new Property<>("ssl.revocation.soft.fail", false);
    private static final Property<String> SSL_REVOCATION_MODE = new Property<>("ssl.revocation.mode",
            RevocationCheckMode.PREFER_CRL.name());

    private final Map<String, Object> properties;

    private final String sslProtocol;
    private final String sslCiphers;
    private final boolean sslHNVerification;
    private final boolean sslRevocationCheckEnabled;
    private final RevocationCheckMode sslRevocationMode;
    private final boolean sslRevocationSoftFail;

    public SslManagerServiceOptions(Map<String, Object> properties) {
        if (isNull(properties)) {
            throw new IllegalArgumentException("SSL Options cannot be null!");
        }
        this.properties = properties;
        this.sslProtocol = SELECTED_SSL_PROTOCOL.get(properties).trim();
        this.sslCiphers = SELECTED_SSL_CIPHERS.get(properties).trim();
        this.sslHNVerification = SELECTED_SSL_HN_VERIFICATION.get(properties);
        this.sslRevocationCheckEnabled = SSL_REVOCATION_CHECK_ENABLED.get(properties);
        this.sslRevocationMode = RevocationCheckMode.valueOf(SSL_REVOCATION_MODE.get(properties));
        this.sslRevocationSoftFail = SSL_REVOCATION_SOFT_FAIL.get(properties);
    }

    public Map<String, Object> getConfigurationProperties() {
        return this.properties;
    }

    /**
     * Returns the ssl.default.protocol.
     *
     * @return
     */
    public String getSslProtocol() {
        return this.sslProtocol;
    }

    /**
     * Returns the ssl.default.trustStore.
     *
     * @return
     */
    public String getSslCiphers() {
        return this.sslCiphers;
    }

    /**
     * Returns the ssl.hostname.verification
     *
     * @return
     */
    public Boolean isSslHostnameVerification() {
        return this.sslHNVerification;
    }

    public boolean isSslRevocationCheckEnabled() {
        return sslRevocationCheckEnabled;
    }

    public RevocationCheckMode getRevocationCheckMode() {
        return sslRevocationMode;
    }

    public boolean isSslRevocationSoftFail() {
        return sslRevocationSoftFail;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.properties == null ? 0 : this.properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SslManagerServiceOptions)) {
            return false;
        }
        SslManagerServiceOptions other = (SslManagerServiceOptions) obj;
        if (this.properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!this.properties.equals(other.properties)) {
            return false;
        }
        return true;
    }
}
