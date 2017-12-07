package org.eclipse.kura.wire.graph;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.wire.WireComponentDefinition;

/**
 * @since 1.4
 */
public interface WireComponentDefinitionService {
    
    /**
     * @since 1.4
     */
    public List<WireComponentDefinition> getComponentDefinitions() throws KuraException;

}
