/*******************************************************************************
 * Copyright (c) 2018, 2024 Eurotech and/or its affiliates and others
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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.http.HTTPException;

import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelServlet extends AuditServlet {

    private static final long serialVersionUID = -1445700937173920652L;

    private static Logger logger = LoggerFactory.getLogger(ChannelServlet.class);

    public ChannelServlet() {
        super("UI Channel Servlet", "Write Channel CSV description");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        KuraRemoteServiceServlet.requirePermissions(req, Mode.ALL, new String[] { KuraPermission.WIRES_ADMIN });

        HttpSession session = req.getSession(false);

        // BEGIN XSRF - Servlet dependent code
        try {
            GwtXSRFToken token = new GwtXSRFToken(req.getParameter("xsrfToken"));
            KuraRemoteServiceServlet.checkXSRFToken(req, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        try {
            String assetPid = req.getParameter("assetPid");
            String id = req.getParameter("id");

            final String attributeKey = "kura.csv.download." + id;

            final String result = (String) session.getAttribute(attributeKey);

            if (result == null) {
                throw new HTTPException(404);
            }

            session.removeAttribute(attributeKey);

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/csv");
            resp.setHeader("Content-Disposition", "attachment; filename=asset_" + assetPid + ".csv");
            resp.setHeader("Cache-Control", "no-transform, max-age=0");
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(result);
            }

        } catch (Exception ex) {
            logger.error("Error while exporting CSV output!", ex);
        }
    }

}
