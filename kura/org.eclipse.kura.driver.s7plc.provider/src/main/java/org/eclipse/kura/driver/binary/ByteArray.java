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
