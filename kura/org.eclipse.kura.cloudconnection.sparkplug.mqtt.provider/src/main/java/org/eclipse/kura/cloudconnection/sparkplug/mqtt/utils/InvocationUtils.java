/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvocationUtils {
    
    private static final String ERROR_MESSAGE_FORMAT = "An error occurred in listener '%s'";

    private InvocationUtils() {
    }

    private static final Logger logger = LoggerFactory.getLogger(InvocationUtils.class);

    public static void callSafely(Runnable f) {
        try {
            f.run();
        } catch (Exception e) {
            logger.error(String.format(ERROR_MESSAGE_FORMAT, f.getClass().getName()), e);
        }
    }

    public static <T> void callSafely(Consumer<T> f, T argument) {
        try {
            f.accept(argument);
        } catch (Exception e) {
            logger.error(String.format(ERROR_MESSAGE_FORMAT, f.getClass().getName()), e);
        }
    }

}
