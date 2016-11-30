/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
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
import java.util.Date;

public class GwtEventInfo extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = 5806274412665387619L;

    public GwtEventInfo() {
    }

    public GwtEventInfo(String topic) {
        set("timestamp", Long.toString(new Date().getTime()));
        set("topic", topic);
    }

    public String getTimestamp() {
        return get("timestamp");
    }

    public String getTopic() {
        return get("topic");
    }

    public String toString() { // TODO remove me
        return this.data.toString();
    }
}
