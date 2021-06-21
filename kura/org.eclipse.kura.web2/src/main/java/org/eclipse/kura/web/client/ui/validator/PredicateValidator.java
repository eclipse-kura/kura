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
package org.eclipse.kura.web.client.ui.validator;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

public class PredicateValidator implements Validator<String> {

    private final Predicate<String> predicate;
    private final String message;

    public PredicateValidator(final Predicate<String> predicate, final String message) {
        this.predicate = predicate;
        this.message = message;
    }

    @Override
    public int getPriority() {
        return Priority.MEDIUM;
    }

    @Override
    public List<EditorError> validate(final Editor<String> editor, final String value) {

        return this.predicate.test(value) ? Collections.emptyList()
                : Collections.singletonList(new BasicEditorError(editor, value, this.message));
    }

}