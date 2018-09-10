/**
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.internal.driver.opcua;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.milo.opcua.stack.core.security.SecurityPolicy.Basic128Rsa15;
import static org.eclipse.milo.opcua.stack.core.security.SecurityPolicy.Basic256;
import static org.eclipse.milo.opcua.stack.core.security.SecurityPolicy.Basic256Sha256;
import static org.eclipse.milo.opcua.stack.core.security.SecurityPolicy.None;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.driver.opcua.auth.CertificateManager;
import org.eclipse.kura.internal.driver.opcua.auth.ExternalKeystoreCertificateManager;
import org.eclipse.kura.util.base.StringUtil;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link OpcUaOptions} is responsible to provide all the required
 * configurable options for the OPC-UA Driver.<br/>
 * <br/>
 *
 * The different properties to configure a OPC-UA Driver are as follows:
 * <ul>
 * <li>endpoint.ip</li>
 * <li>endpoint.port</li>
 * <li>server.name</li>
 * <li>application.name</li>
 * <li>application.uri</li>
 * <li>certificate.location</li>
 * <li>keystore.client.alias</li>
 * <li>keystore.server.alias</li>
 * <li>keystore.password</li>
 * <li>keystore.type</li> must be one of these : PKCS11, PKCS12, JKS
 * <li>security.policy</li> must be one of these : None, Basic128Rsa15,
 * Basic256, Basic256Sha256
 * <li>username</li>
 * <li>password</li>
 * <li>request.timeout</li>
 * <li>session.timeout</li>
 * </ul>
 */
final class OpcUaOptions {

    /**
     * Configurable Property to set OPC-UA application certificate
     */
    private static final String APPLICATION_CERTIFICATE = "certificate.location";

    /**
     * Configurable Property to set OPC-UA application name
     */
    private static final String APPLICATION_NAME = "application.name";

    /**
     * Configurable Property to set OPC-UA application uri
     */
    private static final String APPLICATION_URI = "application.uri";

    /** OPC-UA Endpoint IP */
    private static final String IP = "endpoint.ip";

    /**
     * Configurable property to set client alias for the keystore
     */
    private static final String KEYSTORE_CLIENT_ALIAS = "keystore.client.alias";

    /**
     * Configurable Property to set keystore password
     */
    private static final String KEYSTORE_PASSWORD = "keystore.password";

    /**
     * Configurable Property to set server alias for the keystore
     */
    private static final String AUTHENTICATE_SERVER = "authenticate.server";

    /**
     * Configurable Property to set keystore type
     */
    private static final String KEYSTORE_TYPE = "keystore.type";

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(OpcUaOptions.class);

    /**
     * Configurable Property to OPC-UA server password
     */
    private static final String PASSWORD = "password";

    /** OPC-UA Endpoint Port */
    private static final String PORT = "endpoint.port";

    /**
     * Configurable property specifying the request timeout
     */
    private static final String REQUEST_TIMEOUT = "request.timeout";

    /**
     * Configurable property specifying the Security Policy
     */
    private static final String SECURITY_POLICY = "security.policy";

    /** OPC-UA Server Name */
    private static final String SERVER_NAME = "server.name";

    /**
     * Configurable property specifying the session timeout
     */
    private static final String SESSION_TIMEOUT = "session.timeout";

    /**
     * Configurable Property to set OPC-UA server username
     */
    private static final String USERNAME = "username";

    private static final String SUBSCRIPTION_PUBLISH_INTERVAL = "subscription.publish.interval";

    private static final String MAX_REQUEST_ITEMS = "max.request.items";

    private static final String FORCE_ENDPOINT_URL = "force.endpoint.url";

    /** The Crypto Service dependency. */
    private final CryptoService cryptoService;

    /** The properties as associated */
    private final Map<String, Object> properties;

    private CertificateManager certificateManager;

