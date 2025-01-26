/*******************************************************************************
 * Copyright (c) 2021, 2025 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuditServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final String componentId;
    private final String description;

    public AuditServlet(final String componentId, final String description) {
        this.componentId = componentId;
        this.description = description;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        final AuditContext context = Console.instance().initAuditContext(req);
        try (final Scope scope = AuditContext.openScope(context)) {
            super.service(req, resp);
        } catch (KuraRemoteServiceServlet.KuraPermissionException e) {
            resp.sendError(403);
        } finally {
            if (resp.getStatus() / 200 == 1) {
                auditLogger.info("{} {} - Success - {}", context, componentId, description);
            } else {
                auditLogger.info("{} {} - Failure - {}", context, componentId, description);
            }
        }
    }

}
