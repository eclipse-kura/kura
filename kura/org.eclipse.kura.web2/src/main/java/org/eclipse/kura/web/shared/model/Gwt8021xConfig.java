/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gwt8021xConfig extends KuraBaseModel implements Serializable {

    private static final String KEY_EAP = "eap";
    private static final String KEY_INNER_AUTH = "innerAuth";
    private static final Logger logger = Logger.getLogger(Gwt8021xConfig.class.getSimpleName());
    private static final long serialVersionUID = 7079533925979145804L;

    public Gwt8021xEap getEapEnum() {
        return Gwt8021xEap.fromMetatypeString(getEap());
    }

    public String getEap() {
        return get(KEY_EAP);
    }

    public void setEap(String eap) {
        set(KEY_EAP, eap);
    }

    public void setEap(Gwt8021xEap eap) {
        set(KEY_EAP, eap.getLabel());
    }

    public Gwt8021xInnerAuth getInnerAuthEnum() {
        return Gwt8021xInnerAuth.fromMetatypeString(getInnerAuth());
    }

    public String getInnerAuth() {
        return get(KEY_INNER_AUTH);
    }

    public void setInnerAuth(String innerAuth) {
        set(KEY_INNER_AUTH, innerAuth);
    }

    public void setInnerAuthEnum(Gwt8021xInnerAuth innerAuth) {
        set(KEY_INNER_AUTH, innerAuth.getLabel());
    }

    public String getUsername() {
        return get("username");
    }

    public void setIdentity(String username) {
        set("username", username);
    }

    public String getPassword() {
        return get("password");
    }

    public void setPassword(String password) {
        set("password", password);
    }

    public String getKeystorePid() {
        return get("keystorePid");
    }

    public void setKeystorePid(String keystorePid) {
        set("keystorePid", keystorePid);
    }

    public String getCaCertName() {
        return get("caCertName");
    }

    public void setCaCertName(String caCertName) {
        set("caCertName", caCertName);
    }

    public String getPublicPrivateKeyPairName() {
        return get("privateKeyName");
    }

    public void setPublicPrivateKeyPairName(String privateKeyName) {
        set("privateKeyName", privateKeyName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Gwt8021xConfig)) {
            return false;
        }

        Map<String, Object> properties = getProperties();
        Map<String, Object> otherProps = ((Gwt8021xConfig) o).getProperties();

        if (properties != null) {
            if (otherProps == null) {
                return false;
            }
            if (properties.size() != otherProps.size()) {
                logger.log(Level.FINER, "Sizes differ");
                return false;
            }

            for (Entry<String, Object> entry : properties.entrySet()) {
                final Object oldVal = entry.getValue();
                final Object newVal = otherProps.get(entry.getKey());
                if (oldVal != null) {
                    if (!oldVal.equals(newVal)) {
                        logger.log(Level.FINER, () -> "Values differ - Key: " + entry.getKey() + " oldVal: " + oldVal
                                + ", newVal: " + newVal);
                        return false;
                    }
                } else if (newVal != null) {
                    return false;
                }
            }
        } else if (otherProps != null) {
            return false;
        }

        return true;
    }
}
