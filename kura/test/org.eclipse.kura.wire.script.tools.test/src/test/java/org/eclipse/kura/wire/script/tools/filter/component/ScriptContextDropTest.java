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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.script.tools.TestScripts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.component.ComponentContext;

public class ScriptContextDropTest {

    private FilterComponent filterComponent;
    private Map<String, Object> properties;

    private ArgumentCaptor<Object> outputCaptor = ArgumentCaptor.forClass(Object.class);
    private WireSupport wireSupport = mock(WireSupport.class);
    private WireHelperService wireHelperService = mock(WireHelperService.class);
    private ComponentContext context = mock(ComponentContext.class);

    /*
     * Scenarios
     */

    @Test
    public void shouldDropContext() {
        givenCounterScript();
        givenScriptContextDrop(true);
        givenActivate();

        whenOnWireReceiveWithUpdateBetween(10, 5);

        thenEmittedRecordsAre(15);
        thenCounterValueIs(5);
    }

    @Test
    public void shouldNotDropContext() {
        givenCounterScript();
        givenScriptContextDrop(false);
        givenActivate();

        whenOnWireReceiveWithUpdateBetween(10, 10);

        thenEmittedRecordsAre(20);
        thenCounterValueIs(20);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenActivate() {
        this.filterComponent.activate(this.context, this.properties);
    }

    private void givenScriptContextDrop(boolean value) {
        this.properties.put(FilterComponentOptions.SCRIPT_CONTEXT_DROP_KEY, value);
    }

    private void givenCounterScript() {
        this.properties.put(FilterComponentOptions.SCRIPT_KEY, TestScripts.counter());
    }

    /*
     * When
     */

    private void whenOnWireReceive(int receivedWiresNr) {
        WireEnvelope emptyEnvelope = new WireEnvelope("test", new LinkedList<WireRecord>());
        
        for (int i = 0; i < receivedWiresNr; i++) {
            this.filterComponent.onWireReceive(emptyEnvelope);
        }
    }

    private void whenOnWireReceiveWithUpdateBetween(int receivedWiresNr, int receivedWiresNrAfterUpdate) {
        whenOnWireReceive(receivedWiresNr);
        this.filterComponent.updated(this.properties);
        whenOnWireReceive(receivedWiresNrAfterUpdate);
    }

    /*
     * Then
     */

    @SuppressWarnings("unchecked")
    private void thenEmittedRecordsAre(int expectedNrInvocations) {
        verify(this.wireSupport, times(expectedNrInvocations)).emit((List<WireRecord>) this.outputCaptor.capture());
    }

    @SuppressWarnings("unchecked")
    private void thenCounterValueIs(int expectedValue) {
        List<Object> captures = this.outputCaptor.getAllValues();
        List<WireRecord> lastCapture = (List<WireRecord>) captures.get(captures.size() - 1);
        int outputCounterValue = (int) lastCapture.get(0).getProperties().get("counter").getValue();
        assertEquals(expectedValue, outputCounterValue);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.filterComponent = new FilterComponent();
        when(this.wireHelperService.newWireSupport(any(), any())).thenReturn(this.wireSupport);
        this.filterComponent.bindWireHelperService(this.wireHelperService);

        this.properties = new HashMap<>();
    }

    @After
    public void simulateDeactivate() {
        this.filterComponent.unbindWireHelperService(this.wireHelperService);
        this.filterComponent.deactivate();
    }

}
