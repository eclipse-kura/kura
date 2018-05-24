/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.request;

import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.osgi.annotation.versioning.ProviderType;

/**
 * PoJo class used to wrap the context associated to a request received from the cloud and passed to a
 * {@link RequestHandler}.
 * It should be used, for example, to provide the context that will be leveraged by a {@link RequestHandler} to publish event
 * notifications to a remote cloud platform.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public class RequestHandlerContext {

    private final String notificationPublisherPid;
    private final CloudNotificationPublisher notificationPublisher;

    public RequestHandlerContext(String notificationPublisherPid, CloudNotificationPublisher notificationPublisher) {
        this.notificationPublisherPid = notificationPublisherPid;
        this.notificationPublisher = notificationPublisher;
    }

    public String getNotificationPublisherPid() {
        return this.notificationPublisherPid;
    }
    
    public CloudNotificationPublisher getNotificationPublisher() {
        return this.notificationPublisher;
    }

}
