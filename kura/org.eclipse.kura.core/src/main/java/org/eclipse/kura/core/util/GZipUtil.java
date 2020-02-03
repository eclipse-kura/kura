/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipUtil {

    private GZipUtil() {

    }

    public static boolean isCompressed(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        } else {
            return bytes[0] == (byte) GZIPInputStream.GZIP_MAGIC
                    && bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
        }
    }

    public static byte[] compress(byte[] source) throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzipos = new GZIPOutputStream(baos);) {
            gzipos.write(source);
            return baos.toByteArray();
        }
    }

    public static byte[] decompress(byte[] source) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteArrayInputStream bais = new ByteArrayInputStream(source);
                GZIPInputStream gzipis = new GZIPInputStream(bais);) {
            int n;
            final int maxBuf = 1024;
            byte[] buf = new byte[maxBuf];
            while ((n = gzipis.read(buf, 0, maxBuf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }
}
