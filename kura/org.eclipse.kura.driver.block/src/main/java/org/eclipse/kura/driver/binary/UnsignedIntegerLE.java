/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
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

import java.math.BigInteger;

public class UnsignedIntegerLE extends AbstractBinaryData<BigInteger> {

    private final int startBitOffset;
    private final int sizeBits;

    public UnsignedIntegerLE(final int sizeBits, final int startBitOffset) {
        super(Endianness.LITTLE_ENDIAN, getSizeBytes(sizeBits));
        this.startBitOffset = startBitOffset;
        this.sizeBits = sizeBits;
    }

    @Override
    public void write(Buffer buf, int offset, BigInteger value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigInteger read(final Buffer buf, final int offset) {
        final int sizeBytes = getSize();

        final byte[] raw = new byte[sizeBytes];

        int srcBit = offset * 8 + startBitOffset;
        final int srcEnd = srcBit + sizeBits;

        int dstBit = 0;

        while (srcBit < srcEnd) {
            final int srcByte = srcBit / 8;
            final int dstByte = dstBit / 8;

            if ((buf.get(srcByte) & 0xff & (1 << (srcBit % 8))) != 0) {
                raw[dstByte] |= (1 << (dstBit % 8));
            }

            srcBit++;
            dstBit++;
        }

        for (int i = 0; i < sizeBytes / 2; i++) {
            final byte tmp = raw[i];
            raw[i] = raw[sizeBytes - i - 1];
            raw[sizeBytes - i - 1] = tmp;
        }

        return new BigInteger(1, raw);
    }

    @Override
    public Class<BigInteger> getValueType() {
        return BigInteger.class;
    }

    private static int getSizeBytes(final int sizeBits) {
        if (sizeBits <= 0) {
            throw new IllegalArgumentException("bit size must be positive");
        }
        return (int) Math.ceil((double) sizeBits / 8);
    }
}
