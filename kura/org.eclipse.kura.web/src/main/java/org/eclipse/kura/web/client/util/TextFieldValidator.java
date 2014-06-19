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

import java.util.MissingResourceException;

import org.eclipse.kura.web.client.messages.ValidationMessages;

import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.Validator;
import com.google.gwt.core.client.GWT;

public class TextFieldValidator implements Validator {

	private static final ValidationMessages MSGS = GWT.create(ValidationMessages.class);

	protected FieldType         m_textFieldType;
	protected TextField<String> m_textField;
	
	public TextFieldValidator(TextField<String> textField, FieldType textFieldType) {		

		m_textField = textField;
		m_textFieldType = textFieldType;
		
		// initialize the field for its validation
		if (m_textFieldType.getRegex() != null) {
			m_textField.setRegex(m_textFieldType.getRegex());
		}
		if (m_textFieldType.getToolTipMessage() != null) {
			m_textField.setToolTip(m_textFieldType.getToolTipMessage());
		}
		if (m_textFieldType.getRequiredMessage() != null) {
			m_textField.getMessages().setBlankText(m_textFieldType.getRequiredMessage());
		}
		if (m_textFieldType.getRegexMessage() != null) {
			m_textField.getMessages().setRegexText(m_textFieldType.getRegexMessage());
		}
	}


	public String validate(Field<?> field, String value) {
		
		String result = null;
		if (!value.matches(m_textFieldType.getRegex())) {
			result = m_textFieldType.getRegexMessage();
		}
		return result;
	}
	

	public enum FieldType {
		
		SIMPLE_NAME		("simple_name",  "^[a-zA-Z0-9\\-]{3,}$"),
		NAME            ("name",         "^[a-zA-Z0-9\\_\\-]{3,}$"),
		NAME_SPACE      ("name_space",   "^[a-zA-Z0-9\\ \\_\\-]{3,}$"),
		PASSWORD	    ("password",     "^.*(?=.{6,})(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!\\~\\|]).*$"), 
		EMAIL		    ("email",        "^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$"), 
		PHONE           ("phone",        "^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$"), 
		ALPHABET        ("alphabet",     "^[a-zA-Z_]+$"), 
		ALPHANUMERIC    ("alphanumeric", "^[a-zA-Z0-9_]+$"), 
		NUMERIC         ("numeric",      "^[+0-9.]+$"),
		NETWORK			("network",      "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,3})"),
		IPv4_ADDRESS	("ipv4_address", "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"),
		PORT_RANGE		("port_range", "^[0-9]+:*[0-9]+$"),
		MAC_ADDRESS		("mac_address", "^([0-9a-fA-F]{2}:){5}([0-9a-fA-F]{2})$");
		
		private String m_name;
		private String m_regex;
		private String m_regexMsg;
		private String m_toolTipMsg;
		private String m_requiredMsg;

		FieldType(String name, String regex) {
			
			m_name = name;
			m_regex = regex;
			m_regexMsg = name + "RegexMsg";
			m_toolTipMsg = name + "ToolTipMsg";
			m_requiredMsg = name + "RequiredMsg";
		}


		public String getName() {
			return m_name;
		}
		
		public String getRegex() {
			return m_regex;
		}

		public String getRegexMessage() {
			try {
				return MSGS.getString(m_regexMsg);
			}
			catch (MissingResourceException mre) {
				return null;
			}
		}

		public String getToolTipMessage() {
			try {
				return MSGS.getString(m_toolTipMsg);
			}
			catch (MissingResourceException mre) {
				return null;
			}
		}

		public String getRequiredMessage() {
			try {
				return MSGS.getString(m_requiredMsg);
			}
			catch (MissingResourceException mre) {
				return null;
			}
		}
	}
}
