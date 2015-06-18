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
import java.util.Date;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBSSnapshot;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("snapshot")
public interface GwtBSSnapshotService extends RemoteService
{
	public ArrayList<GwtBSSnapshot> findDeviceSnapshots() throws GwtKuraException;
	
	public void rollbackDeviceSnapshot(GwtBSSnapshot snapshot) throws GwtKuraException;
	
	public Date getDate(Date date);
	
	public Integer getInteger(Integer i);
}
