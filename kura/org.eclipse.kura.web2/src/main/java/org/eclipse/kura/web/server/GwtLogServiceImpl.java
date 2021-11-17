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
import org.eclipse.kura.log.LogProvider;
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
    private final Map<String, LogProvider> registeredLogProviders = new HashMap<>();

    @Override
    public List<String> initLogProviders(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        loadLogProviders();
        registerLogListeners();

        return new ArrayList<>(this.registeredLogProviders.keySet());
    }

    @Override
    public List<GwtLogEntry> readLogs() throws GwtKuraException {
        return this.cache.getLogs();
    }

    private void loadLogProviders() {
        logger.info("Loading log providers.");

        try {
            final String MATCH_EVERYTHING = "(objectClass=*)";
            List<ServiceReference<LogProvider>> logProviderRefs = (List<ServiceReference<LogProvider>>) ServiceLocator
                    .getInstance().getServiceReferences(LogProvider.class, MATCH_EVERYTHING);

            this.registeredLogProviders.clear();

            for (ServiceReference<LogProvider> logProviderRef : logProviderRefs) {
                String pid = (String) logProviderRef.getProperty(ConfigurationService.KURA_SERVICE_PID);
                LogProvider service = FrameworkUtil.getBundle(LogProvider.class).getBundleContext()
                        .getService(logProviderRef);

                this.registeredLogProviders.put(pid, service);
            }
        } catch (GwtKuraException e) {
            logger.error("Error loading log providers.");
        }

        logger.info("Loaded log providers: {}", this.registeredLogProviders.keySet());
    }

    private void registerLogListeners() {

        for (Entry<String, LogProvider> registeredLogProvider : this.registeredLogProviders.entrySet()) {

            String whichLogProvider = registeredLogProvider.getKey();
            LogProvider logProvider = registeredLogProvider.getValue();

            if (logProvider != null) {

                logProvider.registerLogListener((LogEntry entry) -> {
                    GwtLogEntry gwtEntry = new GwtLogEntry();
                    gwtEntry.setProperties(entry.getProperties());
                    gwtEntry.setSourceLogProviderPid(whichLogProvider);

                    GwtLogServiceImpl.this.cache.add(gwtEntry);
                });

                logger.info("Registered LogListener for {}.", whichLogProvider);
            }
        }
    }

    private final class LogEntriesCache {

        private final List<GwtLogEntry> cache = new LinkedList<>();
        private static final int MAX_CACHE_SIZE = 1000;

        public void add(GwtLogEntry newEntry) {
            synchronized (this.cache) {
                if (this.cache.size() >= MAX_CACHE_SIZE) {
                    this.cache.remove(0);
                }
                this.cache.add(newEntry);
            }
        }

        public List<GwtLogEntry> getLogs() {
            List<GwtLogEntry> result = new ArrayList<>();
            synchronized (this.cache) {
                result.addAll(this.cache);
                this.cache.clear();
            }
            return result;
        }
    }
}
