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

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraPartialSuccessException is used capture the response 
 * of bulk operations which allow for the failures of some 
 * of their steps.
 * KuraPartialSuccessException.getCauses() will return the
 * exceptions collected during operations for those steps
 * that failed.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraPartialSuccessException extends KuraException
{
	private static final long serialVersionUID = -350563041335590477L;

	private List<Throwable> m_causes;
	
	public KuraPartialSuccessException(String message, List<Throwable> causes)
	{
		super(KuraErrorCode.PARTIAL_SUCCESS, (Throwable) null, message);
		m_causes = causes; 
	}
	
	
	/**
	 * Returns the list of failures collected during the execution of the bulk operation.
	 * @return causes
	 */
	public List<Throwable> getCauses()
	{
		return m_causes;
	}
}
