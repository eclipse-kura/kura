/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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

public class GwtKeystoreEntry extends GwtBaseModel implements IsSerializable, Serializable {

    private static final long serialVersionUID = 5876379937604300640L;

    public enum Kind {
        TRUSTED_CERT,
        KEY_PAIR,
        SECRET_KEY
    }

    public GwtKeystoreEntry() {
    }

    public GwtKeystoreEntry(final String alias, final String keystoreName, final Kind kind) {
        set("alias", alias);
        set("keystoreName", keystoreName);
        set("kind", kind.toString());
    }

    public String getAlias() {
        return get("alias");
    }

    public String getKeystoreName() {
        return get("keystoreName");
    }

    public Kind getKind() {
        return Kind.valueOf(get("kind"));
    }
}
