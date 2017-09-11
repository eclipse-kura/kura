/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.eddystone;

public enum EddystoneFrameType {

    UID((byte) 0x00),
    URL((byte) 0x10),
    TLM((byte) 0x20),
    EID((byte) 0x30),
    RESERVED((byte) 0x40);

    private final byte frameType;

    private EddystoneFrameType(byte frameType) {
        this.frameType = frameType;
    }

    public byte getFrameTypeCode() {
        return this.frameType;
    }

    public static EddystoneFrameType valueOf(byte frameType) {
        EddystoneFrameType type = null;
        if (frameType == UID.getFrameTypeCode()) {
            type = UID;
        } else if (frameType == URL.getFrameTypeCode()) {
            type = URL;
        } else if (frameType == TLM.getFrameTypeCode()) {
            type = TLM;
        } else if (frameType == EID.getFrameTypeCode()) {
            type = EID;
        } else if (frameType == RESERVED.getFrameTypeCode()) {
            type = RESERVED;
        }
        return type;
    }

}
