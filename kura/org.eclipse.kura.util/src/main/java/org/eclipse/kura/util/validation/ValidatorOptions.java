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
package org.eclipse.kura.util.validation;

import java.util.Map;

import org.eclipse.kura.util.configuration.Property;

public class ValidatorOptions {

    private int passwordMinimumLength = 8;
    private boolean passwordRequireDigits = false;
    private boolean passwordRequireBothCases = false;
    private boolean passwordRequireSpecialChars = false;

    private static final String NEW_PASSW_MIN_LENGTH_PROP = "new.password.min.length";
    private static final String NEW_PASSW_REQUIRE_DIGITS = "new.password.require.digits";
    private static final String NEW_PASSW_REQUIRE_SPECIAL_CHARS = "new.password.require.special.characters";
    private static final String NEW_PASSW_REQUIRE_BOTH_CASES = "new.password.require.both.cases";

    private final Property<Integer> newPasswMinLenghthProperty = new Property<>(NEW_PASSW_MIN_LENGTH_PROP, 8);
    private final Property<Boolean> newPassRequireDigits = new Property<>(NEW_PASSW_REQUIRE_DIGITS, false);
    private final Property<Boolean> newPassRequireSpecialChars = new Property<>(NEW_PASSW_REQUIRE_SPECIAL_CHARS, false);
    private final Property<Boolean> newPassRequireBothCases = new Property<>(NEW_PASSW_REQUIRE_BOTH_CASES, false);

    public ValidatorOptions(int passwordMinimumLength, boolean passwordRequireDigits, boolean passwordRequireBothCases,
            boolean passwordRequireSpecialChars) {

        this.passwordMinimumLength = passwordMinimumLength;
        this.passwordRequireDigits = passwordRequireDigits;
        this.passwordRequireSpecialChars = passwordRequireSpecialChars;
        this.passwordRequireBothCases = passwordRequireBothCases;
    }

    public ValidatorOptions(Map<String, Object> configurationProperties) {
        if (configurationProperties != null) {
            this.passwordMinimumLength = this.newPasswMinLenghthProperty.get(configurationProperties);
            this.passwordRequireDigits = this.newPassRequireDigits.get(configurationProperties);
            this.passwordRequireSpecialChars = this.newPassRequireSpecialChars.get(configurationProperties);
            this.passwordRequireBothCases = this.newPassRequireBothCases.get(configurationProperties);
        }
    }

    public int isPasswordMinimumLength() {
        return this.passwordMinimumLength;
    }

    public boolean isPasswordRequireDigits() {
        return this.passwordRequireDigits;
    }

    public boolean isPasswordRequireBothCases() {
        return this.passwordRequireBothCases;
    }

    public boolean isPasswordRequireSpecialChars() {
        return this.passwordRequireSpecialChars;
    }

}
