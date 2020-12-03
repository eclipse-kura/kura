/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteArray extends AbstractBinaryData<byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(ByteArray.class);

    public ByteArray(int size) {
        super(Endianness.LITTLE_ENDIAN, size);
    }

    @Override
    public void write(Buffer buf, int offset, byte[] value) {

        final int transferSize = getTransferSize(buf, offset);

        buf.write(offset, transferSize, value);
    }

    @Override
    public byte[] read(final Buffer buf, final int offset) {

        final int transferSize = getTransferSize(buf, offset);
        byte[] data = new byte[transferSize];

        buf.read(offset, data);

        return data;
    }

    private int getTransferSize(final Buffer buf, final int offset) {

        final int size = getSize();
        final int bufferAvailable = buf.getLength() - offset;

        if (bufferAvailable < size) {
            logger.debug("received buffer is too small");
        }

        return Math.min(bufferAvailable, size);
    }

    @Override
    public Class<byte[]> getValueType() {
        return byte[].class;
    }

}
