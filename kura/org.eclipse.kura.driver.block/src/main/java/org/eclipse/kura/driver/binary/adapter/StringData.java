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
package org.eclipse.kura.driver.binary.adapter;

import java.nio.charset.Charset;

import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.Endianness;

public class StringData implements BinaryData<String> {

    private final BinaryData<byte[]> wrapped;
    private final Charset charset;

    public StringData(final BinaryData<byte[]> wrapped, final Charset charset) {
        this.wrapped = wrapped;
        this.charset = charset;
    }

    @Override
    public Endianness getEndianness() {
        return wrapped.getEndianness();
    }

    @Override
    public int getSize() {
        return wrapped.getSize();
    }

    @Override
    public void write(Buffer buf, int offset, String value) {
        final byte[] raw = value.getBytes(charset);
        int amount = wrapped.getSize();
        int size = value.length();
        byte[] sendByte = new byte[amount + 2];
        sendByte[0] = (byte) amount;
        sendByte[1] = (byte) size;
        System.arraycopy(raw, 0, sendByte, 2, size);

        wrapped.write(buf, offset, sendByte);
    }

    @Override
    public String read(Buffer buf, int offset) {
        final byte[] raw = wrapped.read(buf, offset);
        int size = (int) raw[1]; // Current length of the string
        return new String(raw, 2, size, charset);
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

}
