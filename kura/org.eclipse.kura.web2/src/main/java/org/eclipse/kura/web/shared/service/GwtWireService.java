/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This is essentially used by GWT to interact with the OSGi WireService for
 * retrieving and managing the Wire Graphs
 */
@RemoteServiceRelativePath("wires")
public interface GwtWireService extends RemoteService {

	/**
	 * Retrieves all the registered driver instances
	 *
	 * @param xsrfToken
	 *            the XSRF token
	 * @return the list of driver instances
	 * @throws GwtKuraException
	 *             if GWT encounters exception while retrieving the driver
	 *             instances
	 */
	public List<String> getDriverInstances(GwtXSRFToken xsrfToken) throws GwtKuraException;

	/**
	 * Returns the {@link GwtWiresConfiguration} instance associated
	 *
	 * @param xsrfToken
	 *            the XSRF token
	 * @return the {@link GwtWiresConfiguration} instance
	 * @throws GwtKuraException
	 *             if the associated instance is not retrieved
	 */
	public GwtWiresConfiguration getWiresConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

	/**
	 * Updates the {@link GwtWiresConfiguration} instance with the provided
	 * configuration
	 *
	 * @param xsrfToken
	 *            the XSRF token
	 * @param newJsonConfiguration
	 *            the new configuration to update
	 * @return the updated {@link GwtWiresConfiguration} instance
	 * @throws GwtKuraException
	 *             if the associated instance is not updated
	 */
	public GwtWiresConfiguration updateWireConfiguration(GwtXSRFToken xsrfToken, String newJsonConfiguration)
			throws GwtKuraException;

}
