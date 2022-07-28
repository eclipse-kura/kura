/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/

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