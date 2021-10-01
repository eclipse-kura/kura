/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.ui.validator.GwtValidators;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

public class NewPasswordInput extends Input {

    public NewPasswordInput() {
        super();

        setValidatorsFrom(EntryClassUi.getUserOptions());
    }

    @SuppressWarnings("unchecked")
    public void setValidatorsFrom(final GwtConsoleUserOptions userOptions) {
        setValidators();

        final List<Validator<String>> validators = GwtValidators.passwordStrength(userOptions);

        addValidator(new Validator<String>() {

            @Override
            public int getPriority() {
                return Priority.MEDIUM;
            }

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                final List<EditorError> result = new ArrayList<>();

                for (final Validator<String> validator : validators) {
                    result.addAll(validator.validate(editor, value));
                }

                return result;
            }

        });

    }
}
