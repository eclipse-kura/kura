package org.eclipse.kura.ai.triton.server;

public class TritonServerRemoteManager implements TritonServerInstanceManager {

    @Override
    public void start() {
        // Not supported for remote instance
    }

    @Override
    public void stop() {
        // Not supported for remote instance
    }

    @Override
    public void kill() {
        // Not supported for remote instance
    }

    @Override
    public boolean isServerRunning() {
        return true;
    }

    @Override
    public boolean isLifecycleManaged() {
        return false;
    }

}
