/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
