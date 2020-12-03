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

class Int16 extends AbstractBinaryData<Integer> {

    public Int16(Endianness endianness) {
        super(endianness, 2);
    }

    @Override
    public void write(Buffer buf, int offset, Integer value) {
        final short val = (short) (int) value;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            buf.put(offset, (byte) (val >> 8 & 0xff));
            buf.put(offset + 1, (byte) (val & 0xff));
        } else {
            buf.put(offset, (byte) (val & 0xff));
            buf.put(offset + 1, (byte) (val >> 8 & 0xff));
        }
    }

    @Override
    public Integer read(Buffer buf, int offset) {
        short result;
        if (this.endianness == Endianness.BIG_ENDIAN) {
            result = (short) (buf.get(offset + 1) & 0xff);
            result |= (buf.get(offset) & 0xff) << 8;
        } else {
            result = (short) (buf.get(offset) & 0xff);
            result |= (buf.get(offset + 1) & 0xff) << 8;
        }
        return (int) result;
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }
}
