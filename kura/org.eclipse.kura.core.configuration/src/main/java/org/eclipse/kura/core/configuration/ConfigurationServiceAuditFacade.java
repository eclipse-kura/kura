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
package org.eclipse.kura.core.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationServiceAuditFacade extends ConfigurationServiceImpl {

    private static final String CONFIGURATION_SERVICE_FAILURE = "{} ConfigurationService - Failure - {}";
    private static final String CONFIGURATION_SERVICE_SUCCESS = "{} ConfigurationService - Success - {}";
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    @Override
    public synchronized void createFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties,
            boolean takeSnapshot) throws KuraException {
        audit(() -> super.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot),
                "Create factory configuration " + factoryPid + " " + pid);
        postConfigurationChangedEvent("created", pid);
    }

    @Override
    public synchronized void deleteFactoryConfiguration(String pid, boolean takeSnapshot) throws KuraException {
        audit(() -> super.deleteFactoryConfiguration(pid, takeSnapshot), "Delete factory configuration: " + pid);
        postConfigurationChangedEvent("deleted", pid);
    }

    @Override
    public List<ComponentConfiguration> getComponentConfigurations() throws KuraException {
        return audit(() -> super.getComponentConfigurations(), "Get component configurations");
    }

    @Override
    public List<ComponentConfiguration> getComponentConfigurations(Filter filter) throws KuraException {
        return audit(() -> super.getComponentConfigurations(filter), "Get component configurations: " + filter);
    }

    @Override
    public ComponentConfiguration getComponentConfiguration(String pid) throws KuraException {
        return audit(() -> super.getComponentConfiguration(pid), "Get component configuration: " + pid);
    }

    @Override
    public synchronized void updateConfiguration(String pid, Map<String, Object> properties) throws KuraException {
        audit(() -> super.updateConfiguration(pid, properties), "Update configuration: " + pid);
        postConfigurationChangedEvent("updated", pid);
    }

    @Override
    public synchronized void updateConfiguration(String pid, Map<String, Object> properties, boolean takeSnapshot)
            throws KuraException {
        audit(() -> super.updateConfiguration(pid, properties, takeSnapshot), "Update configuration: " + pid);
        postConfigurationChangedEvent("updated", pid);
    }

    @Override
    public synchronized void updateConfigurations(List<ComponentConfiguration> configs) throws KuraException {
        audit(() -> super.updateConfigurations(configs), "Update configurations: " + formatConfigurationPids(configs));
    }

    @Override
    public synchronized void updateConfigurations(List<ComponentConfiguration> configs, boolean takeSnapshot)
            throws KuraException {
        audit(() -> super.updateConfigurations(configs, takeSnapshot),
                "Update configurations: " + formatConfigurationPids(configs));
    }

    @Override
    public List<ComponentConfiguration> getSnapshot(long sid) throws KuraException {
        return audit(() -> super.getSnapshot(sid), "Get snapshot: " + sid);
    }

    @Override
    public long snapshot() throws KuraException {
        return audit(super::snapshot, "Take snapshot");
    }

    @Override
    public long rollback() throws KuraException {
        return audit(() -> super.rollback(), "Rollback latest snapshot");
    }

    @Override
    public synchronized void rollback(long id) throws KuraException {
        audit(() -> super.rollback(id), "Rollback snapshot: " + id);
        postConfigurationChangedEvent("made a", "rollback");
    }

    private static <T, E extends Throwable> T audit(final FallibleSupplier<T, E> task, final String message) throws E {
        try {
            final T result = task.get();
            auditLogger.info(CONFIGURATION_SERVICE_SUCCESS, AuditContext.currentOrInternal(), message);
            return result;
        } catch (final Exception e) {
            auditLogger.warn(CONFIGURATION_SERVICE_FAILURE, AuditContext.currentOrInternal(), message);
            throw e;
        }
    }

    private static <E extends Throwable> void audit(final FallibleTask<E> task, final String message) throws E {
        try {
            task.run();
            auditLogger.info(CONFIGURATION_SERVICE_SUCCESS, AuditContext.currentOrInternal(), message);
        } catch (final Exception e) {
            auditLogger.warn(CONFIGURATION_SERVICE_FAILURE, AuditContext.currentOrInternal(), message);
            throw e;
        }
    }

    private String formatConfigurationPids(final List<ComponentConfiguration> configs) {
        return configs.stream().map(ComponentConfiguration::getPid).reduce("", (a, b) -> a + " " + b);
    }

    private interface FallibleSupplier<T, E extends Throwable> {

        public T get() throws E;
    }

    private interface FallibleTask<E extends Throwable> {

        public void run() throws E;
    }

    private void postConfigurationChangedEvent(String changedComponentMessage, String pid) {
        Optional<AuditContext> auditContext = AuditContext.current();

        if (auditContext.isPresent()) {
            String sessionId = auditContext.get().getProperties().get("session.id");

            Map<String, String> properties = new HashMap<>();
            properties.put(ConfigurationChangeEvent.CONF_CHANGE_EVENT_INFO_PROP, changedComponentMessage + " " + pid);
            properties.put(ConfigurationChangeEvent.CONF_CHANGE_EVENT_PID_PROP, pid);
            properties.put(ConfigurationChangeEvent.CONF_CHANGE_EVENT_SESSION_PROP, sessionId);

            if (sessionId != null) {
                this.eventAdmin.postEvent(new ConfigurationChangeEvent(properties));
            }
        }
    }
}
