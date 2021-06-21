/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.validator;

import java.util.function.Predicate;

public class NotEmptyValidator extends PredicateValidator {

    private static final Predicate<String> notEmptyPredicat = value -> value != null && !value.isEmpty();

    public NotEmptyValidator(String message) {
        super(notEmptyPredicat, message);
    }

    @Override
    public int getPriority() {
        return Priority.HIGHEST;
    }

}
