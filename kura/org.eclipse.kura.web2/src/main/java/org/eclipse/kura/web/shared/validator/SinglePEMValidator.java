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

public class SinglePEMValidator extends PredicateValidator {

    private static final String PEM_REGEX = "^-{5}BEGIN CERTIFICATE-{5}\n[\\W\\w]*?-{5}END CERTIFICATE-{5}";

    public SinglePEMValidator(String message) {
        super(v -> {
            boolean match = v.matches(PEM_REGEX);
            String occurrences = v.replaceFirst(PEM_REGEX, "");
            return match && occurrences.isEmpty();
        }, message);

    }
}
