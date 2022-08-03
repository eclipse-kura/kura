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

import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;

public interface CloudStackTrackerListener {

    public void onCloudEndpointAdded(CloudEndpoint cloudEndpoint, String dataServicePid);

    public void onCloudEndpointRemoved(CloudEndpoint cloudEndpoint);

    public void onDataServiceAdded(DataService dataService, String dataTransportServicePid);

    public void onDataServiceRemoved(DataService dataService);

    public void onDataTransportServiceAdded(DataTransportService dataTransportService);

    public void onDataTransportServiceRemoved(DataTransportService dataTransportService);
}
