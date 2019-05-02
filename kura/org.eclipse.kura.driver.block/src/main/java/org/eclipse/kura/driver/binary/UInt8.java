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

class UInt8 extends AbstractBinaryData<Integer> {

    public UInt8() {
        super(Endianness.BIG_ENDIAN, 1);
    }

    @Override
    public void write(Buffer buf, int offset, Integer value) {
        buf.put(offset, (byte) (value & 0xff));
    }

    @Override
    public Integer read(Buffer buf, int offset) {
        return (int) (buf.get(offset) & 0xff);
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }
}
