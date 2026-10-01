/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration;

/**
 *
 * @author matteo.maiero
 *
 */

@Deprecated
public class Password {

    private final char[] password;

    public Password(String password) {
        super();
        this.password = password.toCharArray();
    }

    public Password(char[] password) {
        super();
        this.password = password;
    }

    public char[] getPassword() {
        return this.password;
    }

    @Override
    public String toString() {
        return new String(this.password);
    }
}
