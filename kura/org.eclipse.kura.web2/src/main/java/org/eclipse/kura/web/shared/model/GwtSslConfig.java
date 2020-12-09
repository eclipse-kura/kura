/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;

public class GwtSslConfig extends GwtBaseModel implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -672250310856608091L;

    public GwtSslConfig() {
        super();
    }

    @Override
    public void set(String name, Object value) {
        if (value instanceof String) {
            value = GwtSafeHtmlUtils.htmlEscape((String) value);
        }
        super.set(name, value);
    }

    public String getProtocol() {
        return get("sslProtocol");
    }

    public void setProtocol(String protocol) {
        set("sslProtocol", protocol);
    }

    public String getKeyStore() {
        return get("sslKeyStore");
    }

    public void setKeyStore(String keyStore) {
        set("sslKeyStore", keyStore);
    }

    public String getCiphers() {
        return get("sslCiphers");
    }

    public void setCiphers(String ciphers) {
        set("sslCiphers", ciphers);
    }

    public String getKeystorePassword() {
        return get("sslKeyStorePassword");
    }

    public void setKeystorePassword(String keystorePassword) {
        set("sslKeyStorePassword", keystorePassword);
    }

    public boolean isHostnameVerification() {
        if (get("sslHostnameVerification") != null) {
            return (Boolean) get("sslHostnameVerification");
        }
        return false;
    }

    public void setHostnameVerification(boolean hostnameVerification) {
        set("sslHostnameVerification", hostnameVerification);
    }
}
