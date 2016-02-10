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
package org.eclipse.kura.core.comm;

import java.io.IOException;

import javax.microedition.io.Connection;

import org.eclipse.kura.comm.CommURI;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;

public class CommConnectionFactory implements ConnectionFactory 
{
	@SuppressWarnings("unused")
	private ComponentContext      m_ctx;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	
	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext) 
	{			
		//
		// save the bundle context
		m_ctx = componentContext;
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		m_ctx = null;
	}
	
	public Connection createConnection(String name, int mode, boolean timeouts)
		throws IOException
	{
		try {
			CommURI uri = CommURI.parseString(name);
			return new CommConnectionImpl(uri, mode, timeouts);
		}
		catch (Throwable t) {
			throw new IOException(t);
		}
	}
}
