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

class UInt16 extends AbstractBinaryData<Integer> {

    public UInt16(Endianness endianness) {
        super(endianness, 2);
    }

    @Override
    public void write(Buffer buf, int offset, Integer value) {
        if (this.endianness == Endianness.BIG_ENDIAN) {
            buf.put(offset, (byte) (value >> 8 & 0xff));
            buf.put(offset + 1, (byte) (value & 0xff));
        } else {
            buf.put(offset, (byte) (value & 0xff));
            buf.put(offset + 1, (byte) (value >> 8 & 0xff));
        }
    }

    @Override
    public Integer read(Buffer buf, int offset) {
        int result;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            result = buf.get(offset + 1) & 0xff;
            result |= (buf.get(offset) & 0xff) << 8;
        } else {
            result = buf.get(offset) & 0xff;
            result |= (buf.get(offset + 1) & 0xff) << 8;
        }
        return result;
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }
}
