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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.script.tools.TestScripts;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.component.ComponentContext;

public class IOScriptBindingsTest {

    private ArgumentCaptor<Object> outputCaptor = ArgumentCaptor.forClass(Object.class);
    private WireSupport wireSupport = mock(WireSupport.class);
    private WireHelperService wireHelperService = mock(WireHelperService.class);

    private FilterComponent filterComponent;
    private Map<String, Object> properties;
    private List<WireRecord> inputRecords;
    private WireEnvelope inputEnvelope;

    public IOScriptBindingsTest() {
        this.filterComponent = new FilterComponent();
        this.properties = new HashMap<>();
        this.inputRecords = new LinkedList<>();

        when(this.wireHelperService.newWireSupport(any(), any())).thenReturn(this.wireSupport);
        this.filterComponent.bindWireHelperService(this.wireHelperService);

        ComponentContext context = mock(ComponentContext.class);
        this.filterComponent.activate(context, new HashMap<String, Object>());
    }

    /*
     * Scenarios
     */

    @Test
    public void shouldInvertBoolean() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.invertBooleanOnProperty("p1"));
        givenUpdated(this.properties);
        givenInputWireRecord("p1", TypedValues.newBooleanValue(true));
        givenInputWireEnvelope("test.pid.1");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p1", false);
    }

    @Test
    public void shouldIncrementInteger() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.incrementIntegerOnProperty("p2"));
        givenUpdated(this.properties);
        givenInputWireRecord("p2", TypedValues.newIntegerValue(2));
        givenInputWireEnvelope("test.pid.2");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p2", 3);
    }

    @Test
    public void shouldIncrementDouble() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.incrementDoubleOnProperty("p3"));
        givenUpdated(this.properties);
        givenInputWireRecord("p3", TypedValues.newDoubleValue(3));
        givenInputWireEnvelope("test.pid.3");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p3", 4.0);
    }

    @Test
    public void shouldIncrementLong() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.incrementLongOnProperty("p4"));
        givenUpdated(this.properties);
        givenInputWireRecord("p4", TypedValues.newLongValue(4L));
        givenInputWireEnvelope("test.pid.4");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p4", 5L);
    }

    @Test
    public void shouldIncrementFloat() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.incrementFloatOnProperty("p5"));
        givenUpdated(this.properties);
        givenInputWireRecord("p5", TypedValues.newFloatValue(5));
        givenInputWireEnvelope("test.pid.5");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p5", (float) 6);
    }

    @Test
    public void shouldReturnInputByteArray() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.identityByteArrayOnProperty("p6"));
        givenUpdated(this.properties);
        givenInputWireRecord("p6", TypedValues.newByteArrayValue(new byte[] { 0, 0 }));
        givenInputWireEnvelope("test.pid.6");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p6", new byte[] { 0, 0 });
    }

    @Test
    public void shouldConcatenateStrings() {
        givenProperty(FilterComponentOptions.SCRIPT_KEY, TestScripts.appendStringOnProperty("p7", " world"));
        givenUpdated(this.properties);
        givenInputWireRecord("p7", TypedValues.newStringValue("hello"));
        givenInputWireEnvelope("test.pid.7");

        whenOnWireReceive();

        thenOutputWireRecordListIsEmitted();
        thenOutputContains("p7", "hello world");
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
        this.filterComponent.updated(properties);
    }

    private void givenInputWireRecord(String propertyKey, TypedValue<?> propertyValue) {
        Map<String, TypedValue<?>> inputProperty = new HashMap<>();
        inputProperty.put(propertyKey, propertyValue);
        this.inputRecords.add(new WireRecord(inputProperty));
    }

    private void givenInputWireEnvelope(String emitterPid) {
        this.inputEnvelope = new WireEnvelope(emitterPid, this.inputRecords);
    }

    /*
     * When
     */

    private void whenOnWireReceive() {
        this.filterComponent.onWireReceive(this.inputEnvelope);
    }

    /*
     * Then
     */

    @SuppressWarnings("unchecked")
    private void thenOutputWireRecordListIsEmitted() {
        verify(this.wireSupport).emit((List<WireRecord>) outputCaptor.capture());
    }

    @SuppressWarnings("unchecked")
    private void thenOutputContains(String propertyKey, Object propertyValue) {
        List<WireRecord> outputRecords = (List<WireRecord>) outputCaptor.getValue();

        boolean found = false;
        for (WireRecord outputRecord : outputRecords) {
            if (outputRecord.getProperties().containsKey(propertyKey)) {
                TypedValue<?> value = outputRecord.getProperties().get(propertyKey);
                
                if (value.getValue() instanceof byte[]) {
                    assertTrue(Arrays.equals((byte[]) propertyValue, (byte[]) value.getValue()));
                } else {
                    assertEquals(propertyValue, value.getValue());
                }
                
                found = true;
            }
        }

        assertTrue(found);
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
