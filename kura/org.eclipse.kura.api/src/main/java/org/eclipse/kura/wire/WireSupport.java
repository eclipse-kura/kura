/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire;

import java.util.List;

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;

/**
 * The interface WireSupport is responsible for managing incoming as well as
 * outgoing wires of the contained Wire Component. This is also used to perform
 * wire related operations for instance, emit and receive wire records.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface WireSupport extends Producer, Consumer {

    /**
     * The topic to be used for publishing and receiving the emit trigger events
     */
    public static final String EMIT_EVENT_TOPIC = "org/eclipse/kura/wires/emit";

    /**
     * Emit the provided wire records
     *
     * @param wireRecords
     *            the Wire Records to emit
     */
    public void emit(List<WireRecord> wireRecords);

    /**
     * Filters the provided list of wire records based on the configured
     * {@link SeverityLevel} for the provided wire component. It filters out all
     * the Wire Fields that do not belong to the level as required by the Wire
     * Component.
     *
     * @param records
     *            the list of Wire Records to filter
     * @return the list of filtered Wire Records
     */
    public List<WireRecord> filter(List<WireRecord> records);
}
