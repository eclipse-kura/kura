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

import org.eclipse.kura.cloudconnection.CloudEndpoint;

public interface CloudEndpointTrackerListener {

    public void onCloudEndpointAdded(CloudEndpoint cloudEndpoint);

    public void onCloudEndpointRemoved(CloudEndpoint cloudEndpoint);
}
