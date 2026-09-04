/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.configuration;

import java.util.Arrays;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class Password {

    private final char[] passwordVal;

    public Password(String password) {
        this.passwordVal = password != null ? password.toCharArray() : null;
    }

    public Password(char[] password) {
        this.passwordVal = password != null ? Arrays.copyOf(password, password.length) : null;
    }

    public char[] getPassword() {
        return this.passwordVal;
    }

    @Override
    public String toString() {
        return this.passwordVal != null ? new String(this.passwordVal) : "";
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.passwordVal);
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
        Password other = (Password) obj;
        return Arrays.equals(this.passwordVal, other.passwordVal);
    }
}
