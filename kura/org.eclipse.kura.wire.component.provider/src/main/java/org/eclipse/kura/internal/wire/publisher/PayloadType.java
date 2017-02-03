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
package org.eclipse.kura.internal.wire.publisher;

enum PayloadType {
    KURA_PAYLOAD(1),
    JSON(2);

    private int value;

    private PayloadType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PayloadType getPayloadType(int payloadTypeInt) {
        for (PayloadType tempPayloadType : PayloadType.values()) {
            if (tempPayloadType.getValue() == payloadTypeInt) {
                return tempPayloadType;
            }
        }
        throw new IllegalArgumentException("Payload type not available!");
    }
}
