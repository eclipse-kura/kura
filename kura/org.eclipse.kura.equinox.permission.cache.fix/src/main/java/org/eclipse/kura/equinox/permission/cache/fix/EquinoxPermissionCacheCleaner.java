/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.equinox.permission.cache.fix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.permissionadmin.PermissionAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquinoxPermissionCacheCleaner {

    private static final Logger logger = LoggerFactory.getLogger(EquinoxPermissionCacheCleaner.class);

    private static final String CACHE_THRESHOLD_SIZE = "cache.threshold.size";

    private PermissionAdmin permissionAdmin;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private Future<?> cacheClearTask;

    private int cacheThresholdSize;

    public void setPermissionAdmin(PermissionAdmin permissionAdmin) {
        this.permissionAdmin = permissionAdmin;
    }

    public void activate(Map<String, Object> properties) {
        this.cacheThresholdSize = (Integer) properties.get(CACHE_THRESHOLD_SIZE);

        logger.debug("Permission cache threshold set to {}", this.cacheThresholdSize);

        this.cacheClearTask = this.executor.scheduleWithFixedDelay(this::clearCache, 0, 30, TimeUnit.SECONDS);
    }

    private void clearCache() {

        try {
            Field permAdminTableField = this.permissionAdmin.getClass().getDeclaredField("permAdminTable");
            permAdminTableField.setAccessible(true);

            Field condAdminTableField = this.permissionAdmin.getClass().getDeclaredField("condAdminTable");
            condAdminTableField.setAccessible(true);

            Object permAdminTable = permAdminTableField.get(this.permissionAdmin);
            Object condAdminTable = condAdminTableField.get(this.permissionAdmin);

            Method permAdminCollectionsMethod = permAdminTable.getClass().getDeclaredMethod("getCollections");
            permAdminCollectionsMethod.setAccessible(true);
            Method condAdminRowsMethod = condAdminTable.getClass().getDeclaredMethod("getRows");
            condAdminRowsMethod.setAccessible(true);

            Object[] permAdminCollections = (Object[]) permAdminCollectionsMethod.invoke(permAdminTable);
            Object[] condAdminRows = (Object[]) condAdminRowsMethod.invoke(condAdminTable);

            if (permAdminCollections.length > this.cacheThresholdSize
                    || condAdminRows.length > this.cacheThresholdSize) {

                final Method clearCaches = this.permissionAdmin.getClass().getMethod("clearCaches");

                clearCaches.invoke(this.permissionAdmin);

                logger.info("Permission cache cleared, Threshold limit of {} exceeded", this.cacheThresholdSize);
            }

        } catch (Exception e) {
            logger.warn("Unable to clear the permission cache", e);
        }
    }

    public void deactivate() {
        if (this.cacheClearTask != null) {
            this.cacheClearTask.cancel(true);
        }
        this.executor.shutdownNow();
    }
}
