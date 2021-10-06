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
import java.util.stream.Collectors;

public class StringNotInListValidator extends NotInListValidator<String> {

    public StringNotInListValidator(List<String> values, String message) {
        super(values.stream().map(String::trim).collect(Collectors.toList()), message);
    }

    @Override
    public void validate(final String value, final Consumer<String> errorMessageConsumer) {
        super.validate(value.trim(), errorMessageConsumer);
    }

}
