package org.eclipse.kura.ai.triton.server;

public interface TritonServerInstanceManager {

    /**
     * Start the managed Triton Server instance
     */
    public void start();

    /**
     * Stop the managed Triton Server instance
     */
    public void stop();

    /**
     * Stop forcefully the managed Triton Server instance
     */
    public void kill();

    /**
     * Check the managed Triton Server instance status
     *
     * @return whether the server instance is running
     */
    public boolean isServerRunning();

    /**
     * Check whether the manager can handle the Triton Server instance lifecycle
     * should be always true for local instances (Native, Container)
     *
     * @return whether the server instance lifecycle is managed
     */
    public boolean isLifecycleManaged();

}
