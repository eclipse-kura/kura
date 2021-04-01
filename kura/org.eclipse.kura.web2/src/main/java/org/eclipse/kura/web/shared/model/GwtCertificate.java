/*******************************************************************************
 * Copyright (c) 2020, 2021Eurotech and/or its affiliates and others
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

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtCertificate extends GwtBaseModel implements IsSerializable, Serializable {

    private static final long serialVersionUID = 5876379937604300640L;

    public GwtCertificate() {
    }

    public String getAlias() {
        return get("alias");
    }

    public String getKeystoreName() {
        return get("keystoreName");
    }

    public void setAlias(String alias) {
        set("alias", alias);
    }

    public void setKeystoreName(String keystoreName) {
        set("keystoreName", keystoreName);
    }
}
