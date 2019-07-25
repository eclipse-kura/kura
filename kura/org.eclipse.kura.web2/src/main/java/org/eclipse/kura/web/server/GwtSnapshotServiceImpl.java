/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Jens Reimann <jreimann@redhat.com>
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtSnapshotServiceImpl extends OsgiRemoteServiceServlet implements GwtSnapshotService {

    private static final long serialVersionUID = 8804372718146289179L;

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    @Override
    public ArrayList<GwtSnapshot> findDeviceSnapshots(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        List<GwtSnapshot> snapshots = new ArrayList<>();
        try {
            ServiceLocator locator = ServiceLocator.getInstance();
            ConfigurationService cs = locator.getService(ConfigurationService.class);
            Set<Long> snapshotIds = cs.getSnapshots();
            if (snapshotIds != null && !snapshotIds.isEmpty()) {
                for (Long snapshotId : snapshotIds) {
                    GwtSnapshot snapshot = new GwtSnapshot();
                    snapshot.setCreatedOn(new Date(snapshotId));
                    snapshots.add(0, snapshot);
                }
            }
        } catch (Exception e) {
            auditLogger.warn("UI Snapshots - Failure - Failed to list device snapshots for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            KuraExceptionHandler.handle(e);
        }

        auditLogger.info("UI Snapshots - Success - Successfully listed device snapshots for user: {}, session: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        return new ArrayList<>(snapshots);
    }

    @Override
    public void rollbackDeviceSnapshot(GwtXSRFToken xsrfToken, GwtSnapshot snapshot) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            ServiceLocator locator = ServiceLocator.getInstance();
            ConfigurationService cs = locator.getService(ConfigurationService.class);
            cs.rollback(snapshot.getSnapshotId());

            //
            // Add an additional delay after the configuration update
            // to give the time to the device to apply the received
            // configuration
            SystemService ss = locator.getService(SystemService.class);
            long delay = Long.parseLong(ss.getProperties().getProperty("console.updateConfigDelay", "5000"));
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (Exception e) {
            auditLogger.warn(
                    "UI Snapshots - Failure - Failed to rollback device snapshot for user: {}, session: {}, snapshot id: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(),
                    snapshot.getSnapshotId());
            KuraExceptionHandler.handle(e);
        }

        auditLogger.info(
                "UI Snapshots - Success - Successfully rollbacked snapshot for user: {}, session: {}, snapshot id: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), snapshot.getSnapshotId());
    }
}
