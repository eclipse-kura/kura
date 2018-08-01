package org.eclipse.kura.net.modem;

import org.eclipse.kura.KuraException;

/**
 * @since 2.0
 */
public interface GatewayModemDriver {
    
    public void turnModemOff(String vendor, String product) throws KuraException;
    
    public void turnModemOn(String vendor, String product) throws KuraException;

    public void resetModem(String vendor, String product) throws KuraException;
}
