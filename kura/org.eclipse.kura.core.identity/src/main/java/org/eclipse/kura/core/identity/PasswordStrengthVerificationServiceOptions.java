/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.identity;

import org.osgi.service.component.annotations.ComponentPropertyType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = PasswordStrengthVerificationServiceOptions.PID, name = "Password Strength", //
        description = "This component allows to configure the strength requirements for new passwords.")
@ComponentPropertyType
public @interface PasswordStrengthVerificationServiceOptions {

    public static final String PID = "org.eclipse.kura.identity.PasswordStrengthVerificationService";

    @AttributeDefinition(name = "Minimum password length", //
            min = "0", //
            description = "The minimum length to be enforced for new passwords. Set to 0 to disable.")
    public int new_password_min_length() default 8;

    @AttributeDefinition(name = "Require digits in new password", //
            description = "If set to true, new passwords will be accepted only if containing at least one digit.")
    public boolean new_password_require_digits() default false;

    @AttributeDefinition(name = "Require special characters in new password", //
            description = "If set to true, new passwords will be accepted only if containing at least one non alphanumeric character.")
    public boolean new_password_require_special_characters() default false;

    @AttributeDefinition(name = "Require uppercase and lowercase characters in new passwords", //
            description = "If set to true, new passwords will be accepted only if containing both"
                    + " uppercase and lowercase alphanumeric characters.")
    public boolean new_password_require_both_cases() default false;

}
