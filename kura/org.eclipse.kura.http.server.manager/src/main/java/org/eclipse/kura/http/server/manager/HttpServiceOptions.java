/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.http.server.manager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.util.configuration.Property;

public class HttpServiceOptions {

    public enum RevocationCheckMode {
        PREFER_OCSP,
        PREFER_CRL,
        CRL_ONLY
    }

    static final String PROP_HTTP_PORTS = "http.ports";
    static final String PROP_HTTPS_PORTS = "https.ports";
    static final String PROP_HTTPS_CLIENT_AUTH_PORTS = "https.client.auth.ports";

    static final String PROP_REVOCATION_ENABLED = "https.revocation.check.enabled";
    static final String PROP_REVOCATION_MODE = "ssl.revocation.mode";
    static final String PROP_CRL_PATH = "https.client.crl.path";
    static final String PROP_REVOCATION_SOFT_FAIL = "https.client.revocation.soft.fail";

    static final String PROP_KEYSTORE_SERVICE = "KeystoreService.target";

    private static final Property<Integer[]> HTTP_PORTS = new Property<>(PROP_HTTP_PORTS, new Integer[] {});
    private static final Property<Integer[]> HTTPS_PORTS = new Property<>(PROP_HTTPS_PORTS, new Integer[] {});
    private static final Property<Integer[]> HTTPS_CLIENT_AUTH_PORTS = new Property<>(PROP_HTTPS_CLIENT_AUTH_PORTS,
            new Integer[] {});
    private static final Property<Boolean> REVOCATION_ENABLED = new Property<>(PROP_REVOCATION_ENABLED, false);
    private static final Property<String> REVOCATION_MODE = new Property<>(PROP_REVOCATION_MODE,
            RevocationCheckMode.PREFER_OCSP.name());
    private static final Property<Boolean> REVOCATION_SOFT_FAIL = new Property<>(PROP_REVOCATION_SOFT_FAIL, false);
    private static final Property<String> KEYSTORE_SERVICE = new Property<>(PROP_KEYSTORE_SERVICE,
            "kura.service.pid=changeit");

    private final Set<Integer> httpPorts;
    private final Set<Integer> httpsPorts;
    private final Set<Integer> httpsWithClientAuthPorts;
    private final boolean isRevocationEnabled;
    private final RevocationCheckMode revocationCheckMode;
    private final boolean isRevocationSoftFailEnabled;
    private final String keystoreServicePid;

    public HttpServiceOptions(final Map<String, Object> properties, final String kuraHome) {
        this.httpPorts = loadIntArrayProperty(HTTP_PORTS.get(properties));
        this.httpsPorts = loadIntArrayProperty(HTTPS_PORTS.get(properties));
        this.httpsWithClientAuthPorts = loadIntArrayProperty(HTTPS_CLIENT_AUTH_PORTS.get(properties));
        this.isRevocationEnabled = REVOCATION_ENABLED.get(properties);
        this.revocationCheckMode = RevocationCheckMode.valueOf(REVOCATION_MODE.get(properties));
        this.isRevocationSoftFailEnabled = REVOCATION_SOFT_FAIL.get(properties);

        this.keystoreServicePid = KEYSTORE_SERVICE.get(properties);
    }

    public Set<Integer> getHttpPorts() {
        return this.httpPorts;
    }

    public Set<Integer> getHttpsPorts() {
        return this.httpsPorts;
    }

    public Set<Integer> getHttpsClientAuthPorts() {
        return this.httpsWithClientAuthPorts;
    }

    public boolean isRevocationEnabled() {
        return this.isRevocationEnabled;
    }

    public RevocationCheckMode getRevocationCheckMode() {
        return this.revocationCheckMode;
    }

    public String getKeystoreServicePid() {
        return this.keystoreServicePid;
    }

    public boolean isRevocationSoftFailEnabled() {
        return this.isRevocationSoftFailEnabled;
    }

    private static Set<Integer> loadIntArrayProperty(final Integer[] list) {
        if (list == null) {
            return Collections.emptySet();
        }

        final Set<Integer> result = new HashSet<>();

        for (final Integer value : list) {
            if (value != null) {
                result.add(value);
            }
        }

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpPorts, httpsPorts, httpsWithClientAuthPorts, isRevocationEnabled,
                isRevocationSoftFailEnabled, keystoreServicePid, revocationCheckMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HttpServiceOptions)) {
            return false;
        }
        HttpServiceOptions other = (HttpServiceOptions) obj;
        return Objects.equals(httpPorts, other.httpPorts) && Objects.equals(httpsPorts, other.httpsPorts)
                && Objects.equals(httpsWithClientAuthPorts, other.httpsWithClientAuthPorts)
                && isRevocationEnabled == other.isRevocationEnabled
                && isRevocationSoftFailEnabled == other.isRevocationSoftFailEnabled
                && Objects.equals(keystoreServicePid, other.keystoreServicePid)
                && revocationCheckMode == other.revocationCheckMode;
    }
}
