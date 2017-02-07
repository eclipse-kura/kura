/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * The Class EmitJob is responsible for emitting {@link WireRecord} every specified
 * interval (or specified CRON job interval)
 */
@DisallowConcurrentExecution
public final class EmitJob implements Job {

    /** Timer Field Constant */
    private static final String PROP = "TIMER";

    /**
     * Emits a {@link WireRecord} every specified interval.
     *
     * @param context
     *            the Job Execution context
     * @throws JobExecutionException
     *             the job execution exception
     */
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final TimerJobDataMap dataMap = (TimerJobDataMap) context.getJobDetail().getJobDataMap();
        final WireSupport wireSupport = dataMap.getWireSupport();

        final long currentTime = new Date().getTime();
        final TypedValue<Long> timestamp = TypedValues.newLongValue(currentTime);
        final Map<String, TypedValue<?>> timerProperties = new HashMap<>();
        timerProperties.put(PROP, timestamp);

        final WireRecord timerWireRecord = new WireRecord(timerProperties);
        final List<WireRecord> timerWireRecords = new ArrayList<>();
        timerWireRecords.add(timerWireRecord);

        wireSupport.emit(timerWireRecords);
    }
}