    /**
     * Instantiates a new OPCUA options.
     *
     * @param properties
     *            the properties
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    OpcUaOptions(final Map<String, Object> properties, final CryptoService cryptoService) {
        requireNonNull(properties, "Properties cannot be null");
        requireNonNull(cryptoService, "Crypto Service cannot be null");

        this.properties = properties;
        this.cryptoService = cryptoService;
    }

    String getKeyStorePath() {
        String applicationCert = null;
        final Object certificate = this.properties.get(APPLICATION_CERTIFICATE);
        if (nonNull(certificate) && (certificate instanceof String)) {
            applicationCert = certificate.toString();
        }
        return applicationCert;
    }

    /**
     * Returns the OPC-UA Application Name
     *
     * @return the OPC-UA Application Name
     */
    String getApplicationName() {
        String applicationName = null;
        final Object appName = this.properties.get(APPLICATION_NAME);
        if (nonNull(appName) && (appName instanceof String)) {
            applicationName = appName.toString();
        }
        return applicationName;
    }

    /**
     * Returns the OPC-UA Application URI
     *
     * @return the OPC-UA Application URI
     */
    String getApplicationUri() {
        String applicationUri = null;
        final Object appUri = this.properties.get(APPLICATION_URI);
        if (nonNull(appUri) && (appUri instanceof String)) {
            applicationUri = appUri.toString();
        }
        return applicationUri;
    }

    /**
     * Returns the OPC-UA Identity Provider
     *
     * @return the OPC-UA Identity Provider
     */
    IdentityProvider getIdentityProvider() {
        IdentityProvider identityProvider;
        final String username = this.getUsername();
        final String password = this.getPassword();
        if (StringUtil.isNullOrEmpty(username) && StringUtil.isNullOrEmpty(password)) {
            identityProvider = new AnonymousProvider();
        } else {
            identityProvider = new UsernameProvider(username, password);
        }
        return identityProvider;
    }

    /**
     * Returns the OPC-UA Endpoint IP
     *
     * @return the OPC-UA Endpoint IP
     */
    String getIp() {
        String ipAddress = null;
        final Object ip = this.properties.get(IP);
        if (nonNull(ip) && (ip instanceof String)) {
            ipAddress = ip.toString();
        }
        return ipAddress;
    }

    /**
     * Returns the Keystore Client Alias
     *
     * @return the Keystore Client Alias
     */
    String getKeystoreClientAlias() {
        String clientAlias = null;
        final Object alias = this.properties.get(KEYSTORE_CLIENT_ALIAS);
        if (nonNull(alias) && (alias instanceof String)) {
            clientAlias = alias.toString();
        }
        return clientAlias;
    }

