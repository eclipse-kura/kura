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
package org.eclipse.kura.wire.script.tools.filter.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class FilterComponentOptionsTest {

    private Map<String, Object> properties = new HashMap<>();
    private FilterComponentOptions options;
    private Optional<String> returnedScriptSource = Optional.empty();
    private boolean returnedIsScriptContextDrop;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnDefaultScriptContextDrop() {
        givenFilterComponentOptions();

        whenIsScriptContextDrop();

        thenReturnedScriptContextDropIs(FilterComponentOptions.SCRIPT_CONTEXT_DROP_DEFAULT_VALUE);
    }

    @Test
    public void shouldReturnTrueScriptContextDrop() {
        givenScriptContextDropProperty(true);
        givenFilterComponentOptions();
        
        whenIsScriptContextDrop();
        
        thenReturnedScriptContextDropIs(true);
    }

    @Test
    public void shouldReturnFalseScriptContextDrop() {
        givenScriptContextDropProperty(false);
        givenFilterComponentOptions();

        whenIsScriptContextDrop();

        thenReturnedScriptContextDropIs(false);
    }

    @Test
    public void shouldReturnEmptyScript() {
        givenFilterComponentOptions();

        whenGetScriptSource();

        thenReturnedScriptSourceIsEmpty();
    }

    @Test
    public void shouldReturnCorrectScript() {
        givenScriptProperty("// example JS script\nvar x = 1;");
        givenFilterComponentOptions();

        whenGetScriptSource();

        thenReturnedScriptSourceIs("// example JS script\nvar x = 1;");
    }

    @Test
    public void shouldReturnTrimmedScript() {
        givenScriptProperty("   // example JS script\nvar x = 1;  ");
        givenFilterComponentOptions();

        whenGetScriptSource();

        thenReturnedScriptSourceIs("// example JS script\nvar x = 1;");
    }

    @Test
    public void shouldReturnEmptyTrimmedScript() {
        givenScriptProperty("       ");
        givenFilterComponentOptions();

        whenGetScriptSource();

        thenReturnedScriptSourceIsEmpty();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenScriptProperty(String scriptCode) {
        this.properties.put(FilterComponentOptions.SCRIPT_KEY, scriptCode);
    }

    private void givenScriptContextDropProperty(boolean contextDrop) {
        this.properties.put(FilterComponentOptions.SCRIPT_CONTEXT_DROP_KEY, contextDrop);
    }

    private void givenFilterComponentOptions() {
        this.options = new FilterComponentOptions(this.properties);
    }

    /*
     * When
     */

    private void whenGetScriptSource() {
        this.returnedScriptSource = this.options.getScriptSource();
    }

    private void whenIsScriptContextDrop() {
        this.returnedIsScriptContextDrop = this.options.isScriptContextDrop();
    }

    /*
     * Then
     */

    private void thenReturnedScriptSourceIs(String expectedResult) {
        assertEquals(expectedResult, this.returnedScriptSource.get());
    }

    private void thenReturnedScriptSourceIsEmpty() {
        assertFalse(this.returnedScriptSource.isPresent());
    }

    private void thenReturnedScriptContextDropIs(boolean expectedResult) {
        assertEquals(expectedResult, this.returnedIsScriptContextDrop);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.properties.clear();
    }

}
