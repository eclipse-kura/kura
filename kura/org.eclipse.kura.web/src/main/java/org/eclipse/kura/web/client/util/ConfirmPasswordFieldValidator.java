/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.util;

import org.eclipse.kura.web.client.messages.ValidationMessages;

import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.core.client.GWT;

public class ConfirmPasswordFieldValidator extends PasswordFieldValidator 
{
	private static final ValidationMessages MSGS = GWT.create(ValidationMessages.class);

	private TextField<String> m_passwordField;
	
	public ConfirmPasswordFieldValidator(TextField<String> confirmPasswordField, TextField<String> passwordField) {
		
		super (confirmPasswordField);		
		m_passwordField = passwordField;
	}
	
	
	public String validate(Field<?> field, String value) 
	{	
		String result = super.validate(field, value);
		if (result == null) {
			if (!value.equals(m_passwordField.getValue())) {
				result = MSGS.passwordDoesNotMatch();
			}
		}
		return result;
	}
}
