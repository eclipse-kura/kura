/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.camel.publisher;

import static java.lang.Math.round;
import static org.eclipse.kura.camel.component.Configuration.asDouble;
import static org.eclipse.kura.camel.component.Configuration.asInt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An example publisher based on Apache Camel
 */
public class ExamplePublisher extends AbstractSimplePeriodicPublisher<ExamplePublisher.PublisherConfiguration> {

    /**
     * Our configuration
     */
    public static final class PublisherConfiguration {

        private final int ampInt;
        private final int offsetInt;
        private final int periodInt;

        private final double ampDouble;
        private final double offsetDouble;
        private final int periodDouble;

        private PublisherConfiguration(final int ampInt, final int offsetInt, final int periodInt,
                final double ampDouble, final double offsetDouble, final int periodDouble) {

            this.ampInt = ampInt;
            this.offsetInt = offsetInt;
            this.periodInt = periodInt;

            this.ampDouble = ampDouble;
            this.offsetDouble = offsetDouble;
            this.periodDouble = periodDouble;
        }

        public int getAmpInt() {
            return this.ampInt;
        }

        public int getOffsetInt() {
            return this.offsetInt;
        }

        public int getPeriodInt() {
            return this.periodInt;
        }

        public double getAmpDouble() {
            return this.ampDouble;
        }

        public double getOffsetDouble() {
            return this.offsetDouble;
        }

        public int getPeriodDouble() {
            return this.periodDouble;
        }

        /**
         * Parse configuration from properties
         *
         * @param properties
         *            the properties to parse from
         * @return the result configuration
         */
        public static PublisherConfiguration fromProperties(final Map<String, Object> properties) {
            Objects.requireNonNull(properties);

            final int ampInt = asInt(properties, "int.amp", -20);
            final int offsetInt = asInt(properties, "int.offset", 20);
            final int periodInt = asInt(properties, "int.period", 60);

            final double ampDouble = asDouble(properties, "double.amp", -.5);
            final double offsetDouble = asDouble(properties, "double.offset", .5);
            final int periodDouble = asInt(properties, "double.period", 30);

            return new PublisherConfiguration(ampInt, offsetInt, periodInt, ampDouble, offsetDouble, periodDouble);
        }
    }

    public ExamplePublisher() {
        super("camel/example");
    }

    @Override
    protected PublisherConfiguration parseConfiguration(final Map<String, Object> properties) {
        return PublisherConfiguration.fromProperties(properties);
    }

    @Override
    protected Map<String, Object> getPayload(final PublisherConfiguration configuration) {

        // new result object

        final Map<String, Object> result = new HashMap<>(2);

        // fill with sine curves

        result.put("intValue",
                round(makeSine(configuration.getAmpInt(), configuration.getOffsetInt(), configuration.getPeriodInt())));
        result.put("doubleValue", makeSine(configuration.getAmpDouble(), configuration.getOffsetDouble(),
                configuration.getPeriodDouble()));

        // return result

        return result;
    }

    private static double makeSine(final double amp, final double offset, final double period) {
        final double freq = 1.0 / period * Math.PI * 2.0;
        final double v = System.currentTimeMillis() / 1000.0;

        return Math.sin(freq * v) * amp + offset;
    }
}
