/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
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
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("security")
public interface GwtSecurityService extends RemoteService 
{	
	public Boolean isSecurityServiceAvailable();
	
	public Boolean isDebugMode();
	
	public void reloadSecurityPolicyFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException;
	
	public void reloadCommandLineFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException;
}