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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class SqliteProviderActivatorTest {

    @Test
    public void shouldSetSqliteTempDirIfUnset() {
        givenNoSystemPropertyValue("org.sqlite.tmpdir");
        givenBundleStorageAreaPath("/tmp/foo");

        whenActivatorIsStarted();

        thenNoExceptionIsThrown();
        thenSystemPropertyValueIs("org.sqlite.tmpdir", "/tmp/foo");
    }

    @Test
    public void shouldNotSetSqliteTempDirIfBundleStorageAreaIsNotAvailable() {
        givenNoSystemPropertyValue("org.sqlite.tmpdir");
        givenNoBundleStorageArea();

        whenActivatorIsStarted();

        thenNoExceptionIsThrown();
        thenSystemPropertyValueIs("org.sqlite.tmpdir", null);
    }

    @Test
    public void shouldChangeSqliteTempDirIfSetButNonExisting() {
        givenSystemProperty("org.sqlite.tmpdir", "bar");
        givenBundleStorageAreaPath("/tmp/foo");

        whenActivatorIsStarted();

        thenNoExceptionIsThrown();
        thenSystemPropertyValueIs("org.sqlite.tmpdir", "/tmp/foo");
    }

    @Test
    public void shouldNotChangeSqliteTempDirIfSetAndExisting() {
        givenSystemProperty("org.sqlite.tmpdir", temporaryDirectoryPath());
        givenBundleStorageAreaPath("/tmp/foo");

        whenActivatorIsStarted();

        thenNoExceptionIsThrown();
        thenSystemPropertyValueIs("org.sqlite.tmpdir", temporaryDirectoryPath());
    }

    @Test
    public void shouldClearSqliteTempDirOnStopIfChanged() {
        givenNoSystemPropertyValue("org.sqlite.tmpdir");
        givenBundleStorageAreaPath("/tmp/foo");
        givenStartedActivator();

        whenActivatorIsStopped();

        thenNoExceptionIsThrown();
        thenSystemPropertyValueIs("org.sqlite.tmpdir", null);
    }

    @Test
    public void shouldNotClearSqliteTempDirOnStopIfNotChanged() {
        givenSystemProperty("org.sqlite.tmpdir", temporaryDirectoryPath());
        givenBundleStorageAreaPath("/tmp/foo");
        givenStartedActivator();

        whenActivatorIsStopped();

        thenNoExceptionIsThrown();
        thenSystemPropertyValueIs("org.sqlite.tmpdir", temporaryDirectoryPath());
    }

    private final BundleContext bundleContext = Mockito.mock(BundleContext.class);
    private Optional<Exception> exception = Optional.empty();
    private Optional<String> temporaryDirectoryPath = Optional.empty();
    private SqliteProviderActivator activator = new SqliteProviderActivator();

    private void givenBundleStorageAreaPath(String path) {
        Mockito.when(bundleContext.getDataFile("")).thenReturn(new File(path));
    }

    private void givenNoBundleStorageArea() {
        Mockito.when(bundleContext.getDataFile("")).thenReturn(null);
    }

    private void givenSystemProperty(final String key, final String value) {
        System.setProperty(key, value);
    }

    private void givenNoSystemPropertyValue(final String key) {
        System.clearProperty(key);
    }

    private void givenStartedActivator() {
        whenActivatorIsStarted();
        thenNoExceptionIsThrown();
    }

    private void whenActivatorIsStarted() {
        try {
            this.exception = Optional.empty();
            activator.start(bundleContext);
        } catch (Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void whenActivatorIsStopped() {
        try {
            this.exception = Optional.empty();
            activator.stop(bundleContext);
        } catch (Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void thenSystemPropertyValueIs(final String key, final String value) {
        assertEquals(value, System.getProperty(key));
    }

    private void thenNoExceptionIsThrown() {
        assertEquals(Optional.empty(), this.exception);
    }

    private String temporaryDirectoryPath() {
        if (temporaryDirectoryPath.isPresent()) {
            return temporaryDirectoryPath.get();
        }

        try {
            final String newPath = Files.createTempDirectory(null).toFile().getAbsolutePath();
            this.temporaryDirectoryPath = Optional.of(newPath);
            return newPath;
        } catch (final Exception e) {
            fail("Cannot create temporary directory");
            throw new IllegalStateException("unreachable");
        }
    }
}
