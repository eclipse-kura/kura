/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.identity;

import java.util.Objects;

import org.eclipse.kura.identity.PasswordHash;

public class PasswordHashImpl implements PasswordHash {

    private final String hash;

    public PasswordHashImpl(String hash) {
        super();
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PasswordHashImpl)) {
            return false;
        }
        PasswordHashImpl other = (PasswordHashImpl) obj;
        return Objects.equals(this.hash, other.hash);
    }

    @Override
    public String toString() {
        return this.hash;
    }

}
