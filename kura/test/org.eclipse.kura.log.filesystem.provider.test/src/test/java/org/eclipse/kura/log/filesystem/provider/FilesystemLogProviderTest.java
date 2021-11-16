/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.log.filesystem.provider;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.log.LogEntry;
import org.eclipse.kura.log.listener.LogListener;

import org.junit.Test;
import org.mockito.Matchers;

public class FilesystemLogProviderTest {

    private int nLogLines = 0;
    private Map<String, Object> properties;
    private File file;
    private boolean exceptionOccured = false;
    private FilesystemLogProvider logProvider;
    private List<LogListener> listeners;

    /*
     * Scenarios
     */

    @Test
    public void allListenersShouldBeCalled() {
        givenFile("kuratest");
        givenPropertiesWithLogFilePath();
        givenFilesystemLogProvider();
        givenLogListeners(3);

        whenRegisteringLogListeners();
        whenActivate();
        whenSomeTimePasses();

        thenListenersGetCalled(this.nLogLines - 1);
        thenNoExceptionsOccurred();
    }

    @Test
    public void updateWithWrongProperties() {
        givenFile("kuratest");
        givenPropertiesWithLogFilePath();
        givenFilesystemLogProvider();
        givenLogListeners(3);

        whenRegisteringLogListeners();
        whenUpdateWithWrongProperties();
        whenSomeTimePasses();

        thenListenersAreNotCalled();
        thenNoExceptionsOccurred();
    }

    @Test
    public void updateWithCorrectProperties() {
        givenFile("kuratest");
        givenPropertiesWithLogFilePath();
        givenFilesystemLogProvider();
        givenLogListeners(3);

        whenRegisteringLogListeners();
        whenUpdateWithCorrectProperties();
        whenNewLinesAreAddedToFile(10);
        whenSomeTimePasses();

        thenListenersGetCalled(this.nLogLines - 11);
    }

    @Test
    public void deactivateShouldStopLogProvider() {
        givenFile("kuratest");
        givenPropertiesWithLogFilePath();
        givenFilesystemLogProvider();
        givenLogListeners(3);

        whenRegisteringLogListeners();
        whenActivate();
        whenSomeTimePasses();
        whenDeactivate();
        whenSomeTimePasses();
        whenNewLinesAreAddedToFile(10);

        thenListenersGetCalled(this.nLogLines - 11);
        thenNoExceptionsOccurred();
    }

    @Test
    public void unregisteredListenersShouldNotBeNotified() {
        givenFile("kuratest");
        givenPropertiesWithLogFilePath();
        givenFilesystemLogProvider();
        givenLogListeners(3);

        whenRegisteringLogListeners();
        whenActivate();
        whenSomeTimePasses();
        whenUnregisteringLogListeners();
        whenSomeTimePasses();
        whenNewLinesAreAddedToFile(10);

        thenListenersGetCalled(this.nLogLines - 11);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenFile(String filename) {
        this.exceptionOccured = false;
        try {
            this.file = File.createTempFile(filename, ".log");
            this.file.deleteOnExit();

            try (FileWriter writer = new FileWriter(this.file)) {
                writer.write("line01\n");
                writer.write("line02\n");
                writer.write("line03\n");
                writer.write("line04\n");
                writer.write("line05\n");
                writer.write("line06\n");
                writer.write("line07\n");
                writer.write("line08\n");
                writer.write("line09\n");
                writer.write("line10\n");
                this.nLogLines = 10;
            }
        } catch (IOException e) {
            this.exceptionOccured = true;
        }
    }

    private void givenPropertiesWithLogFilePath() {
        this.properties = new HashMap<>();
        this.properties.put(FilesystemLogProvider.LOG_FILEPATH_PROP_KEY, this.file.getAbsolutePath());
    }

    private void givenFilesystemLogProvider() {
        this.logProvider = new FilesystemLogProvider();
    }

    private void givenLogListeners(int nListeners) {
        this.listeners = new ArrayList<>(nListeners);
        for (int i = 0; i < nListeners; i++) {
            LogListener listener = mock(LogListener.class);
            this.listeners.add(listener);
        }
    }

    /*
     * When
     */

    private void whenRegisteringLogListeners() {
        for (LogListener listener : this.listeners) {
            this.logProvider.registerLogListener(listener);
        }
    }

    private void whenActivate() {
        this.logProvider.activate(this.properties);
    }

    private void whenUpdateWithWrongProperties() {
        this.properties = new HashMap<>();
        this.properties.put(FilesystemLogProvider.LOG_FILEPATH_PROP_KEY, "this/is/a/nonexistent/path");
        this.logProvider.updated(this.properties);
    }

    private void whenUpdateWithCorrectProperties() {
        this.properties = new HashMap<>();
        this.properties.put(FilesystemLogProvider.LOG_FILEPATH_PROP_KEY, this.file.getAbsolutePath());
        this.logProvider.updated(this.properties);
    }

    private void whenDeactivate() {
        this.logProvider.deactivate();
    }

    private void whenSomeTimePasses() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void whenNewLinesAreAddedToFile(int nLines) {
        try (FileWriter writer = new FileWriter(this.file)) {
            for (int i = 0; i < nLines; i++) {
                writer.write("line" + (this.nLogLines + i) + "\n");
            }
            this.nLogLines += nLines;
        } catch (IOException e) {
            this.exceptionOccured = true;
        }
    }

    private void whenUnregisteringLogListeners() {
        for (LogListener listener : this.listeners) {
            this.logProvider.unregisterLogListener(listener);
        }
        givenFile("kura");
    }

    /*
     * Then
     */

    private void thenListenersGetCalled(int times) {
        for (LogListener listener : this.listeners) {
            verify(listener, times(times)).newLogEntry(Matchers.any(LogEntry.class));
        }
    }

    private void thenListenersAreNotCalled() {
        for (LogListener listener : this.listeners) {
            verifyZeroInteractions(listener);
        }
    }

    private void thenNoExceptionsOccurred() {
        assertFalse(this.exceptionOccured);
    }
}
