/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

    public static final String PROP_PROTOCOL = "ssl.default.protocol";
    public static final String PROP_TRUST_STORE = "ssl.default.trustStore";
    public static final String PROP_CIPHERS = "ssl.default.cipherSuites";
    public static final String PROP_HN_VERIFY = "ssl.hostname.verification";
    public static final String PROP_TRUST_PASSWORD = "ssl.keystore.password";

    public static final Boolean PROP_DEFAULT_HN_VERIFY = true;
    public static final String PROP_DEFAULT_TRUST_PASSWORD = "changeit";

    private static final Property<String> SELECTED_SSL_PROTOCOL = new Property<>(PROP_PROTOCOL, "");
    private static final Property<String> SELECTED_SSL_KEYSTORE = new Property<>(PROP_TRUST_STORE, "");
    private static final Property<String> SELECTED_SSL_CIPHERS = new Property<>(PROP_CIPHERS, "");
    private static final Property<String> SELECTED_SSL_KEYSTORE_PASSWORD = new Property<>(PROP_TRUST_PASSWORD,
            PROP_DEFAULT_TRUST_PASSWORD);
    private static final Property<Boolean> SELECTED_SSL_HN_VERIFICATION = new Property<>(PROP_HN_VERIFY,
            PROP_DEFAULT_HN_VERIFY);

    private final Map<String, Object> properties;

    private final String sslProtocol;
    private final String sslKeystore;
    private final String sslCiphers;
    private final String sslKeystorePassword;
    private final boolean sslHNVerification;

    public SslManagerServiceOptions(Map<String, Object> properties) {
        if (isNull(properties)) {
            throw new IllegalArgumentException("SSL Options cannot be null!");
        }
        this.properties = properties;
        this.sslProtocol = SELECTED_SSL_PROTOCOL.get(properties).trim();
        this.sslKeystore = SELECTED_SSL_KEYSTORE.get(properties).trim();
        this.sslCiphers = SELECTED_SSL_CIPHERS.get(properties).trim();
        this.sslKeystorePassword = SELECTED_SSL_KEYSTORE_PASSWORD.get(properties).trim();
        this.sslHNVerification = SELECTED_SSL_HN_VERIFICATION.get(properties);
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
    public String getSslKeyStore() {
        return this.sslKeystore;
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
     * Returns the ssl.keystore.password.
     *
     * @return
     */
    public String getSslKeystorePassword() {
        return this.sslKeystorePassword;
    }

    /**
     * Returns the ssl.hostname.verification
     *
     * @return
     */
    public Boolean isSslHostnameVerification() {
        return this.sslHNVerification;
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
