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
 * CloudConnectionEstablishedEvent is raised with the Cloud Connection is established.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class CloudConnectionEstablishedEvent extends Event {

    /** Topic of the CloudConnectionEstablishedEvent */
    public static final String CLOUD_CONNECTION_STATUS_ESTABLISHED = "org/eclipse/kura/cloud/CloudConnectionStatus/ESTABLISHED";

    public CloudConnectionEstablishedEvent(Map<String, ?> properties) {
        super(CLOUD_CONNECTION_STATUS_ESTABLISHED, properties);
    }
}
