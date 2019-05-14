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

        return defaultHandler.handleSecurity(request, response);
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
