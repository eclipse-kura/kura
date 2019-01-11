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
 * KuraTooManyInflightMessagesException is raised if a publish is attempted when there are already too many messages queued for publishing.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraTooManyInflightMessagesException extends KuraException 
{
	private static final long serialVersionUID = 8759879149959567323L;

	public KuraTooManyInflightMessagesException(Object argument) {
		super(KuraErrorCode.TOO_MANY_INFLIGHT_MESSAGES, null, argument);
	}
	
	public KuraTooManyInflightMessagesException(Throwable cause, Object argument) {
		super(KuraErrorCode.TOO_MANY_INFLIGHT_MESSAGES, cause, argument);
	}
}
