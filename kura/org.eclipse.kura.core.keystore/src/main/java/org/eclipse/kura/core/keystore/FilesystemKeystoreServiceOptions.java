/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.keystore;

import static java.util.Objects.isNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemKeystoreServiceOptions {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemKeystoreServiceOptions.class);

    private static final String KEY_SERVICE_PID = "kura.service.pid";
    static final String KEY_KEYSTORE_PATH = "keystore.path";
    static final String KEY_KEYSTORE_PASSWORD = "keystore.password";
    static final String KEY_RANDOMIZE_PASSWORD = "randomize.password";

    private static final String DEFAULT_KEYSTORE_PATH = "/tmp/keystore.ks";
    private static final boolean DEFAULT_RANDOMIZE_PASSWORD = false;
    static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private final Map<String, Object> properties;
    private final String pid;
    private final String keystorePath;
    private final Password keystorePassword;
    private final boolean randomPassword;

    public FilesystemKeystoreServiceOptions(Map<String, Object> properties, final CryptoService cryptoService) {
        if (isNull(properties) || isNull(cryptoService)) {
            throw new IllegalArgumentException("Input parameters cannot be null!");
        }

        this.properties = properties;

        this.pid = (String) properties.get(KEY_SERVICE_PID);

        String keyStorePath = (String) properties.getOrDefault(KEY_KEYSTORE_PATH, DEFAULT_KEYSTORE_PATH);
        try {
            this.keystorePath = validateAndNormalize(keyStorePath);
        } catch (Exception e) {
            logger.error("Keystore path {} not valid", keyStorePath);
            throw new IllegalArgumentException("Invalid keystore path");
        }

        this.keystorePassword = extractPassword(properties, cryptoService);

        this.randomPassword = (boolean) properties.getOrDefault(KEY_RANDOMIZE_PASSWORD, DEFAULT_RANDOMIZE_PASSWORD);
    }

    private String validateAndNormalize(String keystorePath) throws URISyntaxException, InvalidPathException {
        Paths.get(keystorePath); // throw InvalidPathException if invalid
        return new URI(keystorePath).normalize().toString();
    }

    private static Password extractPassword(final Map<String, Object> properties, final CryptoService cryptoService) {
        final Object optionsPassword = properties.get(KEY_KEYSTORE_PASSWORD);

        if (optionsPassword instanceof String) {
            return new Password((String) optionsPassword);
        }

        try {
            return new Password(cryptoService.encryptAes(DEFAULT_KEYSTORE_PASSWORD.toCharArray()));
        } catch (final Exception e) {
            logger.warn("failed to encrypt default keystore password", e);
            return new Password(DEFAULT_KEYSTORE_PASSWORD);
        }
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public String getPid() {
        return this.pid;
    }

    public String getKeystorePath() {
        return this.keystorePath;
    }

    public char[] getKeystorePassword(final CryptoService cryptoService) {
        try {
            return cryptoService.decryptAes(this.keystorePassword.getPassword());
        } catch (final Exception e) {
            return this.keystorePassword.getPassword();
        }
    }

    public boolean needsRandomPassword() {
        return this.randomPassword;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(keystorePassword.getPassword()), keystorePath, pid, randomPassword);
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
        FilesystemKeystoreServiceOptions other = (FilesystemKeystoreServiceOptions) obj;
        return Arrays.equals(keystorePassword.getPassword(), other.keystorePassword.getPassword())
                && Objects.equals(keystorePath, other.keystorePath) && Objects.equals(pid, other.pid)
                && randomPassword == other.randomPassword;
    }

}