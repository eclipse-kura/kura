/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

public class StringLengthValidator extends PredicateValidator {

    public StringLengthValidator(int maxSize, String message) {
        super(createStringLimits(0, maxSize), message);
    }

    public StringLengthValidator(int minSize, int maxSize, String message) {
        super(createStringLimits(minSize, maxSize), message);
    }

    private static Predicate<String> createStringLimits(int minSize, int maxSize) {
        return value -> value.length() >= minSize && value.length() <= maxSize;
    }
}
