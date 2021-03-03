/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Jens Reimann <jreimann@redhat.com>
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;

public class GwtSnapshotServiceImpl extends OsgiRemoteServiceServlet implements GwtSnapshotService {

    private static final long serialVersionUID = 8804372718146289179L;

    @Override
    public List<GwtSnapshot> findDeviceSnapshots(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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
            KuraExceptionHandler.handle(e);
        }

        return new ArrayList<>(snapshots);
    }

    @Override
    public void rollbackDeviceSnapshot(GwtXSRFToken xsrfToken, GwtSnapshot snapshot) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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
            KuraExceptionHandler.handle(e);
        }
    }
}
