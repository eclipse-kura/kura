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
package org.eclipse.kura.web.server.util;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ServiceLocator 
{
	private static final ServiceLocator s_instance = new ServiceLocator();
	
	private ServiceLocator()
	{}
	
	
	public static ServiceLocator getInstance() {
		return s_instance;
	}
	
	
	public <T> T getService(Class<T> serviceClass)
		throws GwtKuraException
	{
		T service = null; 
		BundleContext bundleContext = Console.getBundleContext();
		if (bundleContext != null) {
			ServiceReference sr = bundleContext.getServiceReference(serviceClass);
			if (sr != null) {
			    service = (T) bundleContext.getService(sr);
			}
		}
		if (service == null) {
			throw GwtKuraException.internalError(serviceClass.toString()+" not found.");
		}
		return service;
	}
}
