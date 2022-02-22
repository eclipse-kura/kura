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
 *******************************************************************************/

package org.eclipse.kura.wire.ai.component.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.ai.inference.ModelInfo;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIComponentTest {

    private static final Logger logger = LoggerFactory.getLogger(AIComponentTest.class);

    private static final String PREPROCESSOR_MODEL_NAME = "preprocessor.model.name";
    private static final String INFERENCE_MODEL_NAME = "inference.model.name";
    private static final String POSTPROCESSOR_MODEL_NAME = "postprocessor.model.name";

    private AIComponent aiComponent;
    private Map<String, Object> compProperties;
    private InferenceEngineServiceMock engine;
    private WireEnvelope inputEnvelope;
    private boolean exceptionsOccurred;

    /*
     * Scenarios
     */

    @Test
    public void shouldInferOnce() {
        givenInferenceEngine();
        givenInputEnvelope();
        givenWireProperties("", "infer", "");
        givenModelInfo("infer");
        givenActivate();

        whenOnWireReceive(this.inputEnvelope);

        thenInferIsCalled(1);
        thenNoExceptionsOccurred();
    }

    @Test
    public void shouldInferTwice() {
        givenInferenceEngine();
        givenInputEnvelope();
        givenWireProperties("preprocessor", "infer", "");
        givenModelInfo("preprocessor");
        givenModelInfo("infer");
        givenActivate();

        whenOnWireReceive(this.inputEnvelope);

        thenInferIsCalled(2);
        thenNoExceptionsOccurred();
    }

    @Test
    public void shouldInfer3Times() {
        givenInferenceEngine();
        givenInputEnvelope();
        givenWireProperties("preprocessor", "infer", "postprocessor");
        givenModelInfo("preprocessor");
        givenModelInfo("infer");
        givenModelInfo("postprocessor");
        givenActivate();

        whenOnWireReceive(this.inputEnvelope);

        thenInferIsCalled(3);
        thenNoExceptionsOccurred();
    }

    @Test
    public void shouldNotInferBecauseEngineIsNotReady() {
        givenNotReadyInferenceEngine();
        givenInputEnvelope();
        givenWireProperties("preprocessor", "infer", "postprocessor");
        givenModelInfo("preprocessor");
        givenModelInfo("infer");
        givenModelInfo("postprocessor");
        givenActivate();

        whenOnWireReceive(this.inputEnvelope);

        thenInferIsCalled(0);
        thenNoExceptionsOccurred();
    }

    @Test
    public void missingModelsShouldNotActivate() {
        givenInferenceEngine();
        givenInputEnvelope();
        givenWireProperties("preprocessor", "infer", "postprocessor");
        givenActivate();

        whenOnWireReceive(this.inputEnvelope);

        thenInferIsCalled(0);
        thenExceptionsOccurred();
    }

    @Test
    public void missingInferenceEngineShouldNotActivate() {
        givenInferenceEngineNotBound();
        givenInputEnvelope();
        givenWireProperties("preprocessor", "infer", "postprocessor");
        givenActivate();

        whenOnWireReceive(this.inputEnvelope);

        thenNoExceptionsOccurred();
    }

    /*
     * Steps
     */

    /*
     * Given steps
     */

    private void givenInferenceEngine() {
        this.engine = new InferenceEngineServiceMock();
        this.engine.setEngineReady();
        this.aiComponent.bindInferenceEngineService(this.engine);
    }

    private void givenNotReadyInferenceEngine() {
        this.engine = new InferenceEngineServiceMock();
        this.engine.setEngineNotReady();
        this.aiComponent.bindInferenceEngineService(this.engine);
    }

    private void givenInferenceEngineNotBound() {
        this.aiComponent.bindInferenceEngineService(null);
    }

    private void givenWireProperties(String preprocModelName, String inferModelName, String postprocModelName) {
        this.compProperties = new HashMap<>();

        if (!preprocModelName.isEmpty()) {
            this.compProperties.put(PREPROCESSOR_MODEL_NAME, preprocModelName);
        }

        if (!inferModelName.isEmpty()) {
            this.compProperties.put(INFERENCE_MODEL_NAME, inferModelName);
        }

        if (!postprocModelName.isEmpty()) {
            this.compProperties.put(POSTPROCESSOR_MODEL_NAME, postprocModelName);
        }
    }

    private void givenActivate() {
        ComponentContext mockContext = mock(ComponentContext.class);
        this.aiComponent.activate(mockContext, this.compProperties);
    }

    private void givenInputEnvelope() {
        List<WireRecord> records = new ArrayList<>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>() {

            private static final long serialVersionUID = 1L;

            {
                put("IN_INT", TypedValues.newIntegerValue(10));
                put("IN_BOOL", TypedValues.newBooleanValue(false));
                put("IN_STR", TypedValues.newStringValue("99"));
                put("IN_BYTEARR", TypedValues.newByteArrayValue(new byte[] { (byte) 0xe0, 0x4f }));
                put("IN_DOUBLE", TypedValues.newDoubleValue((double) 0.16));
                put("IN_FLOAT", TypedValues.newFloatValue((float) 30));
                put("IN_LONG", TypedValues.newLongValue((long) 11));
            }
        };

        records.add(new WireRecord(recordProps));

        this.inputEnvelope = new WireEnvelope("example_asset_pid", records);
    }

    private void givenModelInfo(String modelName) {
        List<Long> inputShape = new ArrayList<>();
        List<Long> outputShape = new ArrayList<>();

        inputShape.add((long) 1);
        outputShape.add((long) 1);

        List<TensorDescriptor> inDescs = new ArrayList<>();
        inDescs.add(new TensorDescriptor("IN_INT", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_BOOL", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_STR", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(
                new TensorDescriptor("IN_BYTEARR", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_DOUBLE", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_LONG", "", Optional.empty(), inputShape, new HashMap<String, Object>()));

        List<TensorDescriptor> outDescs = new ArrayList<>();
        outDescs.add(new TensorDescriptor("IN_INT", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(new TensorDescriptor("IN_BOOL", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(new TensorDescriptor("IN_STR", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(
                new TensorDescriptor("IN_BYTEARR", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(
                new TensorDescriptor("IN_DOUBLE", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(new TensorDescriptor("IN_LONG", "", Optional.empty(), outputShape, new HashMap<String, Object>()));

        ModelInfo info = mock(ModelInfo.class);
        when(info.getName()).thenReturn(modelName);
        when(info.getInputs()).thenReturn(inDescs);
        when(info.getOutputs()).thenReturn(outDescs);

        this.engine.addModelInfo(info);
    }

    /*
     * When
     */

    private void whenOnWireReceive(WireEnvelope env) {
        try {
            this.aiComponent.onWireReceive(env);
        } catch (Exception e) {
            this.exceptionsOccurred = true;
            logger.error("ERROR", e);
        }
    }

    /*
     * Then
     */

    private void thenInferIsCalled(int nTimes) {
        try {
            assertEquals(nTimes, this.engine.wasCalledTimes());
        } catch (Exception e) {
            this.exceptionsOccurred = true;
            logger.error("ERROR", e);
        }
    }

    private void thenNoExceptionsOccurred() {
        assertFalse(this.exceptionsOccurred);
    }

    private void thenExceptionsOccurred() {
        assertTrue(this.exceptionsOccurred);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.aiComponent = new AIComponent();

        // wire dependencies
        WireHelperService wireHelperService = mock(WireHelperService.class);
        WireSupport wireSupport = mock(WireSupport.class);
        when(wireHelperService.newWireSupport(any(), any())).thenReturn(wireSupport);
        this.aiComponent.bindWireHelperService(wireHelperService);

        this.exceptionsOccurred = false;
    }

}