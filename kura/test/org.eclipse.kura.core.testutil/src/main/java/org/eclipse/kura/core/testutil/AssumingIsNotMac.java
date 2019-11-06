/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.testutil;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class AssumingIsNotMac implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    throw new AssumptionViolatedException("Mac OS X not supported. Skipping tests!");
                } else {
                    base.evaluate();
                }
            }
        };
    }
}
