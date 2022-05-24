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

import java.util.Map;

import org.eclipse.kura.wire.basedb.store.BaseDbWireRecordStoreOptions;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class H2DbWireRecordStoreOptions extends BaseDbWireRecordStoreOptions {

    protected H2DbWireRecordStoreOptions(final Map<String, Object> properties) {
        super(properties);
    }
}