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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SecurityHandler {

    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException;

    public default SecurityHandler onFailure(final FailureHandler handler) {
        final SecurityHandler self = this;
        return (req, res) -> {
            boolean authorized;

            try {
                authorized = self.handleSecurity(req, res);
            } catch (final Exception e) {
                authorized = false;
            }

            if (!authorized) {
                handler.onFailure(req, res);
            }

            return authorized;
        };
    }

    public default SecurityHandler redirectOnFailure(final String location) {
        return onFailure((req, res) -> res.sendRedirect(location));
    }

    public default SecurityHandler sendErrorOnFailure(final int status) {
        return onFailure((req, res) -> res.sendError(status));
    }

    public static SecurityHandler chain(SecurityHandler... handlers) {
        return (req, res) -> {
            for (final SecurityHandler handler : handlers) {
                if (!handler.handleSecurity(req, res)) {
                    return false;
                }
            }
            return true;
        };
    }

    public interface FailureHandler {

        public void onFailure(final HttpServletRequest request, final HttpServletResponse response) throws IOException;
    }

}
