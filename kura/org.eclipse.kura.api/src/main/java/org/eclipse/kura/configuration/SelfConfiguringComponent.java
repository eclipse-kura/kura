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

import org.eclipse.kura.KuraException;

/**
 * A SelfConfiguringComponent is a configurable component which maintains its state.
 * A SelfConfiguringComponent exposes its configuration information to the ConfigurationService 
 * and therefore can be have its configuration updated locally or remotely through the 
 * ConfigurationService APIs. 
 * However, a SelfConfiguringComponent does not rely on the ConfigurationService 
 * to keep the storage of its configuration. The configuration state is kept
 * internally in the SelfConfiguringComponent or derived at runtime from
 * other resources such system resources.<br>
 * An example of a SelfConfiguringComponent is the NetworkService whose state
 * is kept in the operating system instead of in the ConfigurationService. 
 */
public interface SelfConfiguringComponent 
{
	/**
	 * This method is called by the ConfigurationService when it requires
	 * the current snapshot of the configuration for this components.
	 * As SelfConfiguringComponents do not rely on the ConfigurationService
	 * to capture and store the current configuration, this call is needed
	 * to expose the current configuration externally and, for example,
	 * being able to store it in snapshot files. 
	 *  
	 * @return the current configuration for this component
	 * @throws KuraException
	 */
	public ComponentConfiguration getConfiguration() throws KuraException;
	
}
