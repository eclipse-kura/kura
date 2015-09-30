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
package org.eclipse.kura.web.shared.service;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;

import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("status")
public interface GwtStatusService extends RemoteService {
	
	/**
	 * Returns a list of name/value pairs describing status aspects of the device.
	 * 
	 * @param hasNetAdmin - Boolean value indicating if system is running with the network management services enabled.
	 * @return
	 * @throws GwtKuraException
	 */
	public ListLoadResult<GwtGroupedNVPair> getDeviceConfig(boolean hasNetAdmin) throws GwtKuraException;
	
	/**
	 * Connects the local MQTT client to the specified broker.
	 * 
	 * @throws GwtKuraException
	 */
	public void connectDataService() throws GwtKuraException;
	
	/**
	 * Disconnects the local MQTT client.
	 * 
	 * @throws GwtKuraException
	 */
	public void disconnectDataService() throws GwtKuraException;
}
