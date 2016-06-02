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
package org.eclipse.kura.configuration;

public class Password 
{
	private char[] m_password;

	public Password(String password) {
		super();
		if (password != null) {
			m_password = password.toCharArray();
		}
	}
	
	public Password(char[] password) {
		super();
		m_password = password;
	}

	public char[] getPassword() {
		return m_password;
	}
	
	@Override
	public String toString() {
		return new String(m_password);
	}
}
