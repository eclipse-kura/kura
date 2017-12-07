package org.eclipse.kura.wire.graph;

import java.util.List;

import org.eclipse.kura.wire.WireSupport;

/**
 * @since 1.4
 */
public interface MultiportWireSupport extends WireSupport {

    public List<EmitterPort> getEmitterPorts();

    public List<ReceiverPort> getReceiverPorts();

}
