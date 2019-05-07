/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     
 *******************************************************************************/
package org.eclipse.kura.http.server.manager;

import java.util.Map;

public class HttpServiceOptions {

    public static final Property<Boolean> HTTP_ENABLED = new Property<>("http.enabled", true);
    public static final Property<Integer> HTTP_PORT = new Property<>("http.port", 80);
    public static final Property<Boolean> HTTPS_ENABLED = new Property<>("https.enabled", false);
    private static final Property<Integer> HTTPS_PORT = new Property<>("https.port", 443);
    private static final Property<String> HTTPS_KEYSTORE_PATH = new Property<>("https.keystore.path", "/opt/eclipse/kura/user/security/httpskeystore.ks");
    private static final Property<String> HTTPS_KEYSTORE_PASSWORD = new Property<>("https.keystore.password", "changeit");

    private final boolean httpEnabled;
    private final int httpPort;
    private final boolean httpsEnabled;
    private final int httpsPort;
    private final String httpsKeystorePath;
    private final char[] httpsKeystorePasswordArray;

    public HttpServiceOptions(final Map<String, Object> properties) {
        this.httpEnabled = HTTP_ENABLED.get(properties);
        this.httpPort = HTTP_PORT.get(properties);
        this.httpsEnabled = HTTPS_ENABLED.get(properties);
        this.httpsPort = HTTPS_PORT.get(properties);
        this.httpsKeystorePath = HTTPS_KEYSTORE_PATH.get(properties);
        this.httpsKeystorePasswordArray = HTTPS_KEYSTORE_PASSWORD.get(properties).toCharArray();
    }

    public boolean isHttpEnabled() {
        return this.httpEnabled;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public boolean isHttpsEnabled() {
        return this.httpsEnabled;
    }

    public int getHttpsPort() {
        return this.httpsPort;
    }

    public String getHttpsKeystorePath() {
        return this.httpsKeystorePath;
    }

    public char[] getHttpsKeystorePassword() {
        return this.httpsKeystorePasswordArray;
    }
}
