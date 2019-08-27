/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.configuration;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class Password {

    private char[] value;

    public Password(String password) {
        super();
        if (password != null) {
            this.value = password.toCharArray();
        }
    }

    public Password(char[] password) {
        super();
        this.value = password;
    }

    public char[] getPassword() {
        return this.value;
    }

    @Override
    public String toString() {
        return new String(this.value);
    }
}
