/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EmitJobTest {

    @Test
    public void testExecute() throws JobExecutionException {
        WireSupport mockWireSupport = mock(WireSupport.class);
        
        TimerJobDataMap timerJobDataMap = new TimerJobDataMap();
        timerJobDataMap.putWireSupport(mockWireSupport);

        JobDetail mockJobDetail = mock(JobDetail.class);
        when(mockJobDetail.getJobDataMap()).thenReturn(timerJobDataMap);
        
        JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
        when(mockJobExecutionContext.getJobDetail()).thenReturn(mockJobDetail);

        long startTime = new Date().getTime();
        
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

            assertEquals(1, wireRecords.size());
            Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

            long endTime = new Date().getTime();
            
            assertEquals(1, properties.size());
            
            LongValue timerTime = (LongValue) properties.get("TIMER");
            
            // Make sure timer time is between start and end of the test
            assertTrue(startTime <= timerTime.getValue());
            assertTrue(timerTime.getValue() <= endTime);

            return null;
        }).when(mockWireSupport).emit(any());
        
        EmitJob job = new EmitJob();
        
        job.execute(mockJobExecutionContext);

        verify(mockWireSupport).emit(any());
    }

}
