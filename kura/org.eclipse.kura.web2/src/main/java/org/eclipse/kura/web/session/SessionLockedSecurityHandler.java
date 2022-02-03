/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionLockedSecurityHandler implements SecurityHandler {

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        return session.getAttribute(Attributes.LOCKED.getValue()) == null;
    }

}
