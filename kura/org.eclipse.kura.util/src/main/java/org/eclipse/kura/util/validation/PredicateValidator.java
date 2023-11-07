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

import java.util.function.Consumer;
import java.util.function.Predicate;

public class PredicateValidator implements Validator<String> {

    private final Predicate<String> predicate;
    private final String message;

    public PredicateValidator(final Predicate<String> predicate, final String message) {
        this.predicate = predicate;
        this.message = message;
    }

    @Override
    public void validate(final String value, final Consumer<String> errorMessageConsumer) {

        if (!this.predicate.test(value)) {
            errorMessageConsumer.accept(this.message);
        }
    }

}