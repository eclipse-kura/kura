/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.binary;

class UInt32 extends AbstractBinaryData<Long> {

    public UInt32(Endianness endianness) {
        super(endianness, 4);
    }

    @Override
    public void write(Buffer buf, int offset, Long value) {
        if (this.endianness == Endianness.BIG_ENDIAN) {
            buf.put(offset, (byte) (value >> 24 & 0xff));
            buf.put(offset + 1, (byte) (value >> 16 & 0xff));
            buf.put(offset + 2, (byte) (value >> 8 & 0xff));
            buf.put(offset + 3, (byte) (value & 0xff));
        } else {
            buf.put(offset, (byte) (value & 0xff));
            buf.put(offset + 1, (byte) (value >> 8 & 0xff));
            buf.put(offset + 2, (byte) (value >> 16 & 0xff));
            buf.put(offset + 3, (byte) (value >> 24 & 0xff));
        }
    }

    @Override
    public Long read(Buffer buf, int offset) {
        long result;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            result = (long) buf.get(offset + 3) & 0xff;
            result |= (long) (buf.get(offset + 2) & 0xff) << 8;
            result |= (long) (buf.get(offset + 1) & 0xff) << 16;
            result |= (long) (buf.get(offset) & 0xff) << 24;
        } else {
            result = buf.get(offset) & 0xff;
            result |= (long) (buf.get(offset + 1) & 0xff) << 8;
            result |= (long) (buf.get(offset + 2) & 0xff) << 16;
            result |= (long) (buf.get(offset + 3) & 0xff) << 24;
        }
        return result;
    }

    @Override
    public Class<Long> getValueType() {
        return Long.class;
    }
}
