/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionExpirationSecurityHandler implements SecurityHandler {

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        final HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        final long now = System.currentTimeMillis();

        if (!session.isNew()) {
            final long lastActivity = getLastActivity(session);

            final int maxInactiveInterval = session.getMaxInactiveInterval();

            if (maxInactiveInterval > 0 && now - lastActivity > maxInactiveInterval * 1000) {
                session.invalidate();
                return false;
            }
        }

        session.setAttribute(Attributes.LAST_ACTIVITY.getValue(), now);

        return true;
    }

    private static long getLastActivity(final HttpSession session) {
        final Object lastActivityRaw = session.getAttribute(Attributes.LAST_ACTIVITY.getValue());

        if (!(lastActivityRaw instanceof Long)) {
            return 0;
        }

        return (long) lastActivityRaw;
    }

}
