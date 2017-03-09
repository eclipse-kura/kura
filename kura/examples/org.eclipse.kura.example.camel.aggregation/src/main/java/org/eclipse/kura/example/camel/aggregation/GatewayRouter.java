/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - Initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.example.camel.aggregation;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.kura.camel.component.Configuration.asInt;

import java.util.Map;

import org.eclipse.kura.camel.component.AbstractJavaCamelComponent;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of the Kura Camel application.
 */
public class GatewayRouter extends AbstractJavaCamelComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(GatewayRouter.class);

    private static final int DEFAULT_MINIMUM = 0;
    private static final int DEFAULT_MAXIMUM = 40;

    private RandomTemperatureGenerator randomTemperatureGenerator = new RandomTemperatureGenerator(DEFAULT_MINIMUM,
            DEFAULT_MAXIMUM);

    @Override
    public void configure() throws Exception {
        from("timer://temperature") //
                .setBody(this.randomTemperatureGenerator) //
                .aggregate(simple("temperature"), new AverageAggregationStrategy()) //
                .completionInterval(SECONDS.toMillis(10)) //
                .to("log:averageTemperatureFromLast10Seconds");
    }

    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws Exception {
        logger.info("Activated");

        setProperties(properties);
        start();
    }

    protected void modified(final Map<String, Object> properties) {
        logger.info("Modified");

        setProperties(properties);
    }

    protected void deactivate() throws Exception {
        stop();
    }

    private void setProperties(final Map<String, Object> properties) {
        int minimum = asInt(properties, "minimum", DEFAULT_MINIMUM);
        int maximum = asInt(properties, "maximum", DEFAULT_MAXIMUM);

        if (maximum - minimum <= 0) {
            throw new IllegalArgumentException("Maximum must be at least one higher than minimum");
        }

        this.randomTemperatureGenerator.setMinimum(minimum);
        this.randomTemperatureGenerator.setMaximum(maximum);
    }
}
