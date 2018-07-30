package org.eclipse.kura.net.modem;

import org.eclipse.kura.KuraException;

/**
 * @since 2.0
 */
public interface ModemDriver {
    
    public void turnModemOff() throws KuraException;
    
    public void turnModemOn() throws KuraException;

}
