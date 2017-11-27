package org.eclipse.kura.marshalling;

import org.eclipse.kura.KuraException;

/**
 * @since 1.4
 */
public interface Marshalling {
    
    public String marshal(Object object) throws KuraException;
    
    public <T> T unmarshal(String s, Class<T> clazz) throws Exception;

}
