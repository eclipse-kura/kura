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
package org.eclipse.kura.web.shared.validator;

import java.util.function.Predicate;

public class NoWhitespaceCharactersValidator extends PredicateValidator {

    private static final Predicate<String> PREDICATE = value -> {
        if (value == null) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.codePointAt(i))) {
                return false;
            }
        }

        return true;
    };

    public NoWhitespaceCharactersValidator(String message) {
        super(PREDICATE, message);
    }
}
