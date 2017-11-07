/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.s7plc.task;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDriver;
import org.junit.Test;

import Moka7.S7;
import Moka7.S7Client;

public class S7PlcToplevelBlockTaskTest {

    @Test
    public void testProcessBufferRead() throws IOException, NoSuchFieldException {
        S7PlcDriver driver = new S7PlcDriver();

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(driver, "client", s7Mock);

        int db = 3;
        int start = 0;

        when(s7Mock.ReadArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), anyObject())).thenReturn(0);

        Mode mode = Mode.READ;
        int end = 5;
        S7PlcToplevelBlockTask task = new S7PlcToplevelBlockTask(driver, mode, db, start, end);

        task.processBuffer();

        verify(s7Mock, times(1)).ReadArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), anyObject());
    }

    @Test
    public void testProcessBufferWrite() throws IOException, NoSuchFieldException {
        S7PlcDriver driver = new S7PlcDriver();

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(driver, "client", s7Mock);

        int db = 3;
        int start = 0;

        when(s7Mock.WriteArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), anyObject())).thenReturn(0);

        Mode mode = Mode.UPDATE;
        int end = 5;
        S7PlcToplevelBlockTask task = new S7PlcToplevelBlockTask(driver, mode, db, start, end);

        task.processBuffer();

        verify(s7Mock, times(1)).WriteArea(eq(S7.S7AreaDB), eq(db), eq(start), eq(5), anyObject());
    }
}
