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

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Timer represents a Wire Component which triggers a ticking event on
 * every interval as configured. It fires the event on every tick.
 */
public final class Timer implements WireEmitter, ConfigurableComponent {

    /** Group Identifier for Quartz Job and Triggers */
    private static final String GROUP_ID = "wires";

    /** This is required to generate unique ID for the Quartz Trigger and Job */
    private static int id = 0;

    private static final Logger logger = LoggerFactory.getLogger(Timer.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Job Key for Quartz Scheduling */
    private JobKey jobKey;

    private Scheduler scheduler;

    private TimerOptions timerOptions;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi service component activation callback
     *
     * @param ctx
     *            the component context
     * @param properties
     *            the configured properties
     */
    protected synchronized void activate(final ComponentContext ctx, final Map<String, Object> properties) {
        logger.debug(message.activatingTimer());
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.timerOptions = new TimerOptions(properties);
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            this.doUpdate();
        } catch (final SchedulerException e) {
            logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        logger.debug(message.activatingTimerDone());
    }

    /**
     * OSGi service component modification callback
     *
     * @param properties
     *            the updated properties
     */
    protected synchronized void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingTimer());
        this.timerOptions = new TimerOptions(properties);
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            this.doUpdate();
        } catch (final SchedulerException e) {
            logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        logger.debug(message.updatingTimerDone());
    }

    /**
     * OSGi service component deactivation callback
     *
     * @param ctx
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext ctx) {
        logger.debug(message.deactivatingTimer());
        if (this.jobKey != null) {
            try {
                this.scheduler.deleteJob(this.jobKey);
            } catch (final SchedulerException e) {
                logger.error(ThrowableUtil.stackTraceAsString(e));
            }
        }
        logger.debug(message.deactivatingTimerDone());
    }

    /**
     * Perform update operation which internally emits a Wire Record every
     * interval
     *
     * @throws SchedulerException
     *             if job scheduling fails
     */
    private void doUpdate() throws SchedulerException {
        int interval;
        if ("SIMPLE".equalsIgnoreCase(this.timerOptions.getType())) {
            interval = this.timerOptions.getSimpleInterval();
            this.scheduleSimpleInterval(interval);
            return;
        }
        final String cronExpression = this.timerOptions.getCronExpression();
        this.scheduleCronInterval(cronExpression);
    }

    /**
     * Creates a trigger based on the provided interval
     *
     * @param interval
     *            the interval
     * @throws SchedulerException
     *             if scheduling fails
     * @throws IllegalArgumentException
     *             if the interval is less than or equal to zero
     */
    private void scheduleSimpleInterval(final int interval) throws SchedulerException {
        if (interval <= 0) {
            throw new IllegalArgumentException(message.intervalNonLessThanEqualToZero());
        }
        ++id;
        if (this.jobKey != null) {
            this.scheduler.deleteJob(this.jobKey);
        }
        this.jobKey = new JobKey("emitJob" + id, GROUP_ID);
        final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .build();

        final TimerJobDataMap jobDataMap = new TimerJobDataMap();
        jobDataMap.putWireSupport(this.wireSupport);
        final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.jobKey).setJobData(jobDataMap).build();

        this.scheduler.start();
        this.scheduler.scheduleJob(job, trigger);
    }

    /**
     * Creates a cron trigger based on the provided interval
     *
     * @param expression
     *            the CRON expression
     * @throws SchedulerException
     *             if scheduling fails
     * @throws NullPointerException
     *             if the argument is null
     */
    private void scheduleCronInterval(final String expression) throws SchedulerException {
        requireNonNull(expression, message.cronExpressionNonNull());
        ++id;
        if (this.jobKey != null) {
            this.scheduler.deleteJob(this.jobKey);
        }
        this.jobKey = new JobKey("emitJob" + id, GROUP_ID);
        final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
                .withSchedule(CronScheduleBuilder.cronSchedule(expression)).build();

        final TimerJobDataMap jobDataMap = new TimerJobDataMap();
        jobDataMap.putWireSupport(this.wireSupport);
        final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.jobKey).setJobData(jobDataMap).build();

        this.scheduler.getContext().put("wireSupport", this.wireSupport);
        this.scheduler.start();

        this.scheduler.scheduleJob(job, trigger);
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /**
     * This is not a good practice though but in case of Timer, it is very much
     * needed because while deactivating, we cannot just stop the scheduler.
     * Scheduler is a singleton instance shared by all the different instances
     * of Timer and if one Timer is explicitly stopped, all the other Timer
     * instances will be affected. So, it is better to have it dereferenced
     * while finalizing its all references. Even though it is not guaranteed
     * that the reference will be garbage collected at a certain point of time,
     * it is an advise to use it as it is better late than never.
     */
    @Override
    protected void finalize() throws Throwable {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
            this.scheduler = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }
}
