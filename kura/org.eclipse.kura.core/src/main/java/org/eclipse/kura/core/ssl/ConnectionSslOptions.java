/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import org.eclipse.kura.ssl.SslManagerServiceOptions;

public class ConnectionSslOptions {

    private final SslManagerServiceOptions m_sslManagerOpts;
    private String m_protocol;
    private String m_ciphers;
    private String m_trustStore;
    private String m_keyStore;
    private char[] m_keyStorePassword;
    private String m_alias;
    private boolean m_hostnameVerification;

    public ConnectionSslOptions(SslManagerServiceOptions sslManagerOpts) {
        this.m_sslManagerOpts = sslManagerOpts;
    }

    public SslManagerServiceOptions getSslManagerOpts() {
        return this.m_sslManagerOpts;
    }

    public String getProtocol() {
        return this.m_protocol;
    }

    public void setProtocol(String protocol) {
        if (protocol == null || "".equals(protocol.trim())) {
            this.m_protocol = this.m_sslManagerOpts.getSslProtocol();
        } else {
            this.m_protocol = protocol;
        }
    }

    public String getCiphers() {
        return this.m_ciphers;
    }

    public void setCiphers(String ciphers) {
        if (ciphers == null || "".equals(ciphers.trim())) {
            this.m_ciphers = this.m_sslManagerOpts.getSslCiphers();
        } else {
            this.m_ciphers = ciphers;
        }
    }

    public String getTrustStore() {
        return this.m_trustStore;
    }

    public void setTrustStore(String trustStore) {
        if (trustStore == null || "".equals(trustStore.trim())) {
            this.m_trustStore = this.m_sslManagerOpts.getSslKeyStore();
        } else {
            this.m_trustStore = trustStore;
        }
    }

    public String getKeyStore() {
        return this.m_keyStore;
    }

    public void setKeyStore(String keyStore) {
        if (keyStore == null || "".equals(keyStore.trim())) {
            this.m_keyStore = this.m_sslManagerOpts.getSslKeyStore();
        } else {
            this.m_keyStore = keyStore;
        }
    }

    public char[] getKeyStorePassword() {
        return this.m_keyStorePassword;
    }

    public void setKeyStorePassword(char[] keyStorePassword) {
        this.m_keyStorePassword = keyStorePassword;
    }

    public String getAlias() {
        return this.m_alias;
    }

    public void setAlias(String alias) {
        this.m_alias = alias;
    }

    public boolean getHostnameVerification() {
        return this.m_hostnameVerification;
    }

    public void setHostnameVerification(boolean hostnameVerification) {
        this.m_hostnameVerification = hostnameVerification;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_alias == null ? 0 : this.m_alias.hashCode());
        result = prime * result + (this.m_ciphers == null ? 0 : this.m_ciphers.hashCode());
        result = prime * result + (this.m_hostnameVerification ? 1231 : 1237);
        result = prime * result + (this.m_keyStore == null ? 0 : this.m_keyStore.hashCode());
        result = prime * result + Arrays.hashCode(this.m_keyStorePassword);
        result = prime * result + (this.m_protocol == null ? 0 : this.m_protocol.hashCode());
        result = prime * result + (this.m_sslManagerOpts == null ? 0 : this.m_sslManagerOpts.hashCode());
        result = prime * result + (this.m_trustStore == null ? 0 : this.m_trustStore.hashCode());
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
        if (this.m_alias == null) {
            if (other.m_alias != null) {
                return false;
            }
        } else if (!this.m_alias.equals(other.m_alias)) {
            return false;
        }
        if (this.m_ciphers == null) {
            if (other.m_ciphers != null) {
                return false;
            }
        } else if (!this.m_ciphers.equals(other.m_ciphers)) {
            return false;
        }
        if (this.m_hostnameVerification != other.m_hostnameVerification) {
            return false;
        }
        if (this.m_keyStore == null) {
            if (other.m_keyStore != null) {
                return false;
            }
        } else if (!this.m_keyStore.equals(other.m_keyStore)) {
            return false;
        }
        if (!Arrays.equals(this.m_keyStorePassword, other.m_keyStorePassword)) {
            return false;
        }
        if (this.m_protocol == null) {
            if (other.m_protocol != null) {
                return false;
            }
        } else if (!this.m_protocol.equals(other.m_protocol)) {
            return false;
        }
        if (this.m_sslManagerOpts == null) {
            if (other.m_sslManagerOpts != null) {
                return false;
            }
        } else if (!this.m_sslManagerOpts.equals(other.m_sslManagerOpts)) {
            return false;
        }
        if (this.m_trustStore == null) {
            if (other.m_trustStore != null) {
                return false;
            }
        } else if (!this.m_trustStore.equals(other.m_trustStore)) {
            return false;
        }
        return true;
    }
}
