/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtCertificate extends GwtBaseModel implements IsSerializable, Serializable {

    private static final long serialVersionUID = 5876379937604300640L;

    public GwtCertificate() {
    }

    public String getAlias() {
        return get("alias");
    }

    public GwtCertificateType getType() {
        return GwtCertificateType.getCertificateType(get("type"));
    }

    public void setAlias(String alias) {
        set("alias", alias);
    }

    public void setType(String type) {
        set("type", type);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GwtNetInterfaceConfig)) {
            return false;
        }

        Map<String, Object> properties = getProperties();
        Map<String, Object> otherProps = ((GwtCertificate) o).getProperties();

        if (properties != null) {
            if (otherProps == null) {
                return false;
            }
            if (properties.size() != otherProps.size()) {
                return false;
            }

            for (Entry<String, Object> entry : properties.entrySet()) {
                final Object oldVal = entry.getValue();
                final Object newVal = otherProps.get(entry.getKey());
                if (oldVal != null) {
                    if (!oldVal.equals(newVal)) {
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
