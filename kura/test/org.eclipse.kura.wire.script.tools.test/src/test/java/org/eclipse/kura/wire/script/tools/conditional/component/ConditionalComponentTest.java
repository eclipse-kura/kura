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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.EmitterPort;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.eclipse.kura.wire.script.tools.TestScripts;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.component.ComponentContext;

public class ConditionalComponentTest {

    private ArgumentCaptor<Object> outputCaptor = ArgumentCaptor.forClass(Object.class);
    private MultiportWireSupport wireSupport = mock(MultiportWireSupport.class);
    private WireHelperService wireHelperService = mock(WireHelperService.class);

    private ConditionalComponent conditionalComponent;
    private Map<String, Object> properties;
    private List<WireRecord> inputRecords;
    private WireEnvelope inputEnvelope;

    EmitterPort portThen = mock(EmitterPort.class);
    EmitterPort portElse = mock(EmitterPort.class);

    private String wireKey1;
    private String wireKey2;

    private TypedValue<?> wireVal1;
    private TypedValue<?> wireVal2;

    private WireEnvelope inccomingWireEnvelope;

    public ConditionalComponentTest() {
        this.conditionalComponent = new ConditionalComponent();
        this.properties = new HashMap<>();
        this.inputRecords = new LinkedList<>();

        final List<EmitterPort> emitterPorts = new LinkedList<>();
        emitterPorts.add(portThen);
        emitterPorts.add(portElse);

        when(this.wireSupport.getEmitterPorts()).thenReturn(emitterPorts);
        when(this.wireHelperService.newWireSupport(any(), any())).thenReturn(this.wireSupport);

        this.conditionalComponent.bindWireHelperService(this.wireHelperService);

        ComponentContext context = mock(ComponentContext.class);
        this.conditionalComponent.activate(context, new HashMap<String, Object>());
    }

    /*
     * Scenarios
     */

    @Test
    public void compareBooleanTrue() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newBooleanValue(true), "p2", TypedValues.newBooleanValue(true));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputTrue();

    }

    @Test
    public void compareStringTrue() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newStringValue("123"), "p2", TypedValues.newStringValue("123"));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputTrue();

    }

    @Test
    public void compareFloatTrue() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newFloatValue((float) 1.334), "p2",
                TypedValues.newFloatValue((float) 1.334));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputTrue();

    }

    @Test
    public void compareDoubleTrue() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newDoubleValue(1.1), "p2", TypedValues.newDoubleValue(1.1));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputTrue();

    }

    @Test
    public void compareLongTrue() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newLongValue(111111), "p2", TypedValues.newLongValue(111111));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputTrue();

    }

    @Test
    public void compareIntTrue() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newIntegerValue(0), "p2", TypedValues.newIntegerValue(0));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputTrue();

    }

    @Test
    public void compareBooleanFalse() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newBooleanValue(false), "p2", TypedValues.newBooleanValue(true));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputFalse();

    }

    @Test
    public void compareStringFalse() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newStringValue("ghrt5g5"), "p2", TypedValues.newStringValue("123"));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputFalse();

    }

    @Test
    public void compareFloatFalse() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newFloatValue((float) 1.434353), "p2",
                TypedValues.newFloatValue((float) 1.334));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputFalse();

    }

    @Test
    public void compareDoubleFalse() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newDoubleValue(43.456), "p2", TypedValues.newDoubleValue(1.1));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputFalse();

    }

    @Test
    public void compareLongFalse() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newLongValue(234234), "p2", TypedValues.newLongValue(111111));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputFalse();

    }

    @Test
    public void compareIntFalse() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY,
                TestScripts.comparePropsOnProperty("p1", "p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newIntegerValue(42), "p2", TypedValues.newIntegerValue(0));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputFalse();

    }

    @Test
    public void whenScriptIsEmpty() {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY, "");
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newIntegerValue(42), "p2", TypedValues.newIntegerValue(0));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenNoOutputProvided();

    }

    @Test
    public void whenScriptIsOptionalEmpty() throws Exception {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY, null);
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newIntegerValue(42), "p2", TypedValues.newIntegerValue(0));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenNoOutputProvided();

    }

    @Test
    public void whenScriptResultIsIntNotBoolean() throws Exception {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY, "5 + 6");
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newIntegerValue(42), "p2", TypedValues.newIntegerValue(0));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenNoOutputProvided();

    }

    @Test
    public void whenScriptResultIsStringNotBoolean() throws Exception {
        givenProperty(ConditionalComponentOptions.CONDITION_PROPERTY_KEY, "test string");
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newIntegerValue(42), "p2", TypedValues.newIntegerValue(0));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenNoOutputProvided();

    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    private void givenUpdated(Map<String, Object> properties) {
        this.conditionalComponent.updated(properties);
    }

    private void givenInputWireRecord(String propertyKey, TypedValue<?> propertyValue, String propertyKey2,
            TypedValue<?> propertyValue2) {

        this.wireKey1 = propertyKey;
        this.wireVal1 = propertyValue;

        this.wireKey2 = propertyKey2;
        this.wireVal2 = propertyValue2;

        Map<String, TypedValue<?>> inputProperty = new HashMap<>();
        inputProperty.put(propertyKey, propertyValue);
        inputProperty.put(propertyKey2, propertyValue2);
        this.inputRecords.add(new WireRecord(inputProperty));
    }

    private void givenInputWireEnvelope(String emitterPid) {
        this.inputEnvelope = new WireEnvelope(emitterPid, this.inputRecords);
    }

    /*
     * When
     */

    private void whenOnWireReceive() {
        this.conditionalComponent.onWireReceive(this.inputEnvelope);
    }

    /*
     * Then
     */

    private void thenOutputTrue() {
        verify(this.portThen, times(1)).emit((WireEnvelope) this.outputCaptor.capture());
        verify(this.portElse, times(0)).emit(any());

    }

    private void thenOutputFalse() {
        verify(this.portElse, times(1)).emit((WireEnvelope) this.outputCaptor.capture());
        verify(this.portThen, times(0)).emit(any());
    }

    private void thenNoOutputProvided() {
        verify(this.portThen, times(0)).emit(any());
        verify(this.portElse, times(0)).emit(any());
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.properties = new HashMap<>();
        this.inputRecords = new LinkedList<>();
    }
}
