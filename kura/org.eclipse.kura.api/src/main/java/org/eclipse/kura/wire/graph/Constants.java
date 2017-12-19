/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 * 
 *******************************************************************************/
package org.eclipse.kura.wire.graph;

/**
 * Wire Graph related constants.
 * 
 * @since 1.4
 */
public enum Constants {

    WIRE_EMITTER_PORT_PROP_NAME("emitter.port"),
    WIRE_RECEIVER_PORT_PROP_NAME("receiver.port"),
    RECEIVER_PORT_COUNT_PROP_NAME("receiver.port.count"),
    EMITTER_PORT_COUNT_PROP_NAME("emitter.port.count");

    private final String value;
    
    private Constants(String v) {
        this.value = v;
    }
    
    public String value() {
        return this.value;
    }

    public static Constants fromValue(String v) {
        for (Constants c : Constants.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
