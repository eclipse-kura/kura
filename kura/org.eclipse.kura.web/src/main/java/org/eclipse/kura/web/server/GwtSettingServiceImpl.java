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

import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.AuthenticationManager;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSettings;
import org.eclipse.kura.web.shared.service.GwtSettingService;

public class GwtSettingServiceImpl extends OsgiRemoteServiceServlet implements GwtSettingService
{
	private static final long serialVersionUID = -3422518194598042896L;

	public void updateSettings(GwtSettings settings) throws GwtKuraException
	{
		AuthenticationManager authMgr = AuthenticationManager.getInstance(); 

		//
		// verify the current password
		boolean validCurrPwd = false;
		
		validCurrPwd = authMgr.authenticate("admin", settings.getPasswordCurrent());
		
		
		if (!validCurrPwd) {
			throw new GwtKuraException(GwtKuraErrorCode.CURRENT_ADMIN_PASSWORD_DOES_NOT_MATCH);
		}
		
		//
		// set the new password
		/*try {
			authMgr.changeAdminPassword(settings.getPasswordNew());
		}
		catch (SQLException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}*/
	}
	
	
	@SuppressWarnings("rawtypes")
	public void logout() 
		throws GwtKuraException
	{
		HttpSession httpSession = this.getThreadLocalRequest().getSession();
		Enumeration attrs = httpSession.getAttributeNames();
		while (attrs.hasMoreElements()) {
			
			String attr = (String) attrs.nextElement();
			httpSession.removeAttribute(attr);
		}
		httpSession.setAttribute("logout", "true");
	}
}