    /**
     * Returns the Keystore Password
     *
     * @return the Keystore Password
     */
    String getKeystorePassword() {
        String password = null;
        Password decryptedPassword;
        final Object keystorePass = this.properties.get(KEYSTORE_PASSWORD);
        if (nonNull(keystorePass) && (keystorePass instanceof String)) {
            try {
                decryptedPassword = new Password(this.cryptoService.decryptAes(keystorePass.toString().toCharArray()));
                password = new String(decryptedPassword.getPassword());
            } catch (final KuraException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return password;
    }

    boolean isServerAuthenticationEnabled() {
        boolean shouldAuthenticateServer = false;
        final Object shouldAuthenticateServerRaw = this.properties.get(AUTHENTICATE_SERVER);
        if (nonNull(shouldAuthenticateServerRaw) && (shouldAuthenticateServerRaw instanceof Boolean)) {
            shouldAuthenticateServer = (Boolean) shouldAuthenticateServerRaw;
        }
        return shouldAuthenticateServer;
    }

    /**
     * Returns the Keystore Type
     *
     * @return the Keystore Type
     */
    String getKeystoreType() {
        String keystoreType = null;
        final Object type = this.properties.get(KEYSTORE_TYPE);
        if (nonNull(type) && (type instanceof String)) {
            keystoreType = type.toString();
        }
        return keystoreType;
    }

    /**
     * Returns the OPC-UA Password
     *
     * @return the OPC-UA Password
     */
    String getPassword() {
        final Object pass = this.properties.get(PASSWORD);
        Password decryptedPassword;
        String password = null;
        if (nonNull(pass) && (pass instanceof String)) {
            try {
                decryptedPassword = new Password(this.cryptoService.decryptAes(pass.toString().toCharArray()));
                password = new String(decryptedPassword.getPassword());
            } catch (final KuraException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return password;
    }

    /**
     * Returns OPC-UA Endpoint Port
     *
     * @return the OPC-UA Endpoint Port
     */
    int getPort() {
        int port = 0;
        final Object endpointPort = this.properties.get(PORT);
        if (nonNull(endpointPort) && (endpointPort instanceof Integer)) {
            port = (Integer) endpointPort;
        }
        return port;
    }

    /**
     * Returns OPC-UA Request Timeout
     *
     * @return the OPC-UA Request Timeout
     */
    int getRequestTimeout() {
        int requestTimeout = 0;
        final Object reqTimeout = this.properties.get(REQUEST_TIMEOUT);
        if (nonNull(reqTimeout) && (reqTimeout instanceof Integer)) {
            requestTimeout = (Integer) reqTimeout;
        }
        return requestTimeout * 1000;
    }

    /**
     * Returns the Security Policy
     *
     * @return the Security Policy
     */
    SecurityPolicy getSecurityPolicy() {
        int securityPolicy = 0;
        final Object policy = this.properties.get(SECURITY_POLICY);
        if (nonNull(policy) && (policy instanceof Integer)) {
            securityPolicy = (Integer) policy;
        }
        switch (securityPolicy) {
        case 1:
            return Basic128Rsa15;
        case 2:
            return Basic256;
        case 3:
            return Basic256Sha256;
        default:
            return None;
        }
    }

    /**
     * Returns the OPC-UA Server Name
     *
     * @return the OPC-UA Server Name
     */
    String getServerName() {
        String serverName = null;
        final Object name = this.properties.get(SERVER_NAME);
        if (nonNull(name) && (name instanceof String)) {
            serverName = name.toString();
        }
        return serverName;
    }

    /**
     * Returns OPC-UA Session Timeout (in milliseconds)
     *
     * @return the OPC-UA Session Timeout (in milliseconds)
     */
    int getSessionTimeout() {
        int sessionTimeout = 0;
        final Object timeout = this.properties.get(SESSION_TIMEOUT);
        if (nonNull(timeout) && (timeout instanceof Integer)) {
            sessionTimeout = (Integer) timeout;
        }
        return sessionTimeout * 1000;
    }

    /**
     * Returns the OPC-UA Username
     *
     * @return the OPC-UA Username
     */
    String getUsername() {
        String username = null;
        final Object name = this.properties.get(USERNAME);
        if (nonNull(name) && (name instanceof String)) {
            username = name.toString();
        }
        return username;
    }

    long getSubsciptionPublishInterval() {
        final Object publishInterval = this.properties.get(SUBSCRIPTION_PUBLISH_INTERVAL);
        if (publishInterval instanceof Long) {
            return (Long) publishInterval;
        }
        return 1000L;
    }

    int getMaxItemCountPerRequest() {
        final Object maxRequestItems = this.properties.get(MAX_REQUEST_ITEMS);
        if (maxRequestItems instanceof Integer) {
            return (Integer) maxRequestItems;
        }
        return 10;
    }

    boolean shouldForceEndpointUrl() {
        final Object raw = this.properties.get(FORCE_ENDPOINT_URL);
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        return false;
    }

    CertificateManager getCertificateManager() {

        if (certificateManager == null) {
            certificateManager = new ExternalKeystoreCertificateManager(getKeyStorePath(), getKeystoreType(),
                    getKeystorePassword(), getKeystoreClientAlias(), isServerAuthenticationEnabled());
        }

        return certificateManager;
    }
}
