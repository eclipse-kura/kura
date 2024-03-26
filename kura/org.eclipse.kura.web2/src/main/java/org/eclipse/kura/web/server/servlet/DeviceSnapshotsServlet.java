/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSnapshotsServlet extends AuditServlet {

    private static final long serialVersionUID = -2533869595709953567L;

    private static Logger logger = LoggerFactory.getLogger(DeviceSnapshotsServlet.class);

    public DeviceSnapshotsServlet() {
        super("UI Snapshots", "Return device snapshot");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            KuraRemoteServiceServlet.requirePermissions(request, Mode.ALL, new String[] { KuraPermission.ADMIN });

        // BEGIN XSRF - Servlet dependent code

        try {
            GwtXSRFToken token = new GwtXSRFToken(request.getParameter("xsrfToken"));
            KuraRemoteServiceServlet.checkXSRFToken(request, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        try {

            String snapshotId = request.getParameter("snapshotId");

            ServiceLocator locator = ServiceLocator.getInstance();
            ConfigurationService cs = locator.getService(ConfigurationService.class);
            if (snapshotId != null) {

                long sid = Long.parseLong(snapshotId);
                List<ComponentConfiguration> configs = cs.getSnapshot(sid);

                GwtServerUtil.writeSnapshot(request, response, configs, "snapshot_" + sid);

            }
        } catch (Exception e) {
            logger.error("Error exporting snapshot");
            throw new ServletException(e);
        }
    }
}
