/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
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

/**
 * The Class Timer represents a Wire Component which triggers a ticking event on
 * every interval as configured. It fires the event on every tick.
 */
public class Timer implements WireEmitter, ConfigurableComponent {

    /** Group Identifier for Quartz Job and Triggers */
    private static final String GROUP_ID = "wires";

    /** This is required to generate unique ID for the Quartz Trigger and Job */
    private static AtomicInteger nextJobId = new AtomicInteger(0);

    private static final Logger logger = LogManager.getLogger(Timer.class);

    /** Job Key for Quartz Scheduling */
    private JobKey jobKey;

    private TimerOptions timerOptions;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private static final AtomicInteger instanceCount = new AtomicInteger(0);
    private static Scheduler scheduler = null;

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
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
    protected void activate(final ComponentContext ctx, final Map<String, Object> properties) {
        logger.debug("Activating Timer...");
        instanceCount.incrementAndGet();
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) ctx.getServiceReference());
        this.timerOptions = new TimerOptions(properties);
        try {
            doUpdate();
        } catch (final SchedulerException e) {
            logger.error("Scheduler exception.", e);
        }
        logger.debug("Activating Timer... Done");
    }

    /**
     * OSGi service component modification callback
     *
     * @param properties
     *            the updated properties
     */
    protected void updated(final Map<String, Object> properties) {
        logger.debug("Updating Timer...");
        this.timerOptions = new TimerOptions(properties);
        try {
            doUpdate();
        } catch (final SchedulerException e) {
            logger.error("Scheduler exception.", e);
        }
        logger.debug("Updating Timer... Done");
    }

    /**
     * OSGi service component deactivation callback
     *
     * @param ctx
     *            the component context
     */
    protected void deactivate(final ComponentContext ctx) {
        logger.debug("Dectivating Timer...");

        try {
            if (nonNull(this.jobKey)) {
                getScheduler().deleteJob(this.jobKey);
            }
        } catch (final SchedulerException e) {
            logger.error("Scheduler exception.", e);
        } finally {
            if (instanceCount.decrementAndGet() == 0) {
                shutdownScheduler();
            }
        }

        logger.debug("Dectivating Timer... Done");
    }

    protected Scheduler getScheduler() throws SchedulerException {
        synchronized (instanceCount) {
            if (scheduler == null) {
                scheduler = new StdSchedulerFactory().getScheduler();
                scheduler.start();
            }
        }
        return scheduler;
    }

    private static void shutdownScheduler() {
        synchronized (instanceCount) {
            if (scheduler != null) {
                try {
                    scheduler.shutdown();
                } catch (SchedulerException e) {
                    logger.warn("Scheduler exception.", e);
                }
                scheduler = null;
            }
        }
    }

    /**
     * Perform update operation which internally emits a {@link WireRecord} every
     * interval
     *
     * @throws SchedulerException
     *             if job scheduling fails
     */
    private void doUpdate() throws SchedulerException {
        if ("SIMPLE".equalsIgnoreCase(this.timerOptions.getType())) {
            scheduleSimpleInterval(
                    this.timerOptions.getSimpleInterval() * this.timerOptions.getSimpleTimeUnitMultiplier());
            return;
        }
        final String cronExpression = this.timerOptions.getCronExpression();
        scheduleCronInterval(cronExpression);
    }

    /**
     * Creates a trigger based on the provided interval
     *
     * @param interval
     *            the interval in milliseconds
     * @throws SchedulerException
     *             if scheduling fails
     * @throws IllegalArgumentException
     *             if the interval is less than or equal to zero
     */
    private void scheduleSimpleInterval(final long interval) throws SchedulerException {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval cannot be less than or equal to zero");
        }
        final Scheduler scheduler = getScheduler();

        final int id = nextJobId.incrementAndGet();
        if (nonNull(this.jobKey)) {
            scheduler.deleteJob(this.jobKey);
        }
        this.jobKey = new JobKey("emitJob" + id, GROUP_ID);
        final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(interval).repeatForever())
                .build();

        final TimerJobDataMap jobDataMap = new TimerJobDataMap();
        jobDataMap.putWireSupport(this.wireSupport);
        final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.jobKey).setJobData(jobDataMap).build();

        scheduler.scheduleJob(job, trigger);
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
        requireNonNull(expression, "Cron Expression cannot be null");
        final Scheduler scheduler = getScheduler();

        final int id = nextJobId.incrementAndGet();
        if (nonNull(this.jobKey)) {
            scheduler.deleteJob(this.jobKey);
        }
        this.jobKey = new JobKey("emitJob" + id, GROUP_ID);
        final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
                .withSchedule(CronScheduleBuilder.cronSchedule(expression)).build();

        final TimerJobDataMap jobDataMap = new TimerJobDataMap();
        jobDataMap.putWireSupport(this.wireSupport);
        final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.jobKey).setJobData(jobDataMap).build();

        scheduler.getContext().put("wireSupport", this.wireSupport);

        scheduler.scheduleJob(job, trigger);
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }
}
