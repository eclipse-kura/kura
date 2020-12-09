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

import org.eclipse.kura.KuraException;

public interface BluetoothProcessListener {

    public void processInputStream(String string) throws KuraException;

    public void processInputStream(int ch) throws KuraException;

    public void processErrorStream(String string) throws KuraException;

}
