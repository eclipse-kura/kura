/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.security;

import org.eclipse.kura.KuraException;

public interface SecurityService {
	
	/**
	 * Takes a keystore path and returns the corresponding password that can be
	 * used to access to the data saved in the specified keystore.
	 * 
	 * @param keyStorePath A String that represents a unique identifier of the specified keystore.
	 * @return A char array that represents the password of the specified keystore.
	 */
	public void reloadSecurityPolicyFingerprint() throws KuraException;
	
	
	/**
	 * Takes a keystore path and returns the corresponding password that can be
	 * used to access to the data saved in the specified keystore.
	 * 
	 * @param keyStorePath A String that represents a unique identifier of the specified keystore.
	 * @return A char array that represents the password of the specified keystore.
	 */
	public void reloadEnvironmentConfigurationFingerprint() throws KuraException;
	
	/**
	 * Takes a keystore path and returns the corresponding password that can be
	 * used to access to the data saved in the specified keystore.
	 * 
	 * @param keyStorePath A String that represents a unique identifier of the specified keystore.
	 * @return A char array that represents the password of the specified keystore.
	 */
	public void reloadFingerprint(String path) throws KuraException;
}
