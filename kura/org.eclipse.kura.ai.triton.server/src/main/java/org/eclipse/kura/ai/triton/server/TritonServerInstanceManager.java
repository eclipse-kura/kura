package org.eclipse.kura.ai.triton.server;

public interface TritonServerInstanceManager {

    public void start();

    public void stop();

    public void kill();

    public boolean isServerRunning();

    public boolean isLifecycleManaged();

}
