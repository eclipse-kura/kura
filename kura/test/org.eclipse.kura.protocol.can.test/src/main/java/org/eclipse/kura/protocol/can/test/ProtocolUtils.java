/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.protocol.can.test;

public class ProtocolUtils {

    public static byte getSigned8(byte[] buf, int offset) {
        return buf[offset];
    }

    public static short getUnsigned8(byte[] buf, int offset) {
        return (short) (buf[offset] & 0xff);
    }

    public static short getSigned16(byte[] buf, int offset) {
        short t = (short) (buf[offset] & 0xff | (buf[offset + 1] & 0xff) << 8);
        return t;
    }

    public static int getUnsigned16(byte[] buf, int offset) {
        short t = getSigned16(buf, offset);
        return t & 0xffff;
    }

    public static int getSigned32(byte[] buf, int offset) {
        int i = buf[offset] & 0xff | (buf[offset + 1] & 0xff) << 8 | (buf[offset + 2] & 0xff) << 16
                | (buf[offset + 3] & 0xff) << 24;
        return i;
    }

    public static long getUnsigned32(byte[] buf, int offset) {
        int i = getSigned32(buf, offset);
        return i & 0xffffffffL;
    }
}
