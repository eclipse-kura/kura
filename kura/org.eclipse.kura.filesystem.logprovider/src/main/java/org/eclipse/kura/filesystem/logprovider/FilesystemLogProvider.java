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
package org.eclipse.kura.filesystem.logprovider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.log.LogProvider;
import org.eclipse.kura.log.LogEntry;
import org.eclipse.kura.log.listener.LogListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemLogProvider implements ConfigurableComponent, LogProvider {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemLogProvider.class);

    private List<LogListener> registeredListeners = new LinkedList<>();
    private FileLogReader readerThread;
    private String filePath;

    protected void activate(Map<String, Object> properties) {
        logger.info("Activating FilesystemLogProvider...");
        this.filePath = (String) properties.get("logFilePath");
        this.readerThread = new FileLogReader(this.filePath);
        this.readerThread.start();
        logger.info("Activating FilesystemLogProvider... Done.");
    }

    protected void deactivate() {
        logger.info("Deactivating FilesystemLogProvider...");
        this.readerThread.interrupt();
        logger.info("Deactivating FilesystemLogProvider... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated FilesystemLogProvider...");
        this.readerThread.interrupt();
        this.filePath = (String) properties.get("logFilePath");
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

    private void notifyListeners(String newLogLine) {

        if (newLogLine != null) {
            String timestamp = "example_timestamp";
            String pid = "example_pid";
            String priority = "example_priority";
            String message = newLogLine;

            if (this.filePath.contains("kura.log")) {
                /*
                 * kura.log message format:
                 * 
                 * _SOURCE_REALTIME_TIMESTAMP [PID] PRIORITY MESSAGE_WITH_POSSIBLE_SPACES
                 */
                String[] splits = newLogLine.split(" ");
                if (splits.length > 3) {
                    timestamp = splits[0];
                    pid = splits[1];
                    pid = pid.replace("[", "");
                    pid = pid.replace("]", "");
                    priority = splits[2];
                    StringBuilder sb = new StringBuilder();
                    for (int i = 3; i < splits.length; i++) {
                        sb.append(splits[i]);
                        sb.append(" ");
                    }
                    message = sb.toString();
                }
            }

            for (LogListener listener : this.registeredListeners) {
                Map<String, Object> entryProperties = new HashMap<>();
                entryProperties.put("_SOURCE_REALTIME_TIMESTAMP", timestamp);
                entryProperties.put("_PID", pid);
                entryProperties.put("MESSAGE", message.trim());
                entryProperties.put("PRIORITY", priority);
                entryProperties.put("SYSLOG_IDENTIFIER", "Kura");
                entryProperties.put("_TRANSPORT", this.filePath);
                listener.newLogEntry(new LogEntry(entryProperties));
            }
        }
    }

    public class FileLogReader extends Thread {

        private static final long SAMPLE_INTERVAL = 100;
        private File logFile;
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

                        String line = file.readLine();
                        while (line != null) {
                            line = file.readLine();
                            notifyListeners(line);
                        }

                        filePointer = file.getFilePointer();
                    }

                    sleep(SAMPLE_INTERVAL);
                }
            } catch (FileNotFoundException fnf) {
                logger.error("File '{}' not found.", this.logFile);
            } catch (IOException ioe) {
                logger.error("IOException {}", ioe.getStackTrace());
            } catch (Exception e) {
                logger.error("Unexpected exception in FilesystemLogProvider: {}", e.getStackTrace());
            } finally {
                Thread.currentThread().interrupt();
            }
        }
    }

}
