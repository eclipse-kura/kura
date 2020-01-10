/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.clock.ClockEvent;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronTimerExecutor implements TimerExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CronTimerExecutor.class);
    private static final String GROUP_ID = "wires";

    private static AtomicInteger nextJobId = new AtomicInteger(0);
    private static SchedulerManager schedulerManager = new SchedulerManager();

    private final JobKey jobKey;

    public CronTimerExecutor(final TimerOptions options, final WireSupport wireSupport) throws SchedulerException {

        final String expression = options.getCronExpression();
        final int id = nextJobId.incrementAndGet();

        this.jobKey = new JobKey("emitJob" + id, GROUP_ID);
        final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
                .withSchedule(CronScheduleBuilder.cronSchedule(expression)).build();

        final TimerJobDataMap jobDataMap = new TimerJobDataMap();
        jobDataMap.putWireSupport(wireSupport);
        final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.jobKey).setJobData(jobDataMap).build();

        try {
            schedulerManager.onInstanceCreated();
            schedulerManager.scheduleJob(job, trigger);
        } catch (final Exception e) {
            schedulerManager.onInstanceDestroyed();
            throw e;
        }
    }

    @Override
    public void shutdown() {
        try {
            schedulerManager.deleteJob(jobKey);
        } catch (final Exception e) {
            logger.warn("failed to delete job", e);
        }
        schedulerManager.onInstanceDestroyed();
    }

    private static class SchedulerManager {

        private int instanceCount;
        private Optional<Scheduler> scheduler = Optional.empty();
        private Optional<ServiceRegistration<EventHandler>> clockChangeEventHandler = Optional.empty();

        Scheduler getScheduler() throws SchedulerException {
            if (this.scheduler.isPresent()) {
                return this.scheduler.get();
            }

            final Scheduler newScheduler = new StdSchedulerFactory().getScheduler();
            newScheduler.start();

            this.scheduler = Optional.of(newScheduler);

            return newScheduler;
        }

        synchronized void onInstanceCreated() {
            instanceCount++;

            if (!clockChangeEventHandler.isPresent()) {
                final Dictionary<String, Object> eventHandlerProperties = new Hashtable<>();
                eventHandlerProperties.put(EventConstants.EVENT_TOPIC, ClockEvent.CLOCK_EVENT_TOPIC);

                final BundleContext bundleContext = FrameworkUtil.getBundle(CronTimerExecutor.class).getBundleContext();

                clockChangeEventHandler = Optional.of(bundleContext.registerService(EventHandler.class,
                        e -> rescheduleTriggers(), eventHandlerProperties));
            }
        }

        synchronized void onInstanceDestroyed() {
            instanceCount--;

            if (instanceCount > 0) {
                return;
            }

            if (clockChangeEventHandler.isPresent()) {
                clockChangeEventHandler.get().unregister();
                clockChangeEventHandler = Optional.empty();
            }

            if (scheduler.isPresent()) {
                try {
                    scheduler.get().shutdown();
                } catch (final Exception e) {
                    logger.warn("failed to shutdown scheduler", e);
                }
                scheduler = Optional.empty();
            }
        }

        synchronized void scheduleJob(final JobDetail jobDetail, final Trigger trigger) throws SchedulerException {
            getScheduler().scheduleJob(jobDetail, trigger);
        }

        synchronized void deleteJob(final JobKey jobKey) throws SchedulerException {
            getScheduler().deleteJob(jobKey);
        }

        synchronized void rescheduleTriggers() {
            if (!this.scheduler.isPresent()) {
                return;
            }

            logger.info("system time changed, rescheduling triggers...");

            final Scheduler currentScheduler = this.scheduler.get();

            final Set<TriggerKey> keys;

            try {
                keys = currentScheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup());
            } catch (final SchedulerException e) {
                logger.warn("failed to get scheduled triggers", e);
                return;
            }

            for (final TriggerKey key : keys) {
                try {
                    final Trigger newTrigger = currentScheduler.getTrigger(key).getTriggerBuilder().startNow().build();
                    logger.debug("rescheduling {}...", key);
                    currentScheduler.rescheduleJob(key, newTrigger);
                    logger.debug("rescheduling {}...done", key);
                } catch (final Exception e) {
                    logger.warn("failed to reschedule trigger {}", key, e);
                }
            }

            logger.info("system time changed, rescheduling triggers...done");
        }
    }

}
