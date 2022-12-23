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
package org.eclipse.kura.wire.script.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class EngineProviderTest {

    private EngineProvider engine = new EngineProvider() {

    };
    private boolean isEngineInit = false;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnEngineInited() {
        givenEngineInit();

        whenIsEngineInit();

        thenEngineIsInit();
    }

    @Test
    public void shouldNotReturnEngineInited() {
        whenIsEngineInit();

        thenEngineIsNotInit();
    }

    @Test
    public void shouldNotReturnEngineInitedAfterEngineClosed() {
        givenEngineInit();
        givenCloseEngine();

        whenIsEngineInit();

        thenEngineIsNotInit();
    }

    @Test
    public void shouldNotExecuteQuit() {
        givenEngineInit();

        whenEvaluate("quit");

        thenResultIsEmpty();
    }

    @Test
    public void shouldNotExecuteExit() {
        givenEngineInit();

        whenEvaluate("exit");

        thenResultIsEmpty();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenEngineInit() {
        this.engine.initEngine();
    }

    private void givenCloseEngine() {
        this.engine.closeEngine();
    }

    /*
     * When
     */

    private void whenIsEngineInit() {
        this.isEngineInit = this.engine.isEngineInit();
    }

    private void whenEvaluate(String sourceCode) {
        this.engine.evaluate(sourceCode);
    }

    /*
     * Then
     */

    private void thenEngineIsInit() {
        assertTrue(this.isEngineInit);
    }

    private void thenEngineIsNotInit() {
        assertFalse(this.isEngineInit);
    }

    private void thenResultIsEmpty() {
        assertFalse(this.engine.getResultAsBoolean().isPresent());
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.isEngineInit = false;
    }

}
