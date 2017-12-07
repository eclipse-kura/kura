package org.eclipse.kura.wire.graph;

import java.util.List;

import org.osgi.service.wireadmin.Wire;

/**
 * @since 1.4
 */
public interface Port {

    public List<Wire> listConnectedWires();

}
