/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * CloudConnectionEstablishedEvent is raised with the Cloud Connection is lost.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class CloudConnectionLostEvent extends Event {

    /** Topic of the CloudConnectionLostEvent */
    public static final String CLOUD_CONNECTION_STATUS_LOST = "org/eclipse/kura/cloud/CloudConnectionStatus/LOST";

    public CloudConnectionLostEvent(Map<String, ?> properties) {
        super(CLOUD_CONNECTION_STATUS_LOST, properties);
    }
}
