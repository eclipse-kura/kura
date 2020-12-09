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

public class GwtChannelRecord extends KuraBaseModel implements Serializable {

    /** Serialization UUID */
    private static final long serialVersionUID = 2188123225288791202L;
    private StackTraceElement[] stackTrace;

    public String getName() {
        return super.get("name");
    }

    public String getValueType() {
        return super.get("valueType");
    }

    public void setName(final String name) {
        super.set("name", name);
    }

    public void setValueType(final String valueType) {
        super.set("valueType", valueType);
    }

    public String getValue() {
        return super.get("value");
    }

    public void setValue(final String value) {
        super.set("value", value);
    }

    public void setExceptionMessage(final String exceptionMessage) {
        super.set("exceptionMessage", exceptionMessage);
    }

    public String getExceptionMessage() {
        return super.get("exceptionMessage");
    }

    public void setExceptionStackTrace(final StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    public StackTraceElement[] getExceptionStackTrace() {
        return this.stackTrace;
    }
}
