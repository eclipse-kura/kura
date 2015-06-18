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

import java.util.ArrayList;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBSGroupedNVPair;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("device")
public interface GwtBSDeviceService extends RemoteService {

	
	public ArrayList<GwtBSGroupedNVPair> findDeviceConfiguration()
			throws GwtKuraException;

	public ArrayList<GwtBSGroupedNVPair> findBundles()
			throws GwtKuraException;

	public ArrayList<GwtBSGroupedNVPair> findThreads()
			throws GwtKuraException;
	
	public ArrayList<GwtBSGroupedNVPair> findSystemProperties()
			throws GwtKuraException;

	public String executeCommand(String cmd, String pwd)
			throws GwtKuraException;

}