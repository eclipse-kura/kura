/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Eurotech
 *******************************************************************************/
package org.eclipse.kura.emulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;

public class Emulator {

    private static final String KURA_SNAPSHOTS_PATH = "kura.snapshots";

    private static final String EMULATOR = "emulator";

    private static final String KURA_MODE = "org.eclipse.kura.mode";

    private static final String SNAPSHOT_0_NAME = "snapshot_0.xml";

    private ComponentContext m_componentContext;

    protected void activate(ComponentContext componentContext) {
        this.m_componentContext = componentContext;

        try {
            // Properties props = System.getProperties();
            String mode = System.getProperty(KURA_MODE);
            if (EMULATOR.equals(mode)) {
                System.out.println("Framework is running in emulation mode");
                final String snapshotFolderPath = System.getProperty(KURA_SNAPSHOTS_PATH);
                if (snapshotFolderPath == null || snapshotFolderPath.isEmpty()) {
                    throw new IllegalStateException("System property 'kura.snapshots' is not set");
                }
                final File snapshotFolder = new File(snapshotFolderPath);
                if (!snapshotFolder.exists() || snapshotFolder.list().length == 0) {
                    snapshotFolder.mkdirs();
                    copySnapshot(snapshotFolderPath);
                }
            } else {
                System.out.println("Framework is not running in emulation mode");
            }

        } catch (Exception e) {
            System.out
                    .println("Framework is not running in emulation mode or initialization failed!: " + e.getMessage());
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        this.m_componentContext = null;
    }

    private void copySnapshot(String snapshotFolderPath) throws IOException {
        InputStream fileInput = null;
        OutputStream fileOutput = null;
        try {
            URL internalSnapshotURL = this.m_componentContext.getBundleContext().getBundle()
                    .getResource(SNAPSHOT_0_NAME);
            fileInput = internalSnapshotURL.openStream();
            fileOutput = new FileOutputStream(snapshotFolderPath + File.separator + SNAPSHOT_0_NAME);
            if (fileInput != null) {
                IOUtils.copy(fileInput, fileOutput);
            }
        } finally {
            if (fileOutput != null) {
                fileOutput.close();
            }
            if (fileInput != null) {
                fileInput.close();
            }
        }
    }
}
