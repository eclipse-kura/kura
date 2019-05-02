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
        wrapped.write(buf, offset, raw);
    }

    @Override
    public String read(Buffer buf, int offset) {
        final byte[] raw = wrapped.read(buf, offset);
        return new String(raw, charset);
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

}
