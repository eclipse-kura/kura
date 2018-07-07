/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.ssl;

import java.util.Map;

public class SslManagerServiceOptions {

    public static final String PROP_PROTOCOL = "ssl.default.protocol";
    public static final String PROP_TRUST_STORE = "ssl.default.trustStore";
    public static final String PROP_CIPHERS = "ssl.default.cipherSuites";
    public static final String PROP_HN_VERIFY = "ssl.hostname.verification";
    public static final String PROP_TRUST_PASSWORD = "ssl.keystore.password";

    public static final String PROP_DEFAULT_PROTOCOL = "TLSv1";
    public static final Boolean PROP_DEFAULT_HN_VERIFY = true;
    public static final String PROP_DEFAULT_TRUST_PASSWORD = "changeit";

    private final Map<String, Object> properties;

    public SslManagerServiceOptions(Map<String, Object> properties) {
        this.properties = properties;
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
        if (this.properties != null && this.properties.get(PROP_PROTOCOL) != null
                && this.properties.get(PROP_PROTOCOL) instanceof String) {
            return (String) this.properties.get(PROP_PROTOCOL);
        }
        return PROP_DEFAULT_PROTOCOL;
    }

    /**
     * Returns the ssl.default.trustStore.
     *
     * @return
     */
    public String getSslKeyStore() {
        if (this.properties != null && this.properties.get(PROP_TRUST_STORE) != null
                && this.properties.get(PROP_TRUST_STORE) instanceof String) {
            return (String) this.properties.get(PROP_TRUST_STORE);
        }
        return null;
    }

    /**
     * Returns the ssl.default.trustStore.
     *
     * @return
     */
    public String getSslCiphers() {
        if (this.properties != null && this.properties.get(PROP_CIPHERS) != null
                && this.properties.get(PROP_CIPHERS) instanceof String) {
            return (String) this.properties.get(PROP_CIPHERS);
        }
        return null;
    }

    /**
     * Returns the ssl.keystore.password.
     *
     * @return
     */
    public String getSslKeystorePassword() {
        if (this.properties != null && this.properties.get(PROP_TRUST_PASSWORD) != null
                && this.properties.get(PROP_TRUST_PASSWORD) instanceof String) {
            return (String) this.properties.get(PROP_TRUST_PASSWORD);
        }
        return PROP_DEFAULT_TRUST_PASSWORD;
    }

    /**
     * Returns the ssl.hostname.verification
     *
     * @return
     */
    public Boolean isSslHostnameVerification() {
        if (this.properties != null && this.properties.get(PROP_HN_VERIFY) != null
                && this.properties.get(PROP_HN_VERIFY) instanceof Boolean) {
            return (Boolean) this.properties.get(PROP_HN_VERIFY);
        }
        return PROP_DEFAULT_HN_VERIFY;
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
