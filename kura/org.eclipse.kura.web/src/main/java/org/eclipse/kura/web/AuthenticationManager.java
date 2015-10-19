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
package org.eclipse.kura.web;

import java.util.Arrays;

import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;


public class AuthenticationManager 
{
	private static AuthenticationManager s_instance;

	private char[] password;
	private String username;

	protected AuthenticationManager(String username, char[] psw) {
		this.username= username;
		this.password= psw;
		s_instance= this;
	}

	public static AuthenticationManager getInstance() {
		return s_instance;
	}
	
	protected void updateUsername(String username){
		this.username= username;
	}
	
	protected void updatePassword(char[] psw){
		password= psw;
	}

	public boolean authenticate(String username, String password)
	{
		try {			

			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			String sha1Password= cryptoService.sha1Hash(password);
			boolean isUsernameMatching= username.equals(this.username);
			boolean isPasswordMatching= Arrays.equals(sha1Password.toCharArray(), this.password);
			return isUsernameMatching && isPasswordMatching;
		}catch (Exception e) {
		}
		return false;
	}
}
