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
package org.eclipse.kura.web.client.ui.validator;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

public class StringNotInListValidator extends NotInListValidator<String> {

    public StringNotInListValidator(List<String> values, String message) {
        super(values.stream().map(String::trim).collect(Collectors.toList()), message);
    }

    @Override
    public List<EditorError> validate(Editor<String> editor, String value) {
        return super.validate(editor, value.trim());
    }

}
