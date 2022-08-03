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
package org.eclipse.kura.configuration.change.publisher.utils.trackers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.change.publisher.utils.CloudStackTrackerListener;
import org.eclipse.kura.data.DataService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class DataServiceTracker extends ServiceTracker<DataService, DataService> {

    private Set<CloudStackTrackerListener> listeners = new HashSet<>();

    public DataServiceTracker(BundleContext context, String dataServicePid) throws InvalidSyntaxException {
        super(context, context.createFilter(String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS,
                DataService.class.getName(), ConfigurationService.KURA_SERVICE_PID, dataServicePid)), null);
    }

    public void registerCloudStackTrackerListener(CloudStackTrackerListener listener) {
        this.listeners.add(listener);
    }

    public void unregisterCloudStackTrackerListener(CloudStackTrackerListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public DataService addingService(ServiceReference<DataService> reference) {
        DataService dataService = super.addingService(reference);
        
        String dataTransportServicePid = (String) reference.getProperty("DataTransportService.target");
        dataTransportServicePid = dataTransportServicePid.replace("(kura.service.pid=", "").replace(")", "");

        for (CloudStackTrackerListener listener : this.listeners) {
            listener.onDataServiceAdded(dataService, dataTransportServicePid);
        }

        return dataService;
    }

    @Override
    public void removedService(ServiceReference<DataService> reference, DataService service) {
        super.removedService(reference, service);

        for (CloudStackTrackerListener listener : this.listeners) {
            listener.onDataServiceRemoved(service);
        }
    }

}
