/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.testutil;

import java.io.File;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;

public class AssumingIsNotJenkins implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                File tempFile = new File("/tmp/isJenkins.txt");
                if (tempFile.exists()) {
                    throw new AssumptionViolatedException("Jenkins detected. Skipping tests!");
                } else {
                    base.evaluate();
                }
            }
        };
    }
}
