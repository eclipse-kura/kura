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
import java.util.LinkedList;
import java.util.List;

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

    private static final LogEntriesCache cache = new LogEntriesCache();
    private static final List<String> registeredLogProviders = new LinkedList<>();

    @Override
    public List<String> initLogProviders(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        loadLogProviders();

        return registeredLogProviders;
    }

    @Override
    public List<GwtLogEntry> readLogs(int fromId) throws GwtKuraException {
        return cache.getLogs(fromId);
    }

    private void loadLogProviders() {
        try {
            List<String> availableLogProviders = new ArrayList<>();

            final String MATCH_EVERYTHING = "(objectClass=*)";
            List<ServiceReference<LogProvider>> logProviderRefs = (List<ServiceReference<LogProvider>>) ServiceLocator
                    .getInstance().getServiceReferences(LogProvider.class, MATCH_EVERYTHING);

            for (ServiceReference<LogProvider> logProviderRef : logProviderRefs) {
                String pid = (String) logProviderRef.getProperty(ConfigurationService.KURA_SERVICE_PID);
                LogProvider service = FrameworkUtil.getBundle(LogProvider.class).getBundleContext()
                        .getService(logProviderRef);

                availableLogProviders.add(pid);

                if (pid != null && service != null && !registeredLogProviders.contains(pid)) {

                    service.registerLogListener((LogEntry entry) -> {
                        GwtLogEntry gwtEntry = new GwtLogEntry();
                        gwtEntry.setProperties(entry.getProperties());
                        gwtEntry.setSourceLogProviderPid(pid);

                        GwtLogServiceImpl.cache.add(gwtEntry);
                    });

                    registeredLogProviders.add(pid);
                    logger.info("LogProvider {} loaded.", pid);
                }
            }

            for (String pid : registeredLogProviders) {
                if (!availableLogProviders.contains(pid)) {
                    registeredLogProviders.remove(pid);
                    logger.info("LogProvider {} no more available.", pid);
                }
            }
        } catch (GwtKuraException e) {
            logger.error("Error loading log providers.");
        }
    }

    private static final class LogEntriesCache {

        private static final List<GwtLogEntry> cache = new LinkedList<>();
        private static final int MAX_CACHE_SIZE = 1000;
        private static int nextEntryId = 0;

        public void add(GwtLogEntry newEntry) {
            synchronized (cache) {
                if (cache.size() >= MAX_CACHE_SIZE) {
                    cache.remove(0);
                }
                manageIdIntOverflow();
                newEntry.setId(nextEntryId++);
                cache.add(newEntry);
            }
        }

        public List<GwtLogEntry> getLogs(int fromId) {
            List<GwtLogEntry> result = new LinkedList<>();
            synchronized (cache) {
                cache.forEach(entry -> {
                    if (entry.getId() > fromId) {
                        result.add(entry);
                    }
                });
            }
            return result;
        }

        /*
         * Very unlikely to happen, but if it will then entries are reindexed
         */
        private static void manageIdIntOverflow() {
            if (nextEntryId >= Integer.MAX_VALUE) {
                logger.info("ID overflow for cached UI log entries. Reindexing.");

                for (int i = 0; i < cache.size(); i++) {
                    cache.get(i).setId(i);
                }
                nextEntryId = cache.size();
            }
        }
    }
}
