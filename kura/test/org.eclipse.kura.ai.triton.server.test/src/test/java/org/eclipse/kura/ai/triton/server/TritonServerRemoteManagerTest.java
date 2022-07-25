package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TritonServerRemoteManagerTest {

    private TritonServerRemoteManager manager;
    private boolean isRunning;
    private boolean isManaged;

    @Test
    public void isServerRunningWorks() {
        givenInstanceManager();

        whenIsServerRunningIsCalled();

        thenServerIsRunning(true);
    }

    @Test
    public void isLifecycleManagedWorks() {
        givenInstanceManager();

        whenIsLifecycleManagedIsCalled();

        thenServerIsManaged(false);
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
    private void whenIsServerRunningIsCalled() {
        this.isRunning = this.manager.isServerRunning();
    }

    private void whenIsLifecycleManagedIsCalled() {
        this.isManaged = this.manager.isLifecycleManaged();
    }

    /*
     * Then
     */
    private void thenServerIsRunning(boolean expectedState) {
        assertEquals(expectedState, this.isRunning);
    }

    private void thenServerIsManaged(boolean expectedState) {
        assertEquals(expectedState, this.isManaged);
    }

}
