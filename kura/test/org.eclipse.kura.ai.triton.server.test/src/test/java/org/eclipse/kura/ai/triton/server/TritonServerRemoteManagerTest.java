package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TritonServerRemoteManagerTest {

    private TritonServerRemoteManager manager;

    @Test
    public void isServerRunningWorks() {
        givenInstanceManager();

        thenServerIsRunningReturns(true);
    }

    @Test
    public void isLifecycleManagedWorks() {
        givenInstanceManager();

        thenServerIsManagedReturns(false);
    }

    @Test
    public void startMethodWorks() {
        givenInstanceManager();

        whenStartMethodIsCalled();

        thenServerIsRunningReturns(true);
    }

    @Test
    public void stopMethodWorks() {
        givenInstanceManager();

        whenStopMethodIsCalled();

        thenServerIsRunningReturns(true);
    }

    @Test
    public void killMethodWorks() {
        givenInstanceManager();

        whenKillMethodIsCalled();

        thenServerIsRunningReturns(true);
    }

    /*
     * Given
     */
    private void givenInstanceManager() {
        this.manager = new TritonServerRemoteManager();
    }

    /*
     * When
     */
    private void whenStartMethodIsCalled() {
        this.manager.start();
    }

    private void whenStopMethodIsCalled() {
        this.manager.stop();
    }

    private void whenKillMethodIsCalled() {
        this.manager.kill();
    }

    /*
     * Then
     */
    private void thenServerIsRunningReturns(boolean expectedState) {
        assertEquals(expectedState, this.manager.isServerRunning());
    }

    private void thenServerIsManagedReturns(boolean expectedState) {
        assertEquals(expectedState, this.manager.isLifecycleManaged());
    }

}
