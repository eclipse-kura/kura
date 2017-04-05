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
import jdk.dio.*;
import jdk.dio.spi.*;

/*
 * MCP3008Provider
 * Provider class definition for connecting to an MCP3008 ADC chip over the Serial
 * Peripheral Interface. Part of a sample Service Provider implementation.
*/
public class MCP3008Provider implements DeviceProvider<MCP3008> {

    public MCP3008Provider() {
    }

    @Override
    public  AbstractDevice<? super MCP3008> open(DeviceConfig<? super MCP3008> config, String[] properties, int mode)
            throws IOException, DeviceException {
        MCP3008Impl impl = new MCP3008Impl(config);
        impl.open();
        return impl;
    }

    @Override
    public Class<MCP3008> getType() {
        return MCP3008.class;
    }

    @Override
    public Class<MCP3008Config> getConfigType() {
        return MCP3008Config.class;
    }

    @Override
    public boolean matches(String[] properties) {
        if (properties == null | properties.length == 0) {
            return true;
        }
        return false;
    }
}
