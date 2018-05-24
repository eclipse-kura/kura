/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloudconnection;

import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;

/**
 * Provides constants that can be used by cloud connections.
 *
 * @since 2.0
 */
public enum CloudConnectionConstants {

    /**
     * The key of the property that specifies the {@code kura.service.pid} of the associated
     * {@link CloudConnectionManager} in {@link CloudPublisher} or {@link CloudSubscriber} component configuration.
     */
    CLOUD_CONNECTION_SERVICE_PID_PROP_NAME("cloud.connection.service.pid"),

    /**
     * The key of the property that specifies the {@code kura.service.pid} of the associated
     * {@link CloudConnectionFactory} in {@link CloudConnectionManager} component definition.
     */
    CLOUD_CONNECTION_FACTORY_PID_PROP_NAME("cloud.connection.factory.pid");

    private String value;

    private CloudConnectionConstants(final String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
