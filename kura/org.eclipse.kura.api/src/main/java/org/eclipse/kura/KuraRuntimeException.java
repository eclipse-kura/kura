/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuraRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = -7202805328805688329L;

	private static final Logger s_logger = LoggerFactory.getLogger(KuraRuntimeException.class);
	
	private static final String KURA_GENERIC_MESSAGES_PATTERN = "Generic Error - {0}: {1}";
	private static final String KURA_EXCEPTION_MESSAGES_BUNDLE = "org.eclipse.kura.core.messages.KuraExceptionMessagesBundle";

	//TODO - add back when logger is working
	//private static final Logger s_logger = LoggerFactory.getLogger(KuraException.class);

	protected KuraErrorCode m_code;
	private Object[] m_arguments;

	@SuppressWarnings("unused")
	private KuraRuntimeException() {
		super();
	}

	/**
	 * Builds a new EdcException instance based on the supplied EdcErrorCode.
	 * 
	 * @param code
	 * @param t
	 * @param arguments
	 */
	public KuraRuntimeException(KuraErrorCode code) {
		m_code = code;
	}

	/**
	 * Builds a new EdcException instance based on the supplied EdcErrorCode.
	 * 
	 * @param code
	 * @param t
	 * @param arguments
	 */
	public KuraRuntimeException(KuraErrorCode code, Object... arguments) {
		m_code = code;
		m_arguments = arguments;
	}

	/**
	 * Builds a new EdcException instance based on the supplied EdcErrorCode, an optional Throwable cause, and optional arguments for the associated exception message.
	 * 
	 * @param code
	 * @param t
	 * @param arguments
	 */
	public KuraRuntimeException(KuraErrorCode code, Throwable cause, Object... arguments) {
		super(cause);
		m_code = code;
		m_arguments = arguments;
	}

	public KuraErrorCode getCode() {
		return m_code;
	}

	public String getMessage() {
		return getLocalizedMessage(Locale.US);
	}

	
	public String getLocalizedMessage() {
		return getLocalizedMessage(Locale.getDefault());
	}

	
	private String getLocalizedMessage(Locale locale) 
	{
		String pattern = getMessagePattern(locale, m_code);
		if (m_code == null || KuraErrorCode.INTERNAL_ERROR.equals(m_code)) {
			if (m_arguments != null && m_arguments.length > 1) {
				// append all arguments into a single one			
				StringBuilder sbAllArgs = new StringBuilder();
				for (Object arg : m_arguments) {
					sbAllArgs.append(" - ");
					sbAllArgs.append(arg);
				}
				m_arguments = new Object[] {sbAllArgs.toString()};
			}
		}
		String message = MessageFormat.format(pattern, m_arguments);
		return message;
	}
	

	private String getMessagePattern(Locale locale, KuraErrorCode code) 
	{
		//
		// Load the message pattern from the bundle
		String messagePattern = null;
		ResourceBundle resourceBundle = null;
		try {
			
			resourceBundle = ResourceBundle.getBundle(KURA_EXCEPTION_MESSAGES_BUNDLE, locale);
			if (resourceBundle != null && code != null) {
				messagePattern = resourceBundle.getString(code.name());
				if (messagePattern == null) {
					s_logger.warn("Could not find Exception Messages for Locale {} and code {}", locale, code);
				}
			}
		} catch (MissingResourceException mre) {
			// log the failure to load a message bundle
			s_logger.warn("Could not load Exception Messages Bundle for Locale {}", locale);
		}

		//
		// If no bundle or code in the bundle is found, use a generic message
		if (messagePattern == null) {
			if (code != null) {
				// build a generic message format
				messagePattern = MessageFormat.format(KURA_GENERIC_MESSAGES_PATTERN, code.name());
			}
			else {
				// build a generic message format
				messagePattern = MessageFormat.format(KURA_GENERIC_MESSAGES_PATTERN, "Unknown");			
			}
		}

		return messagePattern;
	}

}
