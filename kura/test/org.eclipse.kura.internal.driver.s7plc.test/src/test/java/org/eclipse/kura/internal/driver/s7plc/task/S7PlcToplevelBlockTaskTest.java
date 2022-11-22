/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.driver.s7plc.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDriver;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDriverTest;
import org.junit.Test;

import Moka7.S7;
import Moka7.S7Client;

public class S7PlcToplevelBlockTaskTest {

    @Test
    public void testProcessBufferRead() throws IOException, NoSuchFieldException, ConnectionException {

        S7Client s7Mock = mock(S7Client.class);
        S7PlcDriver driver = S7PlcDriverTest.createTestDriver(s7Mock);

        driver.activate(Collections.emptyMap());
        driver.connect();

        int db = 3;
        int start = 0;

        when(s7Mock.ReadArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), any())).thenReturn(0);

        Mode mode = Mode.READ;
        int end = 5;
        S7PlcToplevelBlockTask task = new S7PlcToplevelBlockTask(driver, mode, db, start, end);

        task.processBuffer();

        verify(s7Mock, times(1)).ReadArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), any());
    }

    @Test
    public void testProcessBufferWrite() throws IOException, NoSuchFieldException, ConnectionException {

        S7Client s7Mock = mock(S7Client.class);
        S7PlcDriver driver = S7PlcDriverTest.createTestDriver(s7Mock);

        driver.activate(Collections.emptyMap());
        driver.connect();

        int db = 3;
        int start = 0;

        when(s7Mock.WriteArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), any())).thenReturn(0);

        Mode mode = Mode.UPDATE;
        int end = 5;
        S7PlcToplevelBlockTask task = new S7PlcToplevelBlockTask(driver, mode, db, start, end);

        task.processBuffer();

        verify(s7Mock, times(1)).WriteArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), any());
    }
}
