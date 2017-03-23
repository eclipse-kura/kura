/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import java.util.List;

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;

/**
 * The interface {@link WireSupport} is responsible for managing incoming as well as
 * outgoing wires of the contained {@link WireComponent}. This is also used to perform
 * wire related operations for instance, emit, filter and receive {@link WireRecord}s.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface WireSupport extends Producer, Consumer {

    /**
     * The topic to be used for publishing and receiving the emit trigger events
     */
    public static final String EMIT_EVENT_TOPIC = "org/eclipse/kura/wires/emit";

    /**
     * Emit the provided {@link WireRecord}s
     *
     * @param wireRecords
     *            a list of {@link WireRecord} objects that will be emitted to
     *            the connected {@link WireReceiver} instance
     * @throws NullPointerException
     *             if the argument is null
     */
    public void emit(List<WireRecord> wireRecords);

    /**
     * Filters out the keys from the associated properties of provided {@link WireRecord}s
     * that matches the provided filter
     *
     * @param wireRecords
     *            the list of {@link WireRecord}s
     * @param filter
     *            the filter to match
     * @return the list of {@link WireRecord}s containing the filtered peroperties
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public List<WireRecord> filter(List<WireRecord> wireRecords, String filter);
}
