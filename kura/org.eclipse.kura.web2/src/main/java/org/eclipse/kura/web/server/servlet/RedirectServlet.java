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
package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedirectServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(RedirectServlet.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final Predicate<String> shouldRedirect;
    private final String location;

    public RedirectServlet(final Predicate<String> shouldRedirect, final String location) {
        this.shouldRedirect = shouldRedirect;
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
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        redirect(req, resp);
    }

    private final void redirect(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            final String path = req.getRequestURI();

            if (this.shouldRedirect.test(path)) {
                resp.sendRedirect(this.location);
            } else {
                resp.sendError(404);
            }
        } catch (final Exception e) {
            logger.warn("unexpected exception", e);
        }
    }

}
