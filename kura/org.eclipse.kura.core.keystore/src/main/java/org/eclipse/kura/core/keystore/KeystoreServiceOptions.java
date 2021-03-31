/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;

public class KeystoreServiceOptions {

    private static final String KEY_KEYSTORE_PATH = "keystore.path";
    private static final String KEY_KEYSTORE_PASSWORD = "keystore.password";

    private static final String DEFAULT_KEYSTORE_PATH = "/tmp";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private final Path keystorePath;
    private final Password keystorePassword;

    private final CryptoService cryptoService;

    public KeystoreServiceOptions(Map<String, Object> properties, CryptoService cryptoService) {
        if (isNull(properties) || isNull(cryptoService)) {
            throw new IllegalArgumentException("Input parameters cannot be null!");
        }
        String keystoreLocation = (String) properties.getOrDefault(KEY_KEYSTORE_PATH, DEFAULT_KEYSTORE_PATH);
        this.keystorePath = Paths.get(keystoreLocation);

        this.keystorePassword = new Password(
                (String) properties.getOrDefault(KEY_KEYSTORE_PASSWORD, DEFAULT_KEYSTORE_PASSWORD));

        this.cryptoService = cryptoService;

    }

    public Path getKeystorePath() {
        return this.keystorePath;
    }

    public char[] getKeystorePassword() {
        char[] snapshotPassword = this.keystorePassword.getPassword();
        try {
            snapshotPassword = this.cryptoService.decryptAes(snapshotPassword);
        } catch (KuraException e) {
            // Nothing to do
        }
        return snapshotPassword;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.keystorePassword == null ? 0 : this.keystorePassword.hashCode());
        result = prime * result + (this.keystorePath == null ? 0 : this.keystorePath.hashCode());
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
        KeystoreServiceOptions other = (KeystoreServiceOptions) obj;
        if (this.keystorePassword == null) {
            if (other.keystorePassword != null) {
                return false;
            }
        } else if (!Arrays.equals(this.keystorePassword.getPassword(), other.keystorePassword.getPassword())) {
            return false;
        }
        if (this.keystorePath == null) {
            if (other.keystorePath != null) {
                return false;
            }
        } else if (!this.keystorePath.equals(other.keystorePath)) {
            return false;
        }
        return true;
    }
}
