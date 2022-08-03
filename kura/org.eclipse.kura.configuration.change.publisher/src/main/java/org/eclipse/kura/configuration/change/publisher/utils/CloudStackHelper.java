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
package org.eclipse.kura.configuration.change.publisher.utils;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.configuration.change.publisher.utils.trackers.CloudEndpointServiceTracker;
import org.eclipse.kura.configuration.change.publisher.utils.trackers.DataServiceTracker;
import org.eclipse.kura.configuration.change.publisher.utils.trackers.DataTransportServiceTracker;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudStackHelper implements CloudStackTrackerListener {

    private static final Logger logger = LoggerFactory.getLogger(CloudStackHelper.class);

    private CloudEndpointServiceTracker cloudEndpointTracker;
    private DataServiceTracker dataServiceTracker;
    private DataTransportServiceTracker dataTransportServiceTracker;
    private BundleContext context;
    private CloudEndpoint cloudEndpoint;
    private DataService dataService;
    private DataTransportService dataTransportService;

    public CloudStackHelper(BundleContext context, String endpointPid) {
        this.context = context;

        initCloudEndpointTracker(endpointPid);
    }

    public void close() {
        if (this.cloudEndpointTracker != null) {
            logger.debug("Closing CloudEndpoint tracker...");
            this.cloudEndpointTracker.close();
            this.cloudEndpointTracker.unregisterCloudStackTrackerListener(this);
            this.cloudEndpointTracker = null;
            this.cloudEndpoint = null;
            logger.debug("Closing CloudEndpoint tracker... Done.");
        }
        
        if (this.dataServiceTracker != null) {
            logger.debug("Closing DataService tracker...");
            this.dataServiceTracker.close();
            this.dataServiceTracker.unregisterCloudStackTrackerListener(this);
            this.dataServiceTracker = null;
            this.dataService = null;
            logger.debug("Closing DataService tracker... Done.");
        }
        
        if (this.dataTransportServiceTracker != null) {
            logger.debug("Closing DataTransportService tracker...");
            this.dataTransportServiceTracker.close();
            this.dataTransportServiceTracker.unregisterCloudStackTrackerListener(this);
            this.dataTransportServiceTracker = null;
            this.dataTransportService = null;
            logger.debug("Closing DataTransportService tracker... Done.");
        }
    }

    public String publish(KuraMessage message) throws KuraException {
        if (this.cloudEndpoint == null) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "CloudEndpoint not available");
        }
        
        return this.cloudEndpoint.publish(message);
    }

    public String getAccountName() throws KuraException {
        if (this.dataTransportService == null) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "DataTransportService not available");
        }

        return this.dataTransportService.getAccountName();
    }

    public String getClientId() throws KuraException {
        if (this.dataTransportService == null) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "DataTransportService not available");
        }
        
        return this.dataTransportService.getClientId();
    }

    private void initCloudEndpointTracker(String endpointPid) {
        try {
            this.cloudEndpointTracker = new CloudEndpointServiceTracker(this.context, endpointPid);
            this.cloudEndpointTracker.registerCloudStackTrackerListener(this);
            this.cloudEndpointTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Service tracker filter setup exception.", e);
        }
    }

    private void initDataServiceTracker(String dataServicePid) {
        try {
            this.dataServiceTracker = new DataServiceTracker(this.context, dataServicePid);
            this.dataServiceTracker.registerCloudStackTrackerListener(this);
            this.dataServiceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Service tracker filter setup exception.", e);
        }
    }

    private void initDataTransportServiceTracker(String dataTransportServicePid) {
        try {
            this.dataTransportServiceTracker = new DataTransportServiceTracker(this.context, dataTransportServicePid);
            this.dataTransportServiceTracker.registerCloudStackTrackerListener(this);
            this.dataTransportServiceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Service tracker filter setup exception.", e);
        }
    }

    @Override
    public void onCloudEndpointAdded(CloudEndpoint cloudEndpoint, String dataServicePid) {
        this.cloudEndpoint = cloudEndpoint;
        initDataServiceTracker(dataServicePid);
    }

    @Override
    public void onCloudEndpointRemoved(CloudEndpoint cloudEndpoint) {
        this.cloudEndpoint = null;
    }

    @Override
    public void onDataServiceAdded(DataService dataService, String dataTransportServicePid) {
        this.dataService = dataService;
        initDataTransportServiceTracker(dataTransportServicePid);
    }

    @Override
    public void onDataServiceRemoved(DataService dataService) {
        this.dataService = null;
    }

    @Override
    public void onDataTransportServiceAdded(DataTransportService dataTransportService) {
        this.dataTransportService = dataTransportService;
    }

    public void onDataTransportServiceRemoved(DataTransportService dataTransportService) {
        this.dataTransportService = null;
    }

}
