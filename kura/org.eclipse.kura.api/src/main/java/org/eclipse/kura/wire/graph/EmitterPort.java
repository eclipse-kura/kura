package org.eclipse.kura.wire.graph;

import org.eclipse.kura.wire.WireEnvelope;

/**
 * @since 1.4
 */
public interface EmitterPort extends Port {

    public void emit(WireEnvelope wireEnvelope);

}
