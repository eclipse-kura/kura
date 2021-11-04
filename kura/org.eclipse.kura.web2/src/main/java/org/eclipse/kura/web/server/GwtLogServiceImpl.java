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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.log.LogEntry;
import org.eclipse.kura.log.LogReader;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtLogEntry;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtLogService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtLogServiceImpl extends OsgiRemoteServiceServlet implements GwtLogService {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GwtLogServiceImpl.class);

    private final LogEntriesCache cache = new LogEntriesCache();
    private final Map<String, LogReader> registeredLogReaders = new HashMap<>();

    @Override
    public List<String> initLogReaders(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        loadLogReaders();
        registerLogReaderListeners();

        List<String> registeredPids = new ArrayList<>();
        registeredPids.addAll(this.registeredLogReaders.keySet());
        return registeredPids;
    }

    @Override
    public List<GwtLogEntry> readLogs() throws GwtKuraException {
        return this.cache.getLogs();
    }

    private void loadLogReaders() {
        logger.info("Loading LogReaders.");

        try {
            final String MATCH_EVERYTHING = "(objectClass=*)";
            List<ServiceReference<LogReader>> logReaderRefs = (List<ServiceReference<LogReader>>) ServiceLocator
                    .getInstance().getServiceReferences(LogReader.class, MATCH_EVERYTHING);

            this.registeredLogReaders.clear();

            for (ServiceReference<LogReader> logReaderRef : logReaderRefs) {
                String pid = (String) logReaderRef.getProperty(ConfigurationService.KURA_SERVICE_PID);
                LogReader service = FrameworkUtil.getBundle(LogReader.class).getBundleContext()
                        .getService(logReaderRef);

                this.registeredLogReaders.put(pid, service);
            }
        } catch (GwtKuraException e) {
            logger.error("Error loading log readers.");
        }

        logger.info("Loaded LogReaders: {}", this.registeredLogReaders.keySet());
    }

    private void registerLogReaderListeners() {

        for (Entry<String, LogReader> registeredLogReader : this.registeredLogReaders.entrySet()) {

            String whichLogReader = registeredLogReader.getKey();
            LogReader logReader = registeredLogReader.getValue();

            if (logReader != null) {

                logReader.registerLogListener((LogEntry entry) -> {
                    GwtLogEntry gwtEntry = new GwtLogEntry();
                    gwtEntry.setProperties(entry.getProperties());
                    gwtEntry.setSourceLogReaderPid(whichLogReader);

                    GwtLogServiceImpl.this.cache.add(gwtEntry);
                });

                logger.info("Registered LogReaderListener for {}.", whichLogReader);
            }
        }
    }

    private final class LogEntriesCache {

        private List<GwtLogEntry> cache = new LinkedList<>();
        private static final int MAX_CACHE_SIZE = 1000;

        public void add(GwtLogEntry newEntry) {
            synchronized (this.cache) {
                if (this.cache.size() >= MAX_CACHE_SIZE) {
                    this.cache.remove(0);
                }
                this.cache.add(newEntry);
            }
        }

        public synchronized List<GwtLogEntry> getLogs() {
            List<GwtLogEntry> result = new ArrayList<>();
            synchronized (this.cache) {
                result.addAll(this.cache);
                this.cache.clear();
            }
            return result;
        }
    }
}
