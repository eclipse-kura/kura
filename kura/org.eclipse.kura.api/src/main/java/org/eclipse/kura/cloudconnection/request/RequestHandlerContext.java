/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.cloudconnection.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.osgi.annotation.versioning.ProviderType;

/**
 * PoJo class used to wrap the context associated to a request received from the cloud and passed to a
 * {@link RequestHandler}.
 * It should be used, for example, to provide the context that will be leveraged by a {@link RequestHandler} to publish
 * event notifications to a remote cloud platform.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public class RequestHandlerContext {

    private final CloudNotificationPublisher notificationPublisher;
    private final Map<String, String> contextProperties;

    public RequestHandlerContext(CloudNotificationPublisher notificationPublisher,
            Map<String, String> contextProperties) {
        this.notificationPublisher = notificationPublisher;
        this.contextProperties = new HashMap<>(contextProperties);
    }

    public CloudNotificationPublisher getNotificationPublisher() {
        return this.notificationPublisher;
    }

    public Map<String, String> getContextProperties() {
        return Collections.unmodifiableMap(this.contextProperties);
    }

}
