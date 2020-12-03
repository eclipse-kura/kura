/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
