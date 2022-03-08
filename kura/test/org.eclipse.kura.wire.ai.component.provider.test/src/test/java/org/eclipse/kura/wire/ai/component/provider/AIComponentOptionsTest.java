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

package org.eclipse.kura.wire.ai.component.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class AIComponentOptionsTest {

    private AIComponentOptions optionsInstance;

    private static final String PREPROCESSOR_MODEL_NAME = "preprocessor.model.name";
    private static final String INFERENCE_MODEL_NAME = "inference.model.name";
    private static final String POSTPROCESSOR_MODEL_NAME = "postprocessor.model.name";

    private Optional<String> preprocessorName;
    private String inferenceName;
    private Optional<String> postprocessorName;

    /*
     * Scenarios
     */
    @Test
    public void getPropertiesWorksWhenAllFieldsAreSpecified() {
        givenOptionsPopulatedWithStrings("preprocessor_name", "inference_name", "postprocessor_name");

        whenAccessingOptionsFields();

        thenPreprocessorModelNameIs("preprocessor_name");
        thenInferenceModelNameIs("inference_name");
        thenPostprocessorModelNameIs("postprocessor_name");
    }

    @Test
    public void getPropertiesWorksWhenOnlyMandatoryFieldsAreSpecified() {
        givenOptionsPopulatedWithStrings("", "inference_name", "");

        whenAccessingOptionsFields();

        thenPreprocessorModelNameIsEmpty();
        thenInferenceModelNameIs("inference_name");
        thenPostprocessorModelNameIsEmpty();
    }

    @Test
    public void getPropertiesWorksWhenFieldsAreSpecifiedWithAdditionalSpaces() {
        givenOptionsPopulatedWithStrings("    xyx     ", "    name", "name     ");

        whenAccessingOptionsFields();

        thenPreprocessorModelNameIs("xyx");
        thenInferenceModelNameIs("name");
        thenPostprocessorModelNameIs("name");
    }

    @Test
    public void getPropertiesWorksWhenAllFieldsAreEmpty() {
        givenOptionsNotPopulated();

        whenAccessingOptionsFields();

        thenPreprocessorModelNameIsEmpty();
        thenInferenceModelNameIsEmpty();
        thenPostprocessorModelNameIsEmpty();
    }

    /*
     * Given
     */
    private void givenOptionsPopulatedWithStrings(String prepName, String infName, String postName) {
        Map<String, Object> prop = new HashMap<>();

        prop.put(PREPROCESSOR_MODEL_NAME, prepName);
        prop.put(INFERENCE_MODEL_NAME, infName);
        prop.put(POSTPROCESSOR_MODEL_NAME, postName);

        optionsInstance = new AIComponentOptions(prop);
    }

    private void givenOptionsNotPopulated() {
        Map<String, Object> prop = new HashMap<>();
        optionsInstance = new AIComponentOptions(prop);
    }

    /*
     * When
     */
    private void whenAccessingOptionsFields() {
        preprocessorName = optionsInstance.getPreprocessorModelName();
        inferenceName = optionsInstance.getInferenceModelName();
        postprocessorName = optionsInstance.getPostprocessorModelName();
    }

    /*
     * Then
     */
    private void thenPreprocessorModelNameIs(String expectedName) {
        assertTrue(preprocessorName.isPresent());
        assertEquals(expectedName, preprocessorName.get());
    }

    private void thenInferenceModelNameIs(String expectedName) {
        assertEquals(expectedName, inferenceName);
    }

    private void thenPostprocessorModelNameIs(String expectedName) {
        assertTrue(postprocessorName.isPresent());
        assertEquals(expectedName, postprocessorName.get());
    }

    private void thenPreprocessorModelNameIsEmpty() {
        assertFalse(preprocessorName.isPresent());
        assertEquals(preprocessorName, Optional.empty());
    }

    private void thenInferenceModelNameIsEmpty() {
        assertEquals(null, inferenceName);
    }

    private void thenPostprocessorModelNameIsEmpty() {
        assertFalse(postprocessorName.isPresent());
        assertEquals(postprocessorName, Optional.empty());
    }
}
