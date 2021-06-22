/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
import java.util.Date;

public class GwtEventInfo extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = 5806274412665387619L;
    public static final String CONCURRENT_WRITE_EVENT_SESSION = "session";
    public static final String CONCURRENT_WRITE_EVENT_MODIFIED_COMPONENT = "component";

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
