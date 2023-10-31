/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider.dto;

public class ValidatorOptionsDTO {

    private final int passwordMinimumLength;
    private final boolean passwordRequireDigits;
    private final boolean passwordRequireSpecialChars;
    private final boolean passwordRequireBothCases;

    public ValidatorOptionsDTO(int passwordMinimumLength, boolean passwordRequireDigits,
            boolean passwordRequireBothCases, boolean passwordRequireSpecialChars) {

        this.passwordMinimumLength = passwordMinimumLength;
        this.passwordRequireDigits = passwordRequireDigits;
        this.passwordRequireSpecialChars = passwordRequireSpecialChars;
        this.passwordRequireBothCases = passwordRequireBothCases;
    }

    public int getPasswordMinimumLength() {
        return this.passwordMinimumLength;
    }

    public boolean isPasswordRequireDigits() {
        return this.passwordRequireDigits;
    }

    public boolean isPasswordRequireSpecialChars() {
        return this.passwordRequireSpecialChars;
    }

    public boolean isPasswordRequireBothCases() {
        return this.passwordRequireBothCases;
    }

}
