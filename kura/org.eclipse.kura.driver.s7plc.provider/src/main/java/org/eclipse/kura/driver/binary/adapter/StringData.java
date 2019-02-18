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
        return this.wrapped.getEndianness();
    }

    @Override
    public int getSize() {
        return this.wrapped.getSize();
    }

    @Override
    public void write(Buffer buf, int offset, String value) {
        final byte[] raw = value.getBytes(this.charset);
        int amount = this.wrapped.getSize();
        int size = value.length();
        byte[] sendByte = new byte[amount + 2];
        sendByte[0] = (byte) amount;
        sendByte[1] = (byte) size;
        System.arraycopy(raw, 0, sendByte, 2, size);

        this.wrapped.write(buf, offset, sendByte);
    }

    @Override
    public String read(Buffer buf, int offset) {
        final byte[] raw = this.wrapped.read(buf, offset);
        // int size = raw[1]; // Current length of the string
        return new String(raw, 2, raw.length - 2, this.charset);
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

}
