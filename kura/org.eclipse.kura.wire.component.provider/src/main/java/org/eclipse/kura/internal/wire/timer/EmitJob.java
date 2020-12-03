/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * The Class EmitJob is responsible for emitting {@link org.eclipse.kura.wire.WireRecord} every specified
 * interval (or specified CRON job interval)
 */
@DisallowConcurrentExecution
public final class EmitJob implements Job {

    /**
     * Emits a {@link org.eclipse.kura.wire.WireRecord} every specified interval.
     *
     * @param context
     *            the Job Execution context
     * @throws JobExecutionException
     *             the job execution exception
     */
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final TimerJobDataMap dataMap = (TimerJobDataMap) context.getJobDetail().getJobDataMap();
        Timer.emit(dataMap.getWireSupport());
    }
}
