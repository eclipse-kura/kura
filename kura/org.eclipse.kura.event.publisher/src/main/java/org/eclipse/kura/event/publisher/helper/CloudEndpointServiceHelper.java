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
package org.eclipse.kura.event.publisher.helper;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudEndpointServiceHelper implements CloudEndpointTrackerListener {

    private static final Logger logger = LoggerFactory.getLogger(CloudEndpointServiceHelper.class);

    private CloudEndpointServiceTracker cloudEndpointTracker;
    private BundleContext context;
    private CloudEndpoint cloudEndpoint;

    public CloudEndpointServiceHelper(BundleContext context, String endpointPid) {
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
    }

    public String publish(KuraMessage message) throws KuraException {
        if (this.cloudEndpoint == null) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "CloudEndpoint not available");
        }
        
        return this.cloudEndpoint.publish(message);
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

    @Override
    public void onCloudEndpointAdded(CloudEndpoint cloudEndpoint) {
        this.cloudEndpoint = cloudEndpoint;
    }

    @Override
    public void onCloudEndpointRemoved(CloudEndpoint cloudEndpoint) {
        this.cloudEndpoint = null;
    }

}
