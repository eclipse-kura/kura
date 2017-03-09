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

import java.util.Random;

import org.apache.camel.Exchange;
import org.apache.camel.support.ExpressionAdapter;

final class RandomTemperatureGenerator extends ExpressionAdapter {

    private int minimum;
    private int maximum;

    private final Random random = new Random();

    public RandomTemperatureGenerator(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public Object evaluate(Exchange exchange) {
        return this.random.nextInt(this.maximum - this.minimum) + this.minimum;
    }

    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }

}