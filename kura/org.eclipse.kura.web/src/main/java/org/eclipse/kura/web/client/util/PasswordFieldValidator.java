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

import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.TextField;

public class PasswordFieldValidator extends TextFieldValidator 
{
	public PasswordFieldValidator(TextField<String> passwordField) {
		
		super (passwordField, FieldType.PASSWORD);
		m_textField.setRegex(null);
	}
	
	public String validate(Field<?> field, String value) {

		// if the field is not dirty, ignore the validation
		// this is needed for the update flow, in which we do not show the whole password
		boolean isDirty = m_textField.isDirty(); 
		if (!isDirty) {
			m_textField.setRegex(null);
			return null;
		}
		
		if (m_textFieldType.getRegex() != null) {
			m_textField.setRegex(m_textFieldType.getRegex());
		}		

		return super.validate(field, value);
	}
}
