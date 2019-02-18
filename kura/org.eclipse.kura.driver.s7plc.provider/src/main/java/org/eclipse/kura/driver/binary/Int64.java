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

class Int64 extends AbstractBinaryData<Long> {

    public Int64(Endianness endianness) {
        super(endianness, 8);
    }

    @Override
    public void write(Buffer buf, int offset, Long value) {
        if (this.endianness == Endianness.BIG_ENDIAN) {
            buf.put(offset, (byte) (value >> 56 & 0xff));
            buf.put(offset + 1, (byte) (value >> 48 & 0xff));
            buf.put(offset + 2, (byte) (value >> 40 & 0xff));
            buf.put(offset + 3, (byte) (value >> 32 & 0xff));
            buf.put(offset + 4, (byte) (value >> 24 & 0xff));
            buf.put(offset + 5, (byte) (value >> 16 & 0xff));
            buf.put(offset + 6, (byte) (value >> 8 & 0xff));
            buf.put(offset + 7, (byte) (value & 0xff));
        } else {
            buf.put(offset, (byte) (value & 0xff));
            buf.put(offset + 1, (byte) (value >> 8 & 0xff));
            buf.put(offset + 2, (byte) (value >> 16 & 0xff));
            buf.put(offset + 3, (byte) (value >> 24 & 0xff));
            buf.put(offset + 4, (byte) (value >> 32 & 0xff));
            buf.put(offset + 5, (byte) (value >> 40 & 0xff));
            buf.put(offset + 6, (byte) (value >> 48 & 0xff));
            buf.put(offset + 7, (byte) (value >> 56 & 0xff));
        }
    }

    @Override
    public Long read(Buffer buf, int offset) {
        long result;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            result = buf.get(offset + 7) & 0xffL;
            result |= (buf.get(offset + 6) & 0xffL) << 8;
            result |= (buf.get(offset + 5) & 0xffL) << 16;
            result |= (buf.get(offset + 4) & 0xffL) << 24;
            result |= (buf.get(offset + 3) & 0xffL) << 32;
            result |= (buf.get(offset + 2) & 0xffL) << 40;
            result |= (buf.get(offset + 1) & 0xffL) << 48;
            result |= (buf.get(offset) & 0xffL) << 56;
        } else {
            result = buf.get(offset) & 0xffL;
            result |= (buf.get(offset + 1) & 0xffL) << 8;
            result |= (buf.get(offset + 2) & 0xffL) << 16;
            result |= (buf.get(offset + 3) & 0xffL) << 24;
            result |= (buf.get(offset + 4) & 0xffL) << 32;
            result |= (buf.get(offset + 5) & 0xffL) << 40;
            result |= (buf.get(offset + 6) & 0xffL) << 48;
            result |= (buf.get(offset + 7) & 0xffL) << 56;
        }
        return result;
    }

    @Override
    public Class<Long> getValueType() {
        return Long.class;
    }
}