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

import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.change.publisher.utils.CloudStackTrackerListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class CloudEndpointServiceTracker extends ServiceTracker<CloudEndpoint, CloudEndpoint> {

    private Set<CloudStackTrackerListener> listeners = new HashSet<>();

    public CloudEndpointServiceTracker(BundleContext context, String endpointPid) throws InvalidSyntaxException {
        super(context, context.createFilter(String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS,
                CloudEndpoint.class.getName(), ConfigurationService.KURA_SERVICE_PID, endpointPid)), null);
    }

    public void registerCloudStackTrackerListener(CloudStackTrackerListener listener) {
        this.listeners.add(listener);
    }

    public void unregisterCloudStackTrackerListener(CloudStackTrackerListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public CloudEndpoint addingService(ServiceReference<CloudEndpoint> reference) {
        CloudEndpoint endpoint = super.addingService(reference);
        
        String dataServicePid = (String) reference.getProperty("DataService.target");
        dataServicePid = dataServicePid.replace("(kura.service.pid=", "").replace(")", "");

        for (CloudStackTrackerListener listener : this.listeners) {
            listener.onCloudEndpointAdded(endpoint, dataServicePid);
        }

        return endpoint;
    }

    @Override
    public void removedService(ServiceReference<CloudEndpoint> reference, CloudEndpoint service) {
        super.removedService(reference, service);

        for (CloudStackTrackerListener listener : this.listeners) {
            listener.onCloudEndpointRemoved(service);
        }
    }

}
