/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.ConsoleOptions;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtSessionServiceImpl extends OsgiRemoteServiceServlet implements GwtSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GwtSessionServiceImpl.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void logout(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();

        final HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
            logger.warn("UI Logout - Success - Logout succeeded for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        }
    }

    @Override
    public String getLoginBanner() {
        final ConsoleOptions options = Console.getConsoleOptions();

        if (options.isBannerEnabled()) {
            return options.getBannerContent();
        } else {
            return null;
        }
    }

}
