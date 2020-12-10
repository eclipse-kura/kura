/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.Console;

public class SessionAutorizationSecurityHandler implements SecurityHandler {

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        final Object authorized = session.getAttribute(Attributes.AUTORIZED_USER.getValue());

        if (!(authorized instanceof String)) {
            return false;
        }

        final String userName = (String) authorized;

        return Objects.equals(session.getAttribute(Attributes.CREDENTIALS_HASH.getValue()),
                Console.instance().getUserManager().getCredentialsHash(userName));
    }

}
