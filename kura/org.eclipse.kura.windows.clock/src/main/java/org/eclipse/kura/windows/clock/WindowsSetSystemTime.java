/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.windows.clock;

import org.eclipse.kura.windows.system.KuraNativeWin;

public class WindowsSetSystemTime {

    public boolean SetLocalTime(int wYear, int wMonth, int wDay, int wHour, int wMinute, int wSecond) {
        KuraNativeWin nativeWin = new KuraNativeWin();
        nativeWin.setSystemTime((short) wYear, (short) wMonth, (short) wDay, (short) wHour, (short) wMinute,
                (short) wSecond, (short) 0);
        return true;
    }
}
