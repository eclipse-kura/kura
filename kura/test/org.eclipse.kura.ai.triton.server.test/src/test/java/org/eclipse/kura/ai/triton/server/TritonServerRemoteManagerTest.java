package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TritonServerRemoteManagerTest {

    private TritonServerRemoteManager manager;

    @Test
    public void isServerRunningWorks() {
        givenInstanceManager();

        thenServerIsRunningReturns(false);
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

        thenServerIsRunningReturns(false);
    }

    @Test
    public void killMethodWorks() {
        givenInstanceManager();

        whenKillMethodIsCalled();

        thenServerIsRunningReturns(false);
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
}
