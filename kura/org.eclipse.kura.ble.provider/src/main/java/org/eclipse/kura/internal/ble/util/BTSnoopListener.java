/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
