/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import java.util.List;

import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.form.validator.RegExValidator;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

public class PidTextBox extends TextBox {

    private static final ValidationMessages messages = GWT.create(ValidationMessages.class);
    private static final SymbolicNameValidator SYMBOLIC_NAME_VALIDATOR = new SymbolicNameValidator();

    public PidTextBox() {
        addValidator(SYMBOLIC_NAME_VALIDATOR);
    }

    public String getPid() {
        if (!validate()) {
            return null;
        }
        return getValue();
    }

    // Even if the supplied validator list is empty, this will always validate against the service.pid
    @SuppressWarnings("unchecked")
    @Override
    public void setValidators(Validator<String>... validators) {
        super.setValidators();
        super.addValidator(SYMBOLIC_NAME_VALIDATOR);
        if (validators != null) {
            for (Validator<String> validator : validators) {
                super.addValidator(validator);
            }
        }
    }

    // Need to define a custom type because TextBox does not allow adding two validators having the same type,
    // this allows to optionally add another RegExValidator
    private static class SymbolicNameValidator implements Validator<String> {

        private static final String SYMBOLIC_NAME_PATTERN = "^[\\w\\-]+([\\.][\\w\\-]+)*$";
        private final Validator<String> wrapped = new RegExValidator(SYMBOLIC_NAME_PATTERN, messages.invalidPid());

        @Override
        public int getPriority() {
            return this.wrapped.getPriority();
        }

        @Override
        public List<EditorError> validate(Editor<String> editor, String value) {
            return this.wrapped.validate(editor, value);
        }
    }
}
