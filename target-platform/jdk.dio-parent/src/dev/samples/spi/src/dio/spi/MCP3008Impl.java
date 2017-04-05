/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.

 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dio.spi;

import java.io.*;
import java.nio.*;
import jdk.dio.*;
import jdk.dio.spi.*;
import jdk.dio.spibus.*;

/*
 * MCP3008Impl
 * Implementation class for connecting to an MCP3008 ADC chip over the Serial
 * Peripheral Interface. Part of a sample Service Provider implementation.
*/
public class MCP3008Impl extends AbstractDevice<MCP3008> implements MCP3008 {

    private SPIDevice spiDevice = null;
    private int deviceNumber = -1;

    MCP3008Impl(DeviceConfig<? super MCP3008> config) {
        deviceNumber = ((MCP3008Config)config).getDeviceNumber();
    }

    public int readChannel(int c) {
        if (c >= NUM_CHANNELS) {
            return -1;
        }
        ByteBuffer out = ByteBuffer.allocate(3);
        ByteBuffer in = ByteBuffer.allocate(3);
        out.put((byte)0x01);
        out.put((byte)(((c | 0x08) & 0x0f) << 4));
        out.put((byte)0);
        out.flip();
        try {
            spiDevice.writeAndRead(out, in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int high = (int)(0x0003 & in.get(1));
        int low = (int)(0x00ff & in.get(2));

        return (high << 8) + low;
    }

    void open() throws IOException, UnsupportedDeviceTypeException, DeviceNotFoundException, UnavailableDeviceException {
        spiDevice = (SPIDevice)DeviceManager.open(deviceNumber);
    }

    @Override
    public boolean isOpen() {
        return spiDevice != null && spiDevice.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (spiDevice != null) {
            spiDevice.close();
        }
    }

}
