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

    APP_ID,
    APP_TOPIC,
    QOS,
    RETAIN,
    PRIORITY,
    CONTROL;

}
