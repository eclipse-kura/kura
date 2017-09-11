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

import org.eclipse.kura.KuraException;

public interface BluetoothProcessListener {

    public void processInputStream(String string) throws KuraException;

    public void processInputStream(int ch) throws KuraException;

    public void processErrorStream(String string) throws KuraException;

}
