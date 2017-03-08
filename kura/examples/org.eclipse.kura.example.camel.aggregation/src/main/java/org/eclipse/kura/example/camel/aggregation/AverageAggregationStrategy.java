/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc and others
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

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

final class AverageAggregationStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        } else {
            double incomingValue = newExchange.getIn().getBody(double.class);
            double existingValue = oldExchange.getIn().getBody(double.class);
            newExchange.getIn().setBody((incomingValue + existingValue) / 2d);
            return newExchange;
        }
    }
}