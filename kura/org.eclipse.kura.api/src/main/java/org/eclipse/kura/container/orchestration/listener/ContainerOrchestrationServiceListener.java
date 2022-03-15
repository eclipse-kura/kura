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
package org.eclipse.kura.container.orchestration.listener;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Listener interface to be implemented by applications that needs to be notified of events in the
 * {@link org.eclipse.kura.container.orchestration.ContainerOrchestrationService}.
 * All registered listeners are called synchronously by the
 * {@link org.eclipse.kura.container.orchestration.ContainerOrchestrationService} at the
 * occurrence of the event.
 * It expected that implementers of this interface do NOT perform long running tasks in the implementation of this
 * interface.
 *
 * @since 2.3
 */
@ConsumerType
public interface ContainerOrchestrationServiceListener {

    /**
     * Notifies the listener that the connection to the orchestrator service is established.
     */
    public void onConnect();

    /**
     * Notifies the listener that the connection to the orchestrator service has been lost.
     */
    public void onDisconnect();

    /**
     * Notifies the listener that the connection to the orchestrator service has been disabled
     */
    public void onDisabled();

}
