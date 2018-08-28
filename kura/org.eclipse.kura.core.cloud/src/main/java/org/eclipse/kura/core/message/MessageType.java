/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.message;

public enum MessageType {

    DATA("data"),
    CONTROL("control");

    private String type;

    private MessageType(String type) {
        this.type = type;
    }

    public String value() {
        return this.type;
    }

    public static MessageType fromValue(String v) {
        for (MessageType mt : MessageType.values()) {
            if (mt.type.equals(v)) {
                return mt;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
