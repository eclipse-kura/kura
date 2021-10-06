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

import java.util.List;
import java.util.function.Consumer;

public class NotInListValidator<T> implements Validator<T> {

    private final List<T> values;
    private final String message;

    public NotInListValidator(final List<T> values, final String message) {
        this.values = values;
        this.message = message;
    }

    @Override
    public void validate(T value, Consumer<String> errorMessageConsumer) {
        if (this.values.contains(value)) {
            errorMessageConsumer.accept(this.message);
        }
    }

}
