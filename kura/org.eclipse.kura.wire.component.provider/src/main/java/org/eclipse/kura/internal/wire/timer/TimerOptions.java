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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The Class TimerOptions is responsible to contain all the Timer related
 * configurable options
 */
final class TimerOptions {

    /** The Constant denoting the interval property for the CRON expression */
    private static final String PROP_CRON_INTERVAL = "cron.interval";

    /** The Constant denoting the simple interval property from the metatype */
    private static final String PROP_SIMPLE_INTERVAL = "simple.interval";

    private static final String PROP_SIMPLE_TIME_UNIT = "simple.time.unit";

    private static final String PROP_INTERVAL_TYPE = "type";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new Timer options.
     *
     * @param properties
     *            the provided properties
     */
    TimerOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = properties;
    }

    /**
     * Returns the rate of refresh for this view.
     *
     * @return the CRON expression
     */
    String getCronExpression() {
        String expression = null;
        final Object interval = this.properties.get(PROP_CRON_INTERVAL);
        if (nonNull(interval) && interval instanceof String) {
            expression = (String) interval;
        }
        return expression;
    }

    /**
     * Returns the simple interval as configured.
     *
     * @return the simple interval
     */
    int getSimpleInterval() {
        int interval = 0;
        final Object simpleInterval = this.properties.get(PROP_SIMPLE_INTERVAL);
        if (nonNull(simpleInterval) && simpleInterval instanceof Integer) {
            interval = (Integer) simpleInterval;
        }
        return interval;
    }

    /**
     * Returns type as configured.
     *
     * @return the configured type
     */
    String getType() {
        String type = null;
        final Object timerType = this.properties.get(PROP_INTERVAL_TYPE);
        if (nonNull(timerType) && timerType instanceof String) {
            type = (String) timerType;
        }
        return type;
    }

    long getSimpleTimeUnitMultiplier() throws IllegalArgumentException {
        String timeUnitString = (String) properties.getOrDefault(PROP_SIMPLE_TIME_UNIT, "SECONDS");
        TimeUnit timeUnit = TimeUnit.SECONDS;

        if (TimeUnit.MILLISECONDS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.MILLISECONDS;
        } else if (TimeUnit.SECONDS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.SECONDS;
        } else if (TimeUnit.MINUTES.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.MINUTES;
        } else if (TimeUnit.HOURS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.HOURS;
        } else if (TimeUnit.DAYS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Invalid time unit");
        }

        return timeUnit.toMillis(1);
    }
}