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

    private SslManagerServiceOptions m_sslManagerOpts;
    private String m_protocol;
    private String m_ciphers;
    private String m_trustStore;
    private String m_keyStore;
    private char[] m_keyStorePassword;
    private String m_alias;
    private boolean m_hostnameVerification;

    public ConnectionSslOptions(SslManagerServiceOptions sslManagerOpts) {
        m_sslManagerOpts= sslManagerOpts;
    }
    
    public SslManagerServiceOptions getSslManagerOpts() {
        return m_sslManagerOpts;
    }

    public String getProtocol() {
        return m_protocol;
    }
    public void setProtocol(String protocol) {
        if (protocol == null || "".equals(protocol.trim())) {
            m_protocol= m_sslManagerOpts.getSslProtocol();
        } else {
            m_protocol = protocol;
        }
    }
    
    public String getCiphers() {
        return m_ciphers;
    }
    public void setCiphers(String ciphers) {
        if (ciphers == null || "".equals(ciphers.trim())) {
            m_ciphers= m_sslManagerOpts.getSslCiphers();
        } else {
            m_ciphers = ciphers;
        }
    }
    
    public String getTrustStore() {
        return m_trustStore;
    }
    public void setTrustStore(String trustStore) {
        if (trustStore == null || "".equals(trustStore.trim())) {
            m_trustStore= m_sslManagerOpts.getSslKeyStore();
        } else {
            m_trustStore = trustStore;
        }
    }
    
    public String getKeyStore() {
        return m_keyStore;
    }
    public void setKeyStore(String keyStore) {
        if (keyStore == null || "".equals(keyStore.trim())) {
            m_keyStore= m_sslManagerOpts.getSslKeyStore();
        } else {
            m_keyStore = keyStore;
        }
    }
    
    public char[] getKeyStorePassword() {
        return m_keyStorePassword;
    }
    public void setKeyStorePassword(char[] keyStorePassword) {
        m_keyStorePassword = keyStorePassword;
    }
    
    public String getAlias() {
        return m_alias;
    }
    public void setAlias(String alias) {
        m_alias = alias;
    }
    
    public boolean getHostnameVerification() {
        return m_hostnameVerification;
    }
    public void setHostnameVerification(boolean hostnameVerification) {
        m_hostnameVerification = hostnameVerification;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_alias == null) ? 0 : m_alias.hashCode());
        result = prime * result + ((m_ciphers == null) ? 0 : m_ciphers.hashCode());
        result = prime * result + (m_hostnameVerification ? 1231 : 1237);
        result = prime * result + ((m_keyStore == null) ? 0 : m_keyStore.hashCode());
        result = prime * result + Arrays.hashCode(m_keyStorePassword);
        result = prime * result + ((m_protocol == null) ? 0 : m_protocol.hashCode());
        result = prime * result + ((m_sslManagerOpts == null) ? 0 : m_sslManagerOpts.hashCode());
        result = prime * result + ((m_trustStore == null) ? 0 : m_trustStore.hashCode());
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
        if (m_alias == null) {
            if (other.m_alias != null) {
                return false;
            }
        } else if (!m_alias.equals(other.m_alias)) {
            return false;
        }
        if (m_ciphers == null) {
            if (other.m_ciphers != null) {
                return false;
            }
        } else if (!m_ciphers.equals(other.m_ciphers)) {
            return false;
        }
        if (m_hostnameVerification != other.m_hostnameVerification) {
            return false;
        }
        if (m_keyStore == null) {
            if (other.m_keyStore != null) {
                return false;
            }
        } else if (!m_keyStore.equals(other.m_keyStore)) {
            return false;
        }
        if (!Arrays.equals(m_keyStorePassword, other.m_keyStorePassword)) {
            return false;
        }
        if (m_protocol == null) {
            if (other.m_protocol != null) {
                return false;
            }
        } else if (!m_protocol.equals(other.m_protocol)) {
            return false;
        }
        if (m_sslManagerOpts == null) {
            if (other.m_sslManagerOpts != null) {
                return false;
            }
        } else if (!m_sslManagerOpts.equals(other.m_sslManagerOpts)) {
            return false;
        }
        if (m_trustStore == null) {
            if (other.m_trustStore != null) {
                return false;
            }
        } else if (!m_trustStore.equals(other.m_trustStore)) {
            return false;
        }
        return true;
    } 
}
