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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RoutingSecurityHandler implements SecurityHandler {

    private final List<RouteHandler> handlers = new ArrayList<>();
    private final SecurityHandler defaultHandler;

    public RoutingSecurityHandler(final SecurityHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    public void addRouteHandler(final Predicate<String> uriMatcher, final SecurityHandler handler) {
        this.handlers.add(new RouteHandler(uriMatcher, handler));
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final String requestURI = request.getRequestURI();

        for (final RouteHandler handler : this.handlers) {
            if (handler.uriMatcher.test(requestURI)) {
                return handler.handler.handleSecurity(request, response);
            }
        }

        return this.defaultHandler.handleSecurity(request, response);
    }

    private static class RouteHandler {

        private final Predicate<String> uriMatcher;
        private final SecurityHandler handler;

        public RouteHandler(Predicate<String> uriMatcher, SecurityHandler handler) {
            this.uriMatcher = uriMatcher;
            this.handler = handler;
        }

    }
}
