/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.wire;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.Wire;

/**
 * @since 1.4
 */
@ProviderType
public interface WireComponentPort {

    /**
     * Emit the provided {@link WireRecord}s
     *
     * @param wireRecords
     *            a List of {@link WireRecord} objects that will be sent to the receiver.
     * @throws NullPointerException
     *             if the argument is null
     */
    public void emit(List<WireRecord> wireRecords) throws KuraException;

    /**
     * Adds a new {@link Wire} to the list of wires connected to the port
     * 
     * @param wire
     *            a {@link Wire} instance that needs to be managed
     * @throws KuraException
     *             if the adding operation fails
     */
    public void addWire(Wire wire) throws KuraException;

    /**
     * Removes the provided {@link Wire} from the wires associated to the port.
     * 
     * @param wire
     *            the {@link Wire} instance that is not connected anymore to the port
     * @throws KuraException
     *             if the removal operation fails
     */
    public void removeWire(Wire wire) throws KuraException;

}
