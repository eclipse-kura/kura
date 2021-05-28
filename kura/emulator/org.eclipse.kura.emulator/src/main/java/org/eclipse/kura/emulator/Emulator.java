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
 *******************************************************************************/
package org.eclipse.kura.emulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Emulator {

    private static final Logger logger = LoggerFactory.getLogger(Emulator.class);

    private static final String KURA_SNAPSHOTS_PATH = "kura.snapshots";

    private static final String EMULATOR = "emulator";

    private static final String KURA_MODE = "org.eclipse.kura.mode";

    private static final String SNAPSHOT_0_NAME = "snapshot_0.xml";

    private static final String CLIENT_ID_PLACEHOLDER = "{{client-id-placeholder}}";

    private ComponentContext componentContext;

    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;

        try {
            String mode = System.getProperty(KURA_MODE);

            if (EMULATOR.equals(mode)) {
                logger.info("Framework is running in emulation mode");
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
                logger.info("Framework is not running in emulation mode");
            }

        } catch (Exception e) {
            logger.info("Framework is not running in emulation mode or initialization failed!: {}", e.getMessage());
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        this.componentContext = null;
    }

    private void copySnapshot(String snapshotFolderPath) throws IOException {
        URL internalSnapshotURL = this.componentContext.getBundleContext().getBundle().getResource(SNAPSHOT_0_NAME);

        try (InputStream fileInput = internalSnapshotURL.openStream();
                OutputStream fileOutput = new FileOutputStream(snapshotFolderPath + File.separator + SNAPSHOT_0_NAME)) {

            String generatedClientId = generateRandomClientId();

            String newSnapshot0Content = IOUtils.toString(fileInput, StandardCharsets.UTF_8)
                    .replace(CLIENT_ID_PLACEHOLDER, generatedClientId);

            logger.info("generated new client-id: {}", generatedClientId);

            IOUtils.copy(new StringReader(newSnapshot0Content), fileOutput);
        }
    }

    private String generateRandomClientId() {
        return "kura-emulator-" + RandomStringUtils.randomNumeric(6);
    }
}
