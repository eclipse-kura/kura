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
