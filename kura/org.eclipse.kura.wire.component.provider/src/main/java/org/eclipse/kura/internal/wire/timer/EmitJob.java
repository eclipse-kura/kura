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

import static org.eclipse.kura.type.TypedValues.EMPTY_VALUE;
import static org.eclipse.kura.wire.SeverityLevel.CONFIG;

import java.util.Arrays;

import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * The Class EmitJob is responsible for emitting Wire Record every specified
 * interval (or specified CRON job interval)
 */
@DisallowConcurrentExecution
public final class EmitJob implements Job {

    /** Timer Field Constant */
    private static final String PROP = "TIMER";

    /**
     * Emits a Wire Record every specified interval.
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
        wireSupport.emit(Arrays.asList(new WireRecord(new WireField(PROP, EMPTY_VALUE, CONFIG))));
    }

}
