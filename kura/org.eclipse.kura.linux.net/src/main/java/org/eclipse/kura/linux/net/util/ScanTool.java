/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
