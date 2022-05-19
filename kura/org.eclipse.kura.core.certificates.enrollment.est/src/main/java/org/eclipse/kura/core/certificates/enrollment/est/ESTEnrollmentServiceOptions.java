/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.certificates.enrollment.est;

import static java.util.Objects.requireNonNull;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

public class ESTEnrollmentServiceOptions {

    private static final Property<Boolean> ENABLED = new Property<>("enabled", false);
    private static final Property<String> SERVER_HOST = new Property<>("server.url", "");
    private static final Property<String> SERVER_BOOTSTRAP_CERT = new Property<>("server.bootstrap-certificate", "");
    private static final Property<Boolean> SERVER_CERT_ROLLOVER = new Property<>("server.rollover", true);
    private static final Property<Boolean> HTTP_CLIENT_BASIC_AUTHENTICATION = new Property<>(
            "client.http.basic-authentication.enabled", false);
    private static final Property<Boolean> HTTP_CLIENT_BASIC_AUTHENTICATION_DIGEST = new Property<>(
            "client.http.basic-authentication.digest", false);
    private static final Property<String> HTTP_CLIENT_BASIC_AUTHENTICATION_USERNAME = new Property<>(
            "client.http.basic-authentication.username", "");
    private static final Property<String> HTTP_CLIENT_BASIC_AUTHENTICATION_PASSWORD = new Property<>(
            "client.http.basic-authentication.password", "");
    private static final Property<Boolean> HTTP_CLIENT_POF = new Property<>("client.pof", true);
    private static final Property<Boolean> CLIENT_RENEW = new Property<>("client.renew", false);
    private static final Property<Boolean> CLIENT_TLS_ENABLED = new Property<>("client.tls.enabled", false);
    private static final Property<String> CLIENT_SUBJECT_DN = new Property<>("client.subject.dn", "");
    private static final Property<String> CLIENT_KEYPAIR_ALGORITHM = new Property<>("client.keypair.algorithm",
            "ECDSA");
    private static final Property<String> CLIENT_KEYPAIR_ALGORITHM_PARAMETER = new Property<>(
            "client.keypair.algorithm.parameter", "prime256v1");
    private static final Property<String> CLIENT_CSR_ALGORITHM = new Property<>("client.csr.algorithm",
            "SHA256WITHECDSA");

    private final Boolean enabled;
    private final Server server;
    private final Client client;

    public ESTEnrollmentServiceOptions(final Map<String, Object> properties) throws MalformedURLException {
        requireNonNull(properties, "Properties cannot be null");

        this.enabled = ENABLED.get(properties);

        this.server = new Server();

        this.server.setHost(SERVER_HOST.get(properties));
        this.server.setBootstrapCertificate(SERVER_BOOTSTRAP_CERT.get(properties));
        this.server.setAutomaticCertRollover(SERVER_CERT_ROLLOVER.get(properties));

        this.client = new Client();

        BasicAuthentication basicAuth = new BasicAuthentication();
        basicAuth.setEnabled(HTTP_CLIENT_BASIC_AUTHENTICATION.get(properties));
        basicAuth.setDigestEnabled(HTTP_CLIENT_BASIC_AUTHENTICATION_DIGEST.get(properties));
        basicAuth.setUsername(HTTP_CLIENT_BASIC_AUTHENTICATION_USERNAME.get(properties));

        basicAuth.setPassword(HTTP_CLIENT_BASIC_AUTHENTICATION_PASSWORD.get(properties));

        this.client.setBasicAuthentication(basicAuth);

        this.client.setAutomaticCertRenew(CLIENT_RENEW.get(properties));
        this.client.setKeyPairAlgorithm(CLIENT_KEYPAIR_ALGORITHM.get(properties));
        this.client.setKeyPairAlgorithmParameter(CLIENT_KEYPAIR_ALGORITHM_PARAMETER.get(properties));
        this.client.setProofOfPossessionEnable(HTTP_CLIENT_POF.get(properties));
        this.client.setSignerAlgorithm(CLIENT_CSR_ALGORITHM.get(properties));
        this.client.setSubjectDN(CLIENT_SUBJECT_DN.get(properties));
        this.client.setTlsAuthenticationEnabled(CLIENT_TLS_ENABLED.get(properties));
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Server getServer() {
        return server;
    }

    public Client getClient() {
        return client;
    }

    public static class Server {

        private String host;
        private String bootstrapCertificate;

        private Boolean automaticCertRollover;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getBootstrapCertificate() {
            return bootstrapCertificate;
        }

        public void setBootstrapCertificate(String bootstrapCertificate) {
            this.bootstrapCertificate = bootstrapCertificate;
        }

        public Boolean isAutomaticCertRollover() {
            return this.automaticCertRollover;
        }

        public void setAutomaticCertRollover(Boolean automaticCertRollover) {
            this.automaticCertRollover = automaticCertRollover;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bootstrapCertificate, automaticCertRollover, host);
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
            Server other = (Server) obj;
            return Objects.equals(bootstrapCertificate, other.bootstrapCertificate)
                    && Objects.equals(automaticCertRollover, other.automaticCertRollover)
                    && Objects.equals(host, other.host);
        }

    }

