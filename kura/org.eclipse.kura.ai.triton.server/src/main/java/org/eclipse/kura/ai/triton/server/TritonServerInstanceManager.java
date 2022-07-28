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
 ******************************************************************************/
package org.eclipse.kura.ai.triton.server;

public interface TritonServerInstanceManager {

    /**
     * Start the managed Triton Server instance
     */
    public void start();

    /**
     * Stop the managed Triton Server instance
     */
    public void stop();

    /**
     * Stop forcefully the managed Triton Server instance
     */
    public void kill();

    /**
     * Check the managed Triton Server instance status
     *
     * @return whether the server instance is running
     */
    public boolean isServerRunning();
}