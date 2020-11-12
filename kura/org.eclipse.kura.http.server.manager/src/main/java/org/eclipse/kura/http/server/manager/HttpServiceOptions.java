/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.util.configuration.Property;

public class HttpServiceOptions {

    static final String PROP_HTTP_ENABLED = "http.enabled";
    static final String PROP_HTTP_PORT = "http.port";
    static final String PROP_HTTPS_ENABLED = "https.enabled";
    static final String PROP_HTTPS_PORT = "https.port";
    static final String PROP_HTTPS_CLIENT_AUTH_ENABLED = "https.client.auth.enabled";
    static final String PROP_HTTPS_CLIENT_AUTH_PORT = "https.client.auth.port";

    static final String PROP_HTTPS_KEYSTORE_PATH = "https.keystore.path";
    static final String PROP_HTTPS_KEYSTORE_PASSWORD = "https.keystore.password";

    static final String DEFAULT_HTTPS_KEYSTORE_PASSWORD = "changeit";

    static final String PROP_REVOCATION_ENABLED = "https.revocation.check.enabled";
    static final String PROP_OCSP_URI = "https.client.ocsp.url";
    static final String PROP_CRL_PATH = "https.client.crl.path";
    static final String PROP_REVOCATION_SOFT_FAIL = "https.client.revocation.soft.fail";

    private static final Property<Boolean> HTTP_ENABLED = new Property<>(PROP_HTTP_ENABLED, true);
    private static final Property<Integer> HTTP_PORT = new Property<>(PROP_HTTP_PORT, 80);
    private static final Property<Boolean> HTTPS_ENABLED = new Property<>(PROP_HTTPS_ENABLED, false);
    private static final Property<Integer> HTTPS_PORT = new Property<>(PROP_HTTPS_PORT, 443);
    private static final Property<Boolean> HTTPS_CLIENT_AUTH_ENABLED = new Property<>(PROP_HTTPS_CLIENT_AUTH_ENABLED,
            false);
    private static final Property<Integer> HTTPS_CLIENT_AUTH_PORT = new Property<>(PROP_HTTPS_CLIENT_AUTH_PORT, 4443);
    private static final Property<String> HTTPS_KEYSTORE_PASSWORD = new Property<>(PROP_HTTPS_KEYSTORE_PASSWORD,
            DEFAULT_HTTPS_KEYSTORE_PASSWORD);
    private static final Property<Boolean> REVOCATION_ENABLED = new Property<>(PROP_REVOCATION_ENABLED, false);
    private static final Property<String> OCSP_URI = new Property<>(PROP_OCSP_URI, String.class);
    private static final Property<String> CRL_PATH = new Property<>(PROP_CRL_PATH, String.class);
    private static final Property<Boolean> REVOCATION_SOFT_FAIL = new Property<>(PROP_REVOCATION_SOFT_FAIL, false);

    private final boolean httpEnabled;
    private final int httpPort;
    private final boolean httpsEnabled;
    private final int httpsPort;
    private final boolean httpsClientAuthEnabled;
    private final int httpsClientAuthPort;
    private final String httpsKeystorePath;
    private final char[] httpsKeystorePasswordArray;
    private final boolean isRevocationEnabled;
    private final Optional<String> ocspUri;
    private final Optional<String> crlPath;
    private final boolean isRevocationSoftFailEnabled;

    public HttpServiceOptions(final Map<String, Object> properties, final String kuraHome) {
        Property<String> httpsKeystorePathProp = new Property<>(PROP_HTTPS_KEYSTORE_PATH,
                kuraHome + "/user/security/httpskeystore.ks");

        this.httpEnabled = HTTP_ENABLED.get(properties);
        this.httpPort = HTTP_PORT.get(properties);
        this.httpsEnabled = HTTPS_ENABLED.get(properties);
        this.httpsPort = HTTPS_PORT.get(properties);
        this.httpsClientAuthEnabled = HTTPS_CLIENT_AUTH_ENABLED.get(properties);
        this.httpsClientAuthPort = HTTPS_CLIENT_AUTH_PORT.get(properties);
        this.httpsKeystorePath = httpsKeystorePathProp.get(properties);
        this.httpsKeystorePasswordArray = HTTPS_KEYSTORE_PASSWORD.get(properties).toCharArray();
        this.isRevocationEnabled = REVOCATION_ENABLED.get(properties);
        this.ocspUri = OCSP_URI.getOptional(properties);
        this.crlPath = CRL_PATH.getOptional(properties);
        this.isRevocationSoftFailEnabled = REVOCATION_SOFT_FAIL.get(properties);
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

    public boolean isHttpsClientAuthEnabled() {
        return this.httpsClientAuthEnabled;
    }

    public int getHttpsClientAuthPort() {
        return this.httpsClientAuthPort;
    }

    public String getHttpsKeystorePath() {
        return this.httpsKeystorePath;
    }

    public char[] getHttpsKeystorePassword() {
        return this.httpsKeystorePasswordArray;
    }

    public boolean isRevocationEnabled() {
        return this.isRevocationEnabled;
    }

    public Optional<String> getOcspURI() {
        return this.ocspUri;
    }

    public Optional<String> getCrlPath() {
        return this.crlPath;
    }

    public boolean isRevocationSoftFailEnabled() {
        return this.isRevocationSoftFailEnabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(httpsKeystorePasswordArray);
        result = prime * result + Objects.hash(crlPath, httpEnabled, httpPort, httpsEnabled, httpsKeystorePath,
                httpsPort, isRevocationEnabled, isRevocationSoftFailEnabled, ocspUri);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HttpServiceOptions other = (HttpServiceOptions) obj;
        return Objects.equals(crlPath, other.crlPath) && httpEnabled == other.httpEnabled && httpPort == other.httpPort
                && httpsEnabled == other.httpsEnabled
                && Arrays.equals(httpsKeystorePasswordArray, other.httpsKeystorePasswordArray)
                && Objects.equals(httpsKeystorePath, other.httpsKeystorePath) && httpsPort == other.httpsPort
                && isRevocationEnabled == other.isRevocationEnabled
                && isRevocationSoftFailEnabled == other.isRevocationSoftFailEnabled
                && Objects.equals(ocspUri, other.ocspUri);
    }

}
