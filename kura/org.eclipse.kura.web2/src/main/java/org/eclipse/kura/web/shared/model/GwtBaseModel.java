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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

@SuppressWarnings("unused")
public class GwtBaseModel implements IsSerializable, Serializable {

    private static final long serialVersionUID = -4890171188631895494L;

    // Unused members needed for GWT serialization
    private Date _date;
    private Integer _integer;
    private Boolean _boolean;

    protected HashMap<String, Object> data;

    public GwtBaseModel() {
        this.data = new HashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    public <X> X get(String key) {
        return (X) this.data.get(key);
    }

    public void set(String key, Object value) {
        this.data.put(key, value);
    }

    public void setProperties(Map<String, Object> properties) {
        for (String property : properties.keySet()) {
            set(property, properties.get(property));
        }
    }

    public Map<String, Object> getProperties() {
        return this.data;
    }

}
