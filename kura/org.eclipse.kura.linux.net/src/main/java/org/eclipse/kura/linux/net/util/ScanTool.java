/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.linux.net.util;

import java.util.Collection;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.wifi.WifiOptions;

public abstract class ScanTool {

    public static IScanTool get(String ifaceName, CommandExecutorService executorService) throws KuraException {
        Collection<String> supportedWifiOptions = new WifiOptions(executorService).getSupportedOptions(ifaceName);
        IScanTool scanTool = null;
        if (!supportedWifiOptions.isEmpty()) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                scanTool = new IwScanTool(ifaceName, executorService);
            } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
                scanTool = new IwlistScanTool(ifaceName, executorService);
            }
        }
        return scanTool;
    }
}
