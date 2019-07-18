package org.eclipse.kura.security;

import org.eclipse.kura.KuraException;

/**
 * @since 2.2
 */
public interface LoginProtectionService {
    
    public void enable() throws KuraException;
    
    public void disable() throws KuraException;
    
    public boolean isEnabled() throws KuraException;

}
