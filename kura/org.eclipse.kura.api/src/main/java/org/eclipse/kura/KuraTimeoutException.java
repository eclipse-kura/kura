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
 * KuraTimeoutException is raised when the attempted operation failed to respond before the timeout exprises.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraTimeoutException extends KuraException 
{
	private static final long serialVersionUID = -3042470573773974746L;

	public KuraTimeoutException(String message) {
		super(KuraErrorCode.TIMED_OUT, null, message);
	}
	
	public KuraTimeoutException(String message, Throwable cause) {
		super(KuraErrorCode.TIMED_OUT, cause, message);
	}
}
