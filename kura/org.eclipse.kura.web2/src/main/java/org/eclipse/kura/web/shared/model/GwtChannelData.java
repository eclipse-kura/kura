/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtChannelData extends GwtChannelInfo implements Serializable {

    private static final long serialVersionUID = -5163824847650444028L;

    public String getValue() {
        return super.get("value");
    }

    public void setValue(final String value) {
        super.set("value", value);
    }
}
