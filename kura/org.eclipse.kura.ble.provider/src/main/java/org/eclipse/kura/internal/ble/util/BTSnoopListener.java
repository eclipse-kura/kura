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
package org.eclipse.kura.internal.ble.util;

/**
 * For listening to btsnoop streams
 */
public interface BTSnoopListener {

    /**
     * Process a BTSnoop Record
     *
     * @param record
     */
    public void processBTSnoopRecord(byte[] record);

    /**
     * Process a BTSnoop error stream
     *
     * @param string
     */
    public void processBTSnoopErrorStream(String string);

}
