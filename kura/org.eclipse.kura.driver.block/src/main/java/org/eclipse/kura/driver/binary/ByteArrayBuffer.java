/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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

public class ByteArrayBuffer implements Buffer {

    private final byte[] data;

    public ByteArrayBuffer(byte[] data) {
        this.data = data;
    }

    @Override
    public void put(int offset, byte value) {
        this.data[offset] = value;
    }

    @Override
    public byte get(int offset) {
        return this.data[offset];
    }

    @Override
    public int getLength() {
        return this.data.length;
    }

    @Override
    public void write(int offset, int length, byte[] data) {
        System.arraycopy(data, 0, this.data, offset, length);
    }

    @Override
    public void read(int offset, int length, byte[] data) {
        System.arraycopy(this.data, offset, data, 0, length);
    }

    public byte[] getBackingArray() {
        return this.data;
    }
}
