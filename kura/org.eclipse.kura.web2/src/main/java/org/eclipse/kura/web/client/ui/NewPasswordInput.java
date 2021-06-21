/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.client.ui.validator.PasswordStrengthValidators;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

public class NewPasswordInput extends Input {

    public NewPasswordInput() {
        super();

        setValidatorsFrom(EntryClassUi.getUserOptions());
    }

    @SuppressWarnings("unchecked")
    public void setValidatorsFrom(final GwtConsoleUserOptions userOptions) {
        setValidators();

        for (final Validator<String> validator : PasswordStrengthValidators.fromConfig(userOptions)) {
            addValidator(validator);
        }
    }
}
