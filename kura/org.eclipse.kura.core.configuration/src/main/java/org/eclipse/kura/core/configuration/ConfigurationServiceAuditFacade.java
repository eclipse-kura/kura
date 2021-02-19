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

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public Set<String> getFactoryComponentPids() {
        return audit(super::getFactoryComponentPids, "Get factory component pids");
    }

    @Override
    public synchronized void createFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties,
            boolean takeSnapshot) throws KuraException {
        audit(() -> super.createFactoryConfiguration(factoryPid, pid, properties, takeSnapshot),
                "Create factory configuration " + factoryPid + " " + pid);
    }

    @Override
    public synchronized void deleteFactoryConfiguration(String pid, boolean takeSnapshot) throws KuraException {
        audit(() -> super.deleteFactoryConfiguration(pid, takeSnapshot), "Delete factory configuration: " + pid);
    }

    @Override
    public Set<String> getConfigurableComponentPids() {
        return audit(super::getConfigurableComponentPids, "Get configurable component pids");
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
    public ComponentConfiguration getDefaultComponentConfiguration(String pid) throws KuraException {
        return audit(() -> super.getDefaultComponentConfiguration(pid), "Get default component configuration: " + pid);
    }

    @Override
    public synchronized void updateConfiguration(String pid, Map<String, Object> properties) throws KuraException {
        audit(() -> super.updateConfiguration(pid, properties), "Update configuration: " + pid);
    }

    @Override
    public synchronized void updateConfiguration(String pid, Map<String, Object> properties, boolean takeSnapshot)
            throws KuraException {
        audit(() -> super.updateConfiguration(pid, properties, takeSnapshot), "Update configuration: " + pid);
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
    public Set<Long> getSnapshots() throws KuraException {
        return audit(super::getSnapshots, "Get snapshots");
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
    }

    private static <T, E extends Throwable> T audit(final FallibleSupplier<T, E> task, final String message) throws E {
        try {
            final T result = task.get();
            auditLogger.info(CONFIGURATION_SERVICE_SUCCESS, AuditContext.current(), message);
            return result;
        } catch (final Exception e) {
            auditLogger.warn(CONFIGURATION_SERVICE_FAILURE, AuditContext.current(), message);
            throw e;
        }
    }

    private static <E extends Throwable> void audit(final FallibleTask<E> task, final String message) throws E {
        try {
            task.run();
            auditLogger.info(CONFIGURATION_SERVICE_SUCCESS, AuditContext.current(), message);
        } catch (final Exception e) {
            auditLogger.warn(CONFIGURATION_SERVICE_FAILURE, AuditContext.current(), message);
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
}