    public static class Client {

        private BasicAuthentication basicAuthentication;
        private Boolean automaticCertRenew;
        private Boolean proofOfPossessionEnabled;
        private Boolean tlsAuthenticationEnabled;
        private String subjectDN;
        private String keyPairAlgorithm;
        private String keyPairAlgorithmParameter;
        private String signerAlgorithm;

        public String getSubjectDN() {
            return subjectDN;
        }

        public void setSubjectDN(String subjectDN) {
            this.subjectDN = subjectDN;
        }

        public Boolean isProofOfPossessionEnabled() {
            return proofOfPossessionEnabled;
        }

        public void setProofOfPossessionEnable(Boolean proofOfPossessionEnabled) {
            this.proofOfPossessionEnabled = proofOfPossessionEnabled;
        }

        public BasicAuthentication getBasicAuthentication() {
            return basicAuthentication;
        }

        public void setBasicAuthentication(BasicAuthentication basicAuthentication) {
            this.basicAuthentication = basicAuthentication;
        }

        public Boolean getAutomaticCertRenew() {
            return automaticCertRenew;
        }

        public void setAutomaticCertRenew(Boolean automaticCertRenew) {
            this.automaticCertRenew = automaticCertRenew;
        }

        public Boolean isTlsAuthenticationEnabled() {
            return tlsAuthenticationEnabled;
        }

        public void setTlsAuthenticationEnabled(Boolean tlsAuthenticationEnabled) {
            this.tlsAuthenticationEnabled = tlsAuthenticationEnabled;
        }

        public String getKeyPairAlgorithm() {
            return keyPairAlgorithm;
        }

        public void setKeyPairAlgorithm(String keyPairAlgorithm) {
            this.keyPairAlgorithm = keyPairAlgorithm;
        }

        public String getSignerAlgorithm() {
            return signerAlgorithm;
        }

        public void setSignerAlgorithm(String signerAlgorithm) {
            this.signerAlgorithm = signerAlgorithm;
        }

        public String getKeyPairAlgorithmParameter() {
            return keyPairAlgorithmParameter;
        }

        public void setKeyPairAlgorithmParameter(String keyPairAlgorithmParameter) {
            this.keyPairAlgorithmParameter = keyPairAlgorithmParameter;
        }

        @Override
        public int hashCode() {
            return Objects.hash(automaticCertRenew, basicAuthentication, keyPairAlgorithm, keyPairAlgorithmParameter,
                    proofOfPossessionEnabled, signerAlgorithm, subjectDN, tlsAuthenticationEnabled);
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
            Client other = (Client) obj;
            return Objects.equals(automaticCertRenew, other.automaticCertRenew)
                    && Objects.equals(basicAuthentication, other.basicAuthentication)
                    && Objects.equals(keyPairAlgorithm, other.keyPairAlgorithm)
                    && Objects.equals(keyPairAlgorithmParameter, other.keyPairAlgorithmParameter)
                    && Objects.equals(proofOfPossessionEnabled, other.proofOfPossessionEnabled)
                    && Objects.equals(signerAlgorithm, other.signerAlgorithm)
                    && Objects.equals(subjectDN, other.subjectDN)
                    && Objects.equals(tlsAuthenticationEnabled, other.tlsAuthenticationEnabled);
        }

    }

    public static class BasicAuthentication {

        private Boolean enabled;
        private Boolean digestEnabled;
        private String username;
        private String password;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean isDigestEnabled() {
            return digestEnabled;
        }

        public void setDigestEnabled(Boolean digestEnabled) {
            this.digestEnabled = digestEnabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public int hashCode() {
            return Objects.hash(digestEnabled, enabled, password, username);
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
            BasicAuthentication other = (BasicAuthentication) obj;
            return Objects.equals(digestEnabled, other.digestEnabled) && Objects.equals(enabled, other.enabled)
                    && Objects.equals(password, other.password) && Objects.equals(username, other.username);
        }

    }
}
