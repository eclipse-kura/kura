/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.linux.net.wifi;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.board.BoardResourcePowerService;
import org.eclipse.kura.net.wifi.WifiMode;

/**
 *
 *
 */
public interface WifiDriverService extends BoardResourcePowerService {

    public boolean isKernelModuleLoaded(String interfaceName) throws KuraException;

    public boolean isKernelModuleLoadedForMode(String interfaceName, WifiMode wifiMode) throws KuraException;

    public void unloadKernelModule(String interfaceName) throws KuraException;

    public void loadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException;

}
