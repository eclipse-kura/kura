/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.web.extension.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.web.api.ClientExtensionBundle;
import org.eclipse.kura.web.api.Console;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleExtensionComponent {

    private static final Logger logger = LoggerFactory.getLogger(ExampleExtensionComponent.class);

    private static final ClientExtensionBundle EXTENSION = new ClientExtensionBundle() {

        @Override
        public java.util.Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public String getEntryPointUrl() {
            return "/exampleext/exampleext.nocache.js";
        }

        @Override
        public Set<String> getProvidedAuthenticationMethods() {

            return Collections.unmodifiableSet(new HashSet<>(Arrays.asList("Always Fails", "Always Succeeds")));
        }
    };

    private HttpService httpService;
    private Console console;
    private DummyAuthenticationServiceImpl dummyAuth;

    public void setHttpService(final HttpService httpService) {
        this.httpService = httpService;
    }

    public void setConsole(final Console console) {
        this.console = console;
    }

    public void activate() {
        this.dummyAuth = new DummyAuthenticationServiceImpl(console);

        this.console.registerConsoleExtensionBundle(EXTENSION);
        this.console.registerLoginExtensionBundle(EXTENSION);
        try {
            final HttpContext context = this.httpService.createDefaultHttpContext();

            console.registerLoginServlet("/exampleext/dummylogin", dummyAuth);
            this.httpService.registerResources("/exampleext", "www/exampleext", context);
        } catch (final NamespaceException | ServletException e) {
            logger.warn("failed to register resources", e);
        }
    }

    public void deactivate() {

        try {
            this.console.unregisterConsoleExtensionBundle(EXTENSION);
            this.console.unregisterLoginExtensionBundle(EXTENSION);
            this.console.unregisterServlet("/exampleext/dummylogin");
            this.httpService.unregister("/exampleext");
        } catch (Exception e) {
            logger.warn("failed to unregister resources", e);
        }

    }

    public class RedirectServlet extends HttpServlet {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        private final Function<String, String> location;

        public RedirectServlet(final Function<String, String> location) {
            this.location = location;
        }

        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {
            redirect(req, resp);
        }

        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {
            redirect(req, resp);
        }

        @Override
        protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {
            redirect(req, resp);
        }

        @Override
        protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {
            redirect(req, resp);
        }

        @Override
        protected void doHead(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {
            redirect(req, resp);
        }

        @Override
        protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            redirect(req, resp);
        }

        private final void redirect(final HttpServletRequest req, final HttpServletResponse resp) {
            try {
                final String path = req.getRequestURI();

                resp.sendRedirect(location.apply(path));

            } catch (final Exception e) {
                logger.warn("unexpected exception", e);
            }
        }

    }
}
