package org.eclipse.kura.wire.graph;

import org.eclipse.kura.wire.WireConfiguration;

/**
 * @since 1.4
 */
public class MultiportWireConfiguration extends WireConfiguration {

    private int emitterPort;
    private int receiverPort;

    public MultiportWireConfiguration(String emitterPid, String receiverPid, int emitterPort, int receiverPort) {
        super(emitterPid, receiverPid);
        this.emitterPort = emitterPort;
        this.receiverPort = receiverPort;
    }

    public int getEmitterPort() {
        return emitterPort;
    }

    public void setEmitterPort(int emitterPort) {
        this.emitterPort = emitterPort;
    }

    public int getReceiverPort() {
        return receiverPort;
    }

    public void setReceiverPort(int receiverPort) {
        this.receiverPort = receiverPort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + emitterPort;
        result = prime * result + receiverPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MultiportWireConfiguration other = (MultiportWireConfiguration) obj;
        if (emitterPort != other.emitterPort)
            return false;
        if (receiverPort != other.receiverPort)
            return false;
        return true;
    }

}
