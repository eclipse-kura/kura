/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.Enumeration;

import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSettingService;

public class GwtSettingServiceImpl extends OsgiRemoteServiceServlet implements GwtSettingService {

    private static final long serialVersionUID = -3422518194598042896L;

    @Override
    @SuppressWarnings("rawtypes")
    public void logout(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        HttpSession httpSession = getThreadLocalRequest().getSession();
        Enumeration attrs = httpSession.getAttributeNames();
        while (attrs.hasMoreElements()) {

            String attr = (String) attrs.nextElement();
            httpSession.removeAttribute(attr);
        }
        httpSession.setAttribute("logout", "true");
    }
}
