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

import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;

public class KuraBaseModel extends GwtBaseModel {

    /**
     *
     */
    private static final long serialVersionUID = -2502956403624362215L;

    private transient boolean unescaped;

    public KuraBaseModel() {
        super();
    }

    @Override
    public void set(String name, Object value) {
        if (value instanceof String) {
            value = GwtSafeHtmlUtils.inputSanitize((String) value);
        }
        super.set(name, value);
    }

    public void setUnescaped(boolean unescaped) {
        this.unescaped = unescaped;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X get(String key) {
        if (this.unescaped) {
            Object value = super.get(key);
            if (value instanceof String) {
                return (X) GwtSafeHtmlUtils.htmlUnescape((String) value);
            }
            return (X) value;
        } else {
            return (X) super.get(key);
        }
    }

}
