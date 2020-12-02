/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 ******************************************************************************/
package org.eclipse.kura.wire;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;

/**
 * The interface WireSupport is responsible for managing incoming as well as
 * outgoing wires of the contained Wire Component. This is also used to perform
 * wire related operations for instance, emit and receive {@link WireRecord}s.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface WireSupport extends Producer, Consumer {

    /**
     * Emit the provided {@link WireRecord}s
     *
     * @param wireRecords
     *            a List of {@link WireRecord} objects that will be sent to the receiver.
     * @throws NullPointerException
     *             if the argument is null
     */
    public void emit(List<WireRecord> wireRecords);
}
