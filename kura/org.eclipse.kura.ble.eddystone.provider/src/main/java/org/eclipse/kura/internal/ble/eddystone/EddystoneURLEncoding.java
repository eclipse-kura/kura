/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.eddystone;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EddystoneURLEncoding {

    private static Map<Byte, String> urlEncodings;

    private EddystoneURLEncoding() {
        // Not used
    }

    static {
        urlEncodings = new HashMap<>();
        urlEncodings.put((byte) 0x00, ".com/");
        urlEncodings.put((byte) 0x01, ".org/");
        urlEncodings.put((byte) 0x02, ".edu/");
        urlEncodings.put((byte) 0x03, ".net/");
        urlEncodings.put((byte) 0x04, ".info/");
        urlEncodings.put((byte) 0x05, ".biz/");
        urlEncodings.put((byte) 0x06, ".gov/");
        urlEncodings.put((byte) 0x07, ".com");
        urlEncodings.put((byte) 0x08, ".org");
        urlEncodings.put((byte) 0x09, ".edu");
        urlEncodings.put((byte) 0x0A, ".net");
        urlEncodings.put((byte) 0x0B, ".info");
        urlEncodings.put((byte) 0x0C, ".biz");
        urlEncodings.put((byte) 0x0D, ".gov");
    }

    public static byte[] encodeURL(String url) {
        String hexUrl = String.format("%x", new BigInteger(1, url.getBytes()));
        for (Entry<Byte, String> entry : urlEncodings.entrySet()) {
            hexUrl = hexUrl.replaceAll(String.format("%x", new BigInteger(1, entry.getValue().getBytes())),
                    String.format("%02x", (int) entry.getKey()));
        }
        return urlToByteArray(hexUrl);
    }

    public static String decodeURL(byte[] url) {
        StringBuilder decodedUrl = new StringBuilder();
        for (byte hex : url) {
            if (hex <= 13) {
                decodedUrl.append(urlEncodings.get(hex));
            } else {
                decodedUrl.append((char) hex);
            }
        }
        return decodedUrl.toString();
    }

    private static byte[] urlToByteArray(String url) {
        byte[] urlArray = new byte[url.length() / 2];
        for (int i = 0; i < url.length(); i += 2) {
            urlArray[i
                    / 2] = (byte) ((Character.digit(url.charAt(i), 16) << 4) + Character.digit(url.charAt(i + 1), 16));
        }
        return urlArray;
    }

}
