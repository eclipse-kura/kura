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

import org.osgi.annotation.versioning.ProviderType;


/**
 * KuraStoreException is raised when a failure occurred during a persistence operation.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraStoreException extends KuraException 
{
	private static final long serialVersionUID = -3405089623687223551L;
	
	public KuraStoreException(Object argument) {
		super(KuraErrorCode.STORE_ERROR, null, argument);
	}
	
	public KuraStoreException(Throwable cause, Object argument) {
		super(KuraErrorCode.STORE_ERROR, cause, argument);
	}
}
