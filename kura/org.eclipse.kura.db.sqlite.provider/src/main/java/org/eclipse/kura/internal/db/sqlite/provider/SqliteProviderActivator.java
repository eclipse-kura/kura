/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.db.sqlite.provider;

import java.io.File;
import java.util.Optional;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SqliteProviderActivator implements BundleActivator {

    private static final String SQLITE_TMPDIR_PROPERTY_KEY = "org.sqlite.tmpdir";

    private boolean locationChanged = false;

    @Override
    public void start(final BundleContext context) throws Exception {
        final Optional<String> sqliteTmpDir = Optional.ofNullable(System.getProperty(SQLITE_TMPDIR_PROPERTY_KEY));

        if (!sqliteTmpDir.isPresent() || !new File(sqliteTmpDir.get()).isDirectory()) {

            final Optional<File> bundleStorageAreaLocation = Optional.ofNullable(context.getDataFile(""));

            if (bundleStorageAreaLocation.isPresent()) {
                System.setProperty(SQLITE_TMPDIR_PROPERTY_KEY, bundleStorageAreaLocation.get().getAbsolutePath());
                locationChanged = true;
            }
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (locationChanged) {
            System.clearProperty(SQLITE_TMPDIR_PROPERTY_KEY);
        }
    }

}
