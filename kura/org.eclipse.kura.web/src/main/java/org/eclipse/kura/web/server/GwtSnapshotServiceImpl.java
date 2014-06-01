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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.ListLoadResult;

public class GwtSnapshotServiceImpl extends OsgiRemoteServiceServlet implements GwtSnapshotService
{
	private static final long serialVersionUID = 8804372718146289179L;


	public ListLoadResult<GwtSnapshot> findDeviceSnapshots() 
		throws GwtKuraException
	{
		List<GwtSnapshot> snapshots = new ArrayList<GwtSnapshot>();
		try {
			
			// execute the command
	        ServiceLocator  locator = ServiceLocator.getInstance();
			ConfigurationService cs = locator.getService(ConfigurationService.class);			 
	        Set<Long>  snapshotIds = cs.getSnapshots();
	        if (snapshotIds != null) {
	        	
	        	// sort them by most recent first
	        	if (snapshotIds != null && snapshotIds.size() > 0) {
		        	for (Long snapshotId : snapshotIds) {
		        		GwtSnapshot snapshot = new GwtSnapshot();
		        		snapshot.setCreatedOn( new Date(snapshotId));
		        		snapshots.add(0, snapshot);	
		        	}
	        	}
	        }
		} 
		catch(Throwable t) {
			KuraExceptionHandler.handle(t);
	    }        
		
		return new BaseListLoadResult<GwtSnapshot>(snapshots);
	}


	public void rollbackDeviceSnapshot(GwtSnapshot snapshot) 
		throws GwtKuraException
	{
		try {			

	        ServiceLocator  locator = ServiceLocator.getInstance();
			ConfigurationService cs = locator.getService(ConfigurationService.class);			 
	        cs.rollback(snapshot.getSnapshotId());

        	//
        	// Add an additional delay after the configuration update
        	// to give the time to the device to apply the received 
        	// configuration            
			SystemService ss = locator.getService(SystemService.class);
			long delay = Long.parseLong(ss.getProperties().getProperty("console.updateConfigDelay", "5000"));
            if (delay > 0) {
            	Thread.sleep(delay);
            }		
		} 
		catch(Throwable t) {
			KuraExceptionHandler.handle(t);
	    }        
	}
}
