/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.ssl;

import java.util.Map;

public class SslManagerServiceOptions
{
    private static final String  PROP_PROTOCOL       = "ssl.default.protocol";
    private static final String  PROP_TRUST_STORE    = "ssl.default.trustStore";
    private static final String  PROP_CIPHERS        = "ssl.default.cipherSuites";    
    private static final String  PROP_HN_VERIFY      = "ssl.hostname.verification";
    private static final String  PROP_TRUST_PASSWORD = "ssl.keystore.password";

    private static final String  PROP_DEFAULT_PROTOCOL    = "TLSv1";
    private static final Boolean PROP_DEFAULT_HN_VERIFY   = true;
    private static final String  PROP_DEFAULT_TRUST_STORE = "/opt/eurotech/esf/security/cacerts.ks";

    private Map<String,Object> m_properties;
    
    SslManagerServiceOptions(Map<String,Object> properties) {
        m_properties = properties;
    }
    
    /**
     * Returns the ssl.default.protocol.
     * @return
     */
    public String getSslProtocol() {
        if (m_properties != null &&
            m_properties.get(PROP_PROTOCOL) != null &&
            m_properties.get(PROP_PROTOCOL) instanceof String) {
            return (String) m_properties.get(PROP_PROTOCOL);
        }
        return PROP_DEFAULT_PROTOCOL;
    }
        
    /**
     * Returns the ssl.default.trustStore.
     * @return
     */
    public String getSslTrustStore() {
        if (m_properties != null &&
            m_properties.get(PROP_TRUST_STORE) != null &&
            m_properties.get(PROP_TRUST_STORE) instanceof String) {
            return (String) m_properties.get(PROP_TRUST_STORE);
        }
        return PROP_DEFAULT_TRUST_STORE;
    }
    
    /**
     * Returns the ssl.default.trustStore.
     * @return
     */
    public String getSslCiphers() {
        if (m_properties != null &&
            m_properties.get(PROP_CIPHERS) != null &&
            m_properties.get(PROP_CIPHERS) instanceof String) {
            return (String) m_properties.get(PROP_CIPHERS);
        }
        return null;
    }
    
    /**
     * Returns the ssl.keystore.password.
     * @return
     */
    public String getSslKeystorePassword() {
        if (m_properties != null &&
            m_properties.get(PROP_TRUST_PASSWORD) != null &&
            m_properties.get(PROP_TRUST_PASSWORD) instanceof String) {
            return (String) m_properties.get(PROP_TRUST_PASSWORD);
        }
        return null;
    }
    
    /**
     * Returns the ssl.hostname.verification
     * @return
     */
    public Boolean isSslHostnameVerification() {
    	if (m_properties != null &&
            m_properties.get(PROP_HN_VERIFY) != null &&
            m_properties.get(PROP_HN_VERIFY) instanceof Boolean) {
    		return (Boolean) m_properties.get(PROP_HN_VERIFY);
    	}
    	return PROP_DEFAULT_HN_VERIFY;
    }
    
    public String getPropDefaultTrustStore(){
    	return PROP_DEFAULT_TRUST_STORE;
    }

	public String getPropTrustStore() {
		return PROP_TRUST_STORE;
	}

	public String getPropTrustPassword() {
		return PROP_TRUST_PASSWORD;
	}
    
}
