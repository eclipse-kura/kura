/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnecton.raw.mqtt.util;

import java.util.function.Consumer;

import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    public static <T> Consumer<T> catchAll(final Consumer<T> consumer) {
        return item -> {
            try {
                consumer.accept(item);
            } catch (final Exception e) {
                logger.warn("unexpected exception", e);
            }
        };
    }

    public static Filter createFilter(final Class<?> type, final String kuraServicePid) throws InvalidSyntaxException {
        final StringBuilder builder = new StringBuilder();

        builder.append("(&(") //
                .append(Constants.OBJECTCLASS) //
                .append('=') //
                .append(type.getName()) //
                .append(")(") //
                .append(ConfigurationService.KURA_SERVICE_PID) //
                .append('=') //
                .append(kuraServicePid) //
                .append("))");

        return FrameworkUtil.createFilter(builder.toString());
    }
}
