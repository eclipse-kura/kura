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
        int rawLength = raw.length;
        int totalSize = rawLength;
        if (totalSize <= 2)
            return "";
        int i = 0;
        int totalByteSize = raw[i];
        int size = raw[i + 1]; // Current length of the string
        int length = Math.min(size, totalSize - 2);
        StringBuilder builder = new StringBuilder();
        while (totalSize > 0) {
            if (length > 0)
                builder.append(new String(raw, i + 2, length, this.charset));
            i = i + totalByteSize + 2;
            totalSize = rawLength - i;
            if (totalSize <= 2)
                break;
            totalByteSize = raw[i];
            size = raw[i + 1];
            if (totalByteSize < size)
                break;
            length = Math.min(size, totalSize - 2);
            if (length > 0) {
                int j = i + length + 2;
                if ((rawLength - j) > 2)
                    builder.append('_');
                else
                    break;
            }
        }
        return builder.toString();
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

}
