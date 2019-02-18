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

public interface Buffer {

    public void put(int offset, byte value);

    public byte get(int offset);

    public default void write(int offset, int length, byte[] data) {
        for (int i = 0; i < length; i++) {
            put(offset + i, data[i]);
        }
    }

    public default void write(int offset, byte[] data) {
        write(offset, data.length, data);
    }

    public default void read(int offset, int length, byte[] data) {
        for (int i = 0; i < length; i++) {
            data[i] = get(offset + i);
        }
    }

    public default void read(int offset, byte[] data) {
        read(offset, data.length, data);
    }

    public default byte[] toArray() {
        byte[] result = new byte[getLength()];
        read(0, result);
        return result;
    }

    public int getLength();
}
