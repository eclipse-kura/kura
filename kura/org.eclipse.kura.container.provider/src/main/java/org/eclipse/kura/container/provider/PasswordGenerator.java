/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.container.provider;

import java.security.SecureRandom;
import java.util.Random;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.PasswordStrengthRequirements;

public class PasswordGenerator {

    private static final char[] SPECIAL_CHARS = { '!', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
            ':', ';', '?', '@', '[', ']', '^', '_', '{', '|', '~' };
    private static final int SPECIAL_CHARS_BOUND = SPECIAL_CHARS.length;
    private static final int DIGITS_BOUND = SPECIAL_CHARS_BOUND + ((int) '9' - (int) '0' + 1);
    private static final int LOWERCASE_BOUND = DIGITS_BOUND + ((int) 'z' - (int) 'a' + 1);
    private static final int UPPERCASE_BOUND = LOWERCASE_BOUND + ((int) 'Z' - (int) 'A' + 1);

    private PasswordGenerator() {
    }

    private static char charInRange(final Random random, final char lower, final char upper) {
        return (char) random.nextInt((int) lower, ((int) upper) + 1);
    }

    private static int freeIndex(final Random random, final char[] values) {
        int result;

        do {
            result = random.nextInt(values.length);
        } while (values[result] != '\u0000');

        return result;
    }

    public static char[] generatePassword(final PasswordStrengthRequirements requirements) throws KuraException {
        final Random random = new SecureRandom();

        final char[] pwd = new char[Math.max(32, requirements.getPasswordMinimumLength())];

        if (requirements.digitsRequired()) {
            pwd[freeIndex(random, pwd)] = charInRange(random, '0', '9');
        }

        if (requirements.specialCharactersRequired()) {
            pwd[freeIndex(random, pwd)] = SPECIAL_CHARS[random.nextInt(SPECIAL_CHARS.length)];
        }

        if (requirements.bothCasesRequired()) {
            pwd[freeIndex(random, pwd)] = charInRange(random, 'A', 'Z');
            pwd[freeIndex(random, pwd)] = charInRange(random, 'a', 'z');
        }

        for (int i = 0; i < pwd.length; i++) {
            if (pwd[i] != '\u0000') {
                continue;
            }

            final int index = random.nextInt(UPPERCASE_BOUND);

            char result;

            if (index < SPECIAL_CHARS_BOUND) {
                result = SPECIAL_CHARS[index];
            } else if (index < DIGITS_BOUND) {
                result = (char) ((int) '0' + (index - SPECIAL_CHARS_BOUND));
            } else if (index < LOWERCASE_BOUND) {
                result = (char) ((int) 'a' + (index - DIGITS_BOUND));
            } else {
                result = (char) ((int) 'A' + (index - LOWERCASE_BOUND));
            }

            pwd[i] = result;
        }

        return pwd;
    }
}
