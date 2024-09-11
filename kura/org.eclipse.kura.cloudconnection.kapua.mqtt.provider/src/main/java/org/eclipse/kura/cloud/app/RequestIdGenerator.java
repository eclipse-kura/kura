/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud.app;

import java.util.Random;

public class RequestIdGenerator {

    private static RequestIdGenerator s_instance = new RequestIdGenerator();

    private final Random m_random;

    private RequestIdGenerator() {
        super();
        this.m_random = new Random();
    }

    public static RequestIdGenerator getInstance() {
        return s_instance;
    }

    public String next() {
        long timestamp = System.currentTimeMillis();

        long random;
        synchronized (this.m_random) {
            random = this.m_random.nextLong();
        }

        return timestamp + "-" + random;
    }
}
