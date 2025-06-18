/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.test;

import java.util.Random;

public class RequestIdGenerator {

    private static RequestIdGenerator instance = new RequestIdGenerator();

    private final Random random;

    private RequestIdGenerator() {
        super();
        this.random = new Random();
    }

    public static RequestIdGenerator getInstance() {
        return instance;
    }

    public String next() {
        long timestamp = System.currentTimeMillis();

        long tempRandom;
        synchronized (this.random) {
            tempRandom = this.random.nextLong();
        }

        return timestamp + "-" + tempRandom;
    }
}
