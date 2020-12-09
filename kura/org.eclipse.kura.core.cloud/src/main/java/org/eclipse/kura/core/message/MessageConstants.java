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
 *******************************************************************************/
package org.eclipse.kura.core.message;

import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;

/**
 * Internal enum providing constants for property sharing between {@link CloudPublisher}s or {@link CloudSubscriber}s
 * and {@link CloudConnectionManager} implementations.
 *
 * These constants are used as keys to identify the different properties shared in a {@link KuraMessage} to provide
 * context associated to a corresponding {@link KuraPayload}
 *
 */
public enum MessageConstants {

    FULL_TOPIC,
    APP_ID,
    APP_TOPIC,
    QOS,
    RETAIN,
    PRIORITY,
    CONTROL;

}
