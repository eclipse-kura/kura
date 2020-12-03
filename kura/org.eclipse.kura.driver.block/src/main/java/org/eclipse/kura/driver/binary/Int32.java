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

package org.eclipse.kura.driver.binary;

class Int32 extends AbstractBinaryData<Integer> {

    public Int32(Endianness endianness) {
        super(endianness, 4);
    }

    @Override
    public void write(Buffer buf, int offset, Integer value) {
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
    public Integer read(Buffer buf, int offset) {
        int result;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            result = buf.get(offset + 3) & 0xff;
            result |= (buf.get(offset + 2) & 0xff) << 8;
            result |= (buf.get(offset + 1) & 0xff) << 16;
            result |= (buf.get(offset) & 0xff) << 24;
        } else {
            result = buf.get(offset) & 0xff;
            result |= (buf.get(offset + 1) & 0xff) << 8;
            result |= (buf.get(offset + 2) & 0xff) << 16;
            result |= (buf.get(offset + 3) & 0xff) << 24;
        }
        return result;
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }
}
