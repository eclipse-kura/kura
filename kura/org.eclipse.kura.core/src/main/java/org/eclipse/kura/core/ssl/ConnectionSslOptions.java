/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import java.util.Arrays;

public class ConnectionSslOptions {

    private final SslManagerServiceOptions sslManagerOpts;
    private String protocol;
    private String ciphers;
    private String trustStore;
    private String keyStore;
    private char[] keyStorePassword;
    private String alias;
    private boolean hostnameVerification;

    public ConnectionSslOptions(SslManagerServiceOptions sslManagerOpts) {
        this.sslManagerOpts = sslManagerOpts;
    }

    public SslManagerServiceOptions getSslManagerOpts() {
        return this.sslManagerOpts;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        if (protocol == null || "".equals(protocol.trim())) {
            this.protocol = this.sslManagerOpts.getSslProtocol();
        } else {
            this.protocol = protocol;
        }
    }

    public String getCiphers() {
        return this.ciphers;
    }

    public void setCiphers(String ciphers) {
        if (ciphers == null || "".equals(ciphers.trim())) {
            this.ciphers = this.sslManagerOpts.getSslCiphers();
        } else {
            this.ciphers = ciphers;
        }
    }

    public String getTrustStore() {
        return this.trustStore;
    }

    public void setTrustStore(String trustStore) {
        if (trustStore == null || "".equals(trustStore.trim())) {
            this.trustStore = this.sslManagerOpts.getSslKeyStore();
        } else {
            this.trustStore = trustStore;
        }
    }

    public String getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(String keyStore) {
        if (keyStore == null || "".equals(keyStore.trim())) {
            this.keyStore = this.sslManagerOpts.getSslKeyStore();
        } else {
            this.keyStore = keyStore;
        }
    }

    public char[] getKeyStorePassword() {
        return this.keyStorePassword;
    }

    public void setKeyStorePassword(char[] keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean getHostnameVerification() {
        return this.hostnameVerification;
    }

    public void setHostnameVerification(boolean hostnameVerification) {
        this.hostnameVerification = hostnameVerification;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.alias == null ? 0 : this.alias.hashCode());
        result = prime * result + (this.ciphers == null ? 0 : this.ciphers.hashCode());
        result = prime * result + (this.hostnameVerification ? 1231 : 1237);
        result = prime * result + (this.keyStore == null ? 0 : this.keyStore.hashCode());
        result = prime * result + Arrays.hashCode(this.keyStorePassword);
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.sslManagerOpts == null ? 0 : this.sslManagerOpts.hashCode());
        result = prime * result + (this.trustStore == null ? 0 : this.trustStore.hashCode());
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
        if (!(obj instanceof ConnectionSslOptions)) {
            return false;
        }
        ConnectionSslOptions other = (ConnectionSslOptions) obj;
        if (this.alias == null) {
            if (other.alias != null) {
                return false;
            }
        } else if (!this.alias.equals(other.alias)) {
            return false;
        }
        if (this.ciphers == null) {
            if (other.ciphers != null) {
                return false;
            }
        } else if (!this.ciphers.equals(other.ciphers)) {
            return false;
        }
        if (this.hostnameVerification != other.hostnameVerification) {
            return false;
        }
        if (this.keyStore == null) {
            if (other.keyStore != null) {
                return false;
            }
        } else if (!this.keyStore.equals(other.keyStore)) {
            return false;
        }
        if (!Arrays.equals(this.keyStorePassword, other.keyStorePassword)) {
            return false;
        }
        if (this.protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!this.protocol.equals(other.protocol)) {
            return false;
        }
        if (this.sslManagerOpts == null) {
            if (other.sslManagerOpts != null) {
                return false;
            }
        } else if (!this.sslManagerOpts.equals(other.sslManagerOpts)) {
            return false;
        }
        if (this.trustStore == null) {
            if (other.trustStore != null) {
                return false;
            }
        } else if (!this.trustStore.equals(other.trustStore)) {
            return false;
        }
        return true;
    }
}
