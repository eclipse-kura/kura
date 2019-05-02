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

class Double extends AbstractBinaryData<java.lang.Double> {

    public Double(Endianness endianness) {
        super(endianness, 8);
    }

    @Override
    public void write(Buffer buf, int offset, java.lang.Double d) {
        long tmp = java.lang.Double.doubleToRawLongBits(d);
        if (this.endianness == Endianness.BIG_ENDIAN) {
            buf.put(offset, (byte) (tmp >> 56 & 0xff));
            buf.put(offset + 1, (byte) (tmp >> 48 & 0xff));
            buf.put(offset + 2, (byte) (tmp >> 40 & 0xff));
            buf.put(offset + 3, (byte) (tmp >> 32 & 0xff));
            buf.put(offset + 4, (byte) (tmp >> 24 & 0xff));
            buf.put(offset + 5, (byte) (tmp >> 16 & 0xff));
            buf.put(offset + 6, (byte) (tmp >> 8 & 0xff));
            buf.put(offset + 7, (byte) (tmp & 0xff));
        } else {
            buf.put(offset, (byte) (tmp & 0xff));
            buf.put(offset + 1, (byte) (tmp >> 8 & 0xff));
            buf.put(offset + 2, (byte) (tmp >> 16 & 0xff));
            buf.put(offset + 3, (byte) (tmp >> 24 & 0xff));
            buf.put(offset + 4, (byte) (tmp >> 32 & 0xff));
            buf.put(offset + 5, (byte) (tmp >> 40 & 0xff));
            buf.put(offset + 6, (byte) (tmp >> 48 & 0xff));
            buf.put(offset + 7, (byte) (tmp >> 56 & 0xff));
        }
    }

    @Override
    public java.lang.Double read(Buffer buf, int offset) {
        long tmp;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            tmp = buf.get(offset + 7) & 0xffL;
            tmp |= (buf.get(offset + 6) & 0xffL) << 8;
            tmp |= (buf.get(offset + 5) & 0xffL) << 16;
            tmp |= (buf.get(offset + 4) & 0xffL) << 24;
            tmp |= (buf.get(offset + 3) & 0xffL) << 32;
            tmp |= (buf.get(offset + 2) & 0xffL) << 40;
            tmp |= (buf.get(offset + 1) & 0xffL) << 48;
            tmp |= (buf.get(offset) & 0xffL) << 56;
        } else {
            tmp = buf.get(offset) & 0xffL;
            tmp |= (buf.get(offset + 1) & 0xffL) << 8;
            tmp |= (buf.get(offset + 2) & 0xffL) << 16;
            tmp |= (buf.get(offset + 3) & 0xffL) << 24;
            tmp |= (buf.get(offset + 4) & 0xffL) << 32;
            tmp |= (buf.get(offset + 5) & 0xffL) << 40;
            tmp |= (buf.get(offset + 6) & 0xffL) << 48;
            tmp |= (buf.get(offset + 7) & 0xffL) << 56;
        }
        return java.lang.Double.longBitsToDouble(tmp);
    }

    @Override
    public Class<java.lang.Double> getValueType() {
        return java.lang.Double.class;
    }
}