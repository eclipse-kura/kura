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

package org.eclipse.kura.container.orchestration;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;

/**
 * Stores the credentials for password authentication to Container Orchestration registry
 * The password provided is managed as {@link org.eclipse.kura.configuration.Password} and is encrypted by the
 * {@link org.eclipse.kura.crypto.CryptoService#encryptAes(char[])}
 *
 * @since 2.3
 */
public class PasswordRegistryCredentials implements RegistryCredentials {

    private final Optional<String> url;
    private final String username;
    private final Password password;

    public PasswordRegistryCredentials(Optional<String> url, String username, Password password) {
        super();
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Optional<String> getUrl() {
        return this.url;
    }

    public String getUsername() {
        return this.username;
    }

    public Password getPassword() {
        return this.password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(this.password.getPassword()), this.url, this.username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PasswordRegistryCredentials)) {
            return false;
        }
        PasswordRegistryCredentials other = (PasswordRegistryCredentials) obj;
        return Arrays.equals(this.password.getPassword(), other.password.getPassword())
                && Objects.equals(this.url, other.url) && Objects.equals(this.username, other.username);
    }

}
