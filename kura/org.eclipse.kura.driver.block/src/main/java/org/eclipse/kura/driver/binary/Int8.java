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

class Int8 extends AbstractBinaryData<Integer> {

    public Int8() {
        super(Endianness.BIG_ENDIAN, 1);
    }

    @Override
    public void write(Buffer buf, int offset, Integer value) {
        buf.put(offset, (byte) (int) value);
    }

    @Override
    public Integer read(Buffer buf, int offset) {
        return (int) buf.get(offset);
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }
}
