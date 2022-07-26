package org.eclipse.kura.ai.triton.server;

public class TritonServerRemoteManager implements TritonServerInstanceManager {

    private boolean isRunning = false;

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }

    @Override
    public void kill() {
        this.isRunning = false;
    }

    @Override
    public boolean isServerRunning() {
        return this.isRunning;
    }
}
