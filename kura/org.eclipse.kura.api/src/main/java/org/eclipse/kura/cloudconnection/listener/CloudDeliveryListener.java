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
package org.eclipse.kura.cloudconnection.listener;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Implementors of this interface will be able to handle cloud stack related
 * events that deal with message delivery.
 *
 * All the registered listeners are called synchronously at the occurrence of the event.
 * It is expected that implementors of this interface do NOT perform long running tasks in the implementation of this
 * interface.
 *
 * @since 2.0
 */
@ConsumerType
public interface CloudDeliveryListener {

    /**
     * Confirms message delivery to the cloud platform.
     *
     * @param messageId
     */
    public void onMessageConfirmed(String messageId);

}
