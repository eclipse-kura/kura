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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

public class EquinoxPermissionCacheCleanerTest {

    private static final int DEFAULT_CACHE_THRESHOLD_SIZE = 10000;

    private static final String CACHE_THRESHOLD_SIZE = "cache.threshold.size";

    private EquinoxPermissionCacheCleaner equinoxPermissionCacheCleaner;

    private PermissionAdmin permissionAdmin;

    private boolean clearCachesInvoked = false;

    @Test
    public void shouldInvokeClearCaches() {

        givenEquinoxPermissionCacheCleaner();
        givenTestPermissionAdminWithCacheSize(102000);

        whenEquinoxPermissionCacheCleanerActivated();

        thenClearCachesInvoked(true);
    }

    @Test
    public void shouldNotInvokeClearCaches() {

        givenEquinoxPermissionCacheCleaner();
        givenTestPermissionAdminWithCacheSize(9000);

        whenEquinoxPermissionCacheCleanerActivated();

        thenClearCachesInvoked(false);
    }

    private void givenTestPermissionAdminWithCacheSize(int cacheSize) {
        this.permissionAdmin = new TestPermissionAdmin(cacheSize);
        this.equinoxPermissionCacheCleaner.setPermissionAdmin(this.permissionAdmin);
    }

    private void givenEquinoxPermissionCacheCleaner() {
        this.equinoxPermissionCacheCleaner = new EquinoxPermissionCacheCleaner();
    }

    private void whenEquinoxPermissionCacheCleanerActivated() {
        Map<String, Object> props = new HashMap<>();
        props.put(CACHE_THRESHOLD_SIZE, DEFAULT_CACHE_THRESHOLD_SIZE);

        this.equinoxPermissionCacheCleaner.activate(props);

        try {
            Thread.sleep(500l);
        } catch (InterruptedException e) {
            fail();
        }
    }

    private void thenClearCachesInvoked(boolean expectedInvoked) {
        assertEquals(expectedInvoked, clearCachesInvoked);
    }

    private class TestPermissionAdmin implements PermissionAdmin {

        @SuppressWarnings("unused")
        private final TestPermAdminTable permAdminTable;
        @SuppressWarnings("unused")
        private final TestCondAdminTable condAdminTable;

        public TestPermissionAdmin(int cacheSize) {
            this.permAdminTable = new TestPermAdminTable(cacheSize);
            this.condAdminTable = new TestCondAdminTable(cacheSize);
        }

        @Override
        public PermissionInfo[] getPermissions(String location) {
            return null;
        }

        @Override
        public void setPermissions(String location, PermissionInfo[] permissions) {
            // nothing to do, mock method
        }

        @Override
        public String[] getLocations() {
            return null;
        }

        @Override
        public PermissionInfo[] getDefaultPermissions() {
            return null;
        }

        @Override
        public void setDefaultPermissions(PermissionInfo[] permissions) {
            // nothing to do, mock method
        }

        @SuppressWarnings("unused")
        public void clearCaches() {
            clearCachesInvoked = true;
        }

    }

    private class TestCondAdminTable {

        private final Object[] objects;

        public TestCondAdminTable(int cacheSize) {
            this.objects = new Object[cacheSize];
        }

        @SuppressWarnings("unused")
        public Object[] getRows() {
            return this.objects;
        }
    }

    private class TestPermAdminTable {

        private final Object[] objects;

        public TestPermAdminTable(int cacheSize) {
            this.objects = new Object[cacheSize];
        }

        @SuppressWarnings("unused")

        public Object[] getCollections() {
            return this.objects;
        }
    }
}
