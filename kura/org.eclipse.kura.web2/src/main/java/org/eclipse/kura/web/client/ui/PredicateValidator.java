/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

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