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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

public class EmulatorTest {

    private static ComponentContext componentContext;

    private static String tempDir = System.getProperty("java.io.tmpdir");

    private static final String SNAPSHOT_0_NAME = "snapshot_0.xml";

    private static final String KURA_SNAPSHOTS_PATH = "kura.snapshots";

    private static final String EMULATOR = "emulator";

    private static final String KURA_MODE = "org.eclipse.kura.mode";

    private static final Path snapshotsFolder = Paths.get(tempDir, "snapshots");
    private static final Path snapshot0File = Paths.get(snapshotsFolder.toString(), SNAPSHOT_0_NAME);

    @BeforeClass
    public static void init() throws IOException {

        System.setProperty(KURA_SNAPSHOTS_PATH, snapshotsFolder.toString());
        System.setProperty(KURA_MODE, EMULATOR);

        URL snapshotUrl = Emulator.class.getClassLoader().getResource(SNAPSHOT_0_NAME);

        componentContext = mock(ComponentContext.class, Mockito.RETURNS_DEEP_STUBS);
        when(componentContext.getBundleContext().getBundle().getResource(any())).thenReturn(snapshotUrl);
    }

    @Test
    public void testSnapshotCopy() {
        Emulator emulator = new Emulator();
        emulator.activate(componentContext);
        assertTrue("Snapshot doesn't exist", Files.exists(snapshot0File));
    }

    @After
    @Before
    public void cleanUp() throws IOException {
        Files.deleteIfExists(snapshot0File);
        Files.deleteIfExists(snapshotsFolder);
    }
}
