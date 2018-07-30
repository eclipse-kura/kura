package org.eclipse.kura.net.wifi;

import org.eclipse.kura.KuraException;

/**
 * @since 2.0
 */
public interface WifiUtils {

    public boolean isKernelModuleLoaded(String interfaceName) throws KuraException;
    
    public boolean isKernelModuleLoadedForMode(String interfaceName, WifiMode wifiMode) throws KuraException;
    
    public void unloadKernelModule(String interfaceName) throws KuraException;
    
    public void loadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException;
    
    public boolean isWifiDeviceOn(String interfaceName);
    
    public void turnWifiDeviceOn(String interfaceName) throws KuraException;
    
    public void turnWifiDeviceOff(String interfaceName) throws KuraException;
    
}
