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

import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.form.validator.RegExValidator;

import com.google.gwt.core.client.GWT;

public class PidTextBox extends TextBox {

    private static final ValidationMessages messages = GWT.create(ValidationMessages.class);
    private static final String SYMBOLIC_NAME_PATTERN = "^[\\w\\-]+([\\.][\\w\\-]+)*$";

    public PidTextBox() {
        final RegExValidator validator = new RegExValidator(SYMBOLIC_NAME_PATTERN, messages.invalidPid());
        addValidator(validator);
    }

    public String getPid() {
        if (!validate()) {
            return null;
        }
        return getValue();
    }
}
