/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.graph;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides all the needed methods to interact with the
 * WireGraph.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.4
 */
@ProviderType
public interface WireGraphService {

    /**
     * This method allows to create and update the graph, by providing a
     * {@link WireGraphConfiguration}.
     *
     * @param graphConfiguration
     *            A {@link WireGraphConfiguration} object that represents an updated
     *            status of the Wire Graph
     * @throws {@link
     *             KuraException} if the update operation fails
     */
    public void update(WireGraphConfiguration graphConfiguration) throws KuraException;

    /**
     * This method allows to delete the current Wire Graph.
     *
     * @throws {@link
     *             KuraException} if the delete operation fails
     */
    public void delete() throws KuraException;

    /**
     * This method returns the current Wire Graph configuration.
     *
     * @return a {@link WireGraphConfiguration} object that represents the current
     *         configuration of the Wire Graph
     * @throws {@link
     *             KuraException} if the get operation fails
     */
    public WireGraphConfiguration get() throws KuraException;

}
