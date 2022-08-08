/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.configuration.change.manager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("rawtypes")
public class ComponentsServiceTracker extends ServiceTracker {

    private final Set<ServiceTrackerListener> listeners;
    private static final String FILTER_EXCLUDE_CONF_CHANGE_MANAGER_FACTORY = "(!(service.factoryPid=org.eclipse.kura.configuration.change.manager.ConfigurationChangeManager))";

    @SuppressWarnings("unchecked")
    public ComponentsServiceTracker(BundleContext context) throws InvalidSyntaxException {
        super(context, context.createFilter(FILTER_EXCLUDE_CONF_CHANGE_MANAGER_FACTORY), null);
        this.listeners = new HashSet<>();
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Object addingService(ServiceReference ref) {
        Object service = super.addingService(ref);

        notifyListeners(ref);

        return service;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        notifyListeners(reference);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void removedService(ServiceReference reference, Object service) {
        super.removedService(reference, service);

        notifyListeners(reference);
    }

    public void addServiceTrackerListener(ServiceTrackerListener listener) {
        this.listeners.add(listener);
    }

    public void removeServiceTrackerListener(ServiceTrackerListener listener) {
        this.listeners.remove(listener);
    }

    private void notifyListeners(ServiceReference ref) {
        Optional<String> pid = getPidFromServiceReference(ref);

        if (pid.isPresent()) {
            for (ServiceTrackerListener listener : this.listeners) {
                listener.onConfigurationChanged(pid.get());
            }
        }
    }

    private Optional<String> getPidFromServiceReference(ServiceReference ref) {
        Optional<String> pid = Optional.empty();

        if (ref.getProperty(ConfigurationService.KURA_SERVICE_PID) != null) {
            pid = Optional.of((String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID));
        } else if (ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID) != null) {
            pid = Optional.of((String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID));
        } else if (ref.getProperty(Constants.SERVICE_PID) != null) {
            pid = Optional.of((String) ref.getProperty(Constants.SERVICE_PID));
        }

        return pid;
    }
}