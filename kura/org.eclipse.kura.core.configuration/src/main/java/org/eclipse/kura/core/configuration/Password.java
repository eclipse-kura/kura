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
package org.eclipse.kura.core.configuration;

public class Password 
{
	private char[] password;

	public Password(String password) {
		super();
		this.password = password.toCharArray();
	}
	
	public Password(char[] password) {
		super();
		this.password = password;
	}

	public char[] getPassword() {
		return password;
	}
	
	public String toString() {
		return new String(password);
	}
}
