/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.wifi;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.board.BoardResourcePowerService;
import org.eclipse.kura.net.wifi.WifiMode;

/**
 * 
 *
 */
public interface WifiDriverService extends BoardResourcePowerService{

    public boolean isKernelModuleLoaded(String interfaceName) throws KuraException;
    
    public boolean isKernelModuleLoadedForMode(String interfaceName, WifiMode wifiMode) throws KuraException;
    
    public void unloadKernelModule(String interfaceName) throws KuraException;
    
    public void loadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException;
    
}
