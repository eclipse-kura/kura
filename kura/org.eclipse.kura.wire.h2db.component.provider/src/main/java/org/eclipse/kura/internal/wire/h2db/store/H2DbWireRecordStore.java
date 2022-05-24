/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.store;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.basedb.store.BaseDbWireRecordStore;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received {@link WireRecord}.
 */
public class H2DbWireRecordStore extends BaseDbWireRecordStore
        implements WireEmitter, WireReceiver, ConfigurableComponent {
}
