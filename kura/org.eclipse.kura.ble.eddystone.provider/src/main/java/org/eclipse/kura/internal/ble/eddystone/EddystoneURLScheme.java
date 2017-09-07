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

public enum EddystoneURLScheme {

    HTTPWWW("http://www.", (byte) 0x00),
    HTTPSWWW("https://www.", (byte) 0x01),
    HTTP("http://", (byte) 0x02),
    HTTPS("https://", (byte) 0x03),
    UKNOWN("", (byte) 0x04);

    private final String urlScheme;
    private final byte urlSchemeCode;
    private final int urlSchemeLength;

    private EddystoneURLScheme(String urlScheme, byte urlSchemeCode) {
        this.urlScheme = urlScheme;
        this.urlSchemeCode = urlSchemeCode;
        this.urlSchemeLength = urlScheme.length();
    }

    public byte getUrlSchemeCode() {
        return this.urlSchemeCode;
    }

    public String getUrlScheme() {
        return this.urlScheme;
    }

    public int getLength() {
        return this.urlSchemeLength;
    }

    public static EddystoneURLScheme encodeURLScheme(String url) {
        EddystoneURLScheme scheme;
        if (url.startsWith(HTTPWWW.urlScheme)) {
            scheme = HTTPWWW;
        } else if (url.startsWith(HTTPSWWW.urlScheme)) {
            scheme = HTTPSWWW;
        } else if (url.startsWith(HTTP.urlScheme)) {
            scheme = HTTP;
        } else if (url.startsWith(HTTPS.urlScheme)) {
            scheme = HTTPS;
        } else {
            scheme = UKNOWN;
        }
        return scheme;
    }

    public static String decodeURLScheme(byte scheme) {
        String prefix;
        if (scheme == HTTPWWW.urlSchemeCode) {
            prefix = HTTPWWW.urlScheme;
        } else if (scheme == HTTPSWWW.urlSchemeCode) {
            prefix = HTTPSWWW.urlScheme;
        } else if (scheme == HTTP.urlSchemeCode) {
            prefix = HTTP.urlScheme;
        } else if (scheme == HTTPS.urlSchemeCode) {
            prefix = HTTPS.urlScheme;
        } else {
            prefix = UKNOWN.urlScheme;
        }
        return prefix;
    }
}
