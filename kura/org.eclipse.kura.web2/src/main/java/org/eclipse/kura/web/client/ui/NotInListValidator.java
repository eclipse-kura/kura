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
package org.eclipse.kura.web.client.ui;

import java.util.Collections;
import java.util.List;

import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

public class NotInListValidator<T> implements Validator<T> {

    private final List<T> values;
    private final String message;

    public NotInListValidator(final List<T> values, final String message) {
        this.values = values;
        this.message = message;
    }

    @Override
    public int getPriority() {
        return Priority.MEDIUM;
    }

    @Override
    public List<EditorError> validate(final Editor<T> editor, final T value) {
        return this.values.contains(value)
                ? Collections.singletonList(new BasicEditorError(editor, value, this.message))
                : Collections.emptyList();
    }

}
