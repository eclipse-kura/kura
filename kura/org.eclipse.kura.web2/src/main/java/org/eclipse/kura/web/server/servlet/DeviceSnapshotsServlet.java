
/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DeviceSnapshotsServlet extends AuditServlet {

    private static final String SNAPSHOT_DOWNLOAD_TAG = "snapshot_";

    private static final long serialVersionUID = -2533869595709953567L;

    private static Logger logger = LoggerFactory.getLogger(DeviceSnapshotsServlet.class);

    public DeviceSnapshotsServlet() {
        super("UI Snapshots", "Return device snapshot");
    }

    // USED TO RETRIEVE SYSTEM SNAPSHOT
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        KuraRemoteServiceServlet.requirePermissions(request, Mode.ALL, new String[] { KuraPermission.ADMIN });

        try {
            GwtXSRFToken token = new GwtXSRFToken(request.getParameter("xsrfToken"));
            KuraRemoteServiceServlet.checkXSRFToken(request, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        try {

            String downloadFormat = Objects.requireNonNull(request.getParameter("downloadFormat"));
            Long snapshotId = Objects.requireNonNull(Long.parseLong(request.getParameter("snapshotId")));
            String pidList = Objects.requireNonNull(request.getParameter("pidsList"));

            if (pidList.isEmpty()) {
                parseEntireSnapshot(response, snapshotId, downloadFormat);
            } else {
                parsePartialSnapshot(response, snapshotId, downloadFormat, pidList);
            }

        } catch (Exception e) {
            logger.error("Error exporting snapshot");
            throw new ServletException(e);
        }

    }

    private void parseEntireSnapshot(HttpServletResponse response, Long snapshotId, String downloadFormat)
            throws GwtKuraException, ServletException, KuraException {

        ServiceLocator locator = ServiceLocator.getInstance();
        ConfigurationService cs = locator.getService(ConfigurationService.class);

        GwtServerUtil.writeSnapshot(response, cs.getSnapshot(snapshotId), SNAPSHOT_DOWNLOAD_TAG + snapshotId,
                downloadFormat);
    }

    private void parsePartialSnapshot(HttpServletResponse response, Long snapshotId, String downloadFormat,
            String pidList) throws GwtKuraException, ServletException, KuraException {

        ServiceLocator locator = ServiceLocator.getInstance();
        ConfigurationService cs = locator.getService(ConfigurationService.class);

        List<String> selectedPids = Arrays.asList(pidList.replace("SelectedPids: ", "").split(","));

        List<ComponentConfiguration> configs = cs.getSnapshot(snapshotId).stream()
                .filter(config -> selectedPids.contains(config.getPid())).collect(Collectors.toList());

        GwtServerUtil.writeSnapshot(response, configs, SNAPSHOT_DOWNLOAD_TAG + snapshotId, downloadFormat);
    }

}
