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
package org.eclipse.kura.wire.script.tools.conditional.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ConditionalComponentOptionsTest {

    private Map<String, Object> properties = new HashMap<>();
    private ConditionalComponentOptions options;
    private Optional<String> returnedBooleanExpression = Optional.empty();

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnEmptyScript() {
        givenFilterComponentOptions();

        whenGetScriptSource();

        thenReturnedScriptSourceIsEmpty();
    }

    @Test
    public void shouldReturnCorrectScript() {
        givenScriptProperty("// example JS script\n1 == 1;");
        givenFilterComponentOptions();

        whenGetScriptSource();

        thenReturnedScriptSourceIs("// example JS script\n1 == 1;");
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

    private void givenScriptProperty(String booleanExpression) {
        this.properties.put(ConditionalComponentOptions.CONDITION_PROPERTY_KEY, booleanExpression);
    }

    private void givenFilterComponentOptions() {
        this.options = new ConditionalComponentOptions(this.properties);
    }

    /*
     * When
     */

    private void whenGetScriptSource() {
        this.returnedBooleanExpression = this.options.getBooleanExpression();
    }

    /*
     * Then
     */

    private void thenReturnedScriptSourceIs(String expectedResult) {
        assertEquals(expectedResult, this.returnedBooleanExpression.get());
    }

    private void thenReturnedScriptSourceIsEmpty() {
        assertFalse(this.returnedBooleanExpression.isPresent());
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.properties.clear();
    }

}
