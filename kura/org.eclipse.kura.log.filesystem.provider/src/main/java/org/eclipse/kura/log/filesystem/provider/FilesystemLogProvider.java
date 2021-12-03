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
 *******************************************************************************/
package org.eclipse.kura.log.filesystem.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.log.LogEntry;
import org.eclipse.kura.log.LogProvider;
import org.eclipse.kura.log.listener.LogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemLogProvider implements ConfigurableComponent, LogProvider {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemLogProvider.class);
    public static final String LOG_FILEPATH_PROP_KEY = "logFilePath";

    private final List<LogListener> registeredListeners = new LinkedList<>();
    private FileLogReader readerThread;
    private String filePath;

    protected void activate(Map<String, Object> properties) {
        logger.info("Activating FilesystemLogProvider...");
        updated(properties);
        logger.info("Activating FilesystemLogProvider... Done.");
    }

    protected void deactivate() {
        logger.info("Deactivating FilesystemLogProvider...");
        if (this.readerThread != null) {
            this.readerThread.interrupt();
        }
        logger.info("Deactivating FilesystemLogProvider... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated FilesystemLogProvider...");
        if (this.readerThread != null) {
            this.readerThread.interrupt();
        }
        this.filePath = (String) properties.get(LOG_FILEPATH_PROP_KEY);
        this.readerThread = new FileLogReader(this.filePath);
        this.readerThread.start();
        logger.info("Updated FilesystemLogProvider... Done.");
    }

    @Override
    public void registerLogListener(LogListener listener) {
        this.registeredListeners.add(listener);
    }

    @Override
    public void unregisterLogListener(LogListener listener) {
        this.registeredListeners.remove(listener);
    }

    class FileLogReader extends Thread {

        private static final long SAMPLE_INTERVAL = 100;
        private final File logFile;
        private boolean follow = true;

        public FileLogReader(String filePath) {
            this.logFile = new File(filePath);
            this.follow = true;
        }

        @Override
        public void run() {
            long filePointer = 0;

            try (RandomAccessFile file = new RandomAccessFile(this.logFile, "r")) {
                while (this.follow) {
                    long fileLength = this.logFile.length();
                    if (fileLength < filePointer) {
                        filePointer = fileLength;
                    }

                    if (filePointer < fileLength) {
                        file.seek(filePointer);
                        readLinesAndNotifyListeners(file);
                        if (filePointer < file.getFilePointer()) {
                            filePointer = file.getFilePointer();
                        }
                    }

                    sleep(SAMPLE_INTERVAL);
                }
            } catch (FileNotFoundException fnf) {
                logger.error("File '{}' not found.", this.logFile.getPath());
            } catch (InterruptedException ie) {
                // nothing to do
            } catch (Exception e) {
                logger.error("Unexpected exception in FilesystemLogProvider.", e);
            } finally {
                Thread.currentThread().interrupt();
            }
        }

        private void readLinesAndNotifyListeners(RandomAccessFile file) throws IOException {
            String line = file.readLine();
            while (line != null) {

                String message = line;

                // look ahead for stacktrace
                StringBuilder stacktrace = new StringBuilder();
                line = file.readLine();
                while (line != null && isStacktrace(line)) {
                    stacktrace.append(line);
                    stacktrace.append("\n");
                    line = file.readLine();
                }

                notifyListeners(message, stacktrace.toString().trim());
            }
        }

        private boolean isStacktrace(String line) {
            /*
             * stacktrace lines do not start with a timestamp
             * 
             * in kura-audit log file the lines start with a '<'
             */
            return line.length() > 4 && !line.substring(0, 4).matches("\\d{4}") && !line.startsWith("<");
        }

        private void notifyListeners(String message, String stacktrace) {
            if (message != null) {
                for (LogListener listener : FilesystemLogProvider.this.registeredListeners) {
                    LogEntry entry = KuraLogLineParser.createLogEntry(message, FilesystemLogProvider.this.filePath,
                            stacktrace);
                    listener.newLogEntry(entry);
                }
            }
        }
    }

}
