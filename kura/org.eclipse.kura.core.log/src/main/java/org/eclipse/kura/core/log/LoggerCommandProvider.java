/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech and/or its affiliates
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.log;

import static org.apache.log4j.Level.toLevel;
import static org.apache.log4j.LogManager.getCurrentLoggers;
import static org.apache.log4j.LogManager.getRootLogger;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.service.command.Descriptor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LoggerCommandProvider {

    @Descriptor("Shows the Kura log level for all loggers")
    public void getkuraloglevel() {
        getkuraloglevel(null);
    }

    @Descriptor("Shows the Kura log level for a specific logger")
    public void getkuraloglevel(@Descriptor("<logger name>") String argument) {

        final Enumeration<?> currentLoggers = getCurrentLoggers();
        final Map<String, Logger> loggers = new TreeMap<>();

        // enumerate all loggers

        while (currentLoggers.hasMoreElements()) {
            final Logger log4jLogger = (Logger) currentLoggers.nextElement();
            loggers.put(log4jLogger.getName(), log4jLogger);
        }

        // display

        if (argument == null || argument.isEmpty()) {
            // Display all
            System.out.format("Default log level: %s%n", getRootLogger().getEffectiveLevel());

            for (final Logger logger : loggers.values()) {
                System.out.format("%s: %s%n", logger.getName(), logger.getEffectiveLevel());
            }
        } else {
            // Display single
            final Logger logger = loggers.get(argument);

            if (logger != null) {
                System.out.format("%s: %s%n", logger.getName(), logger.getEffectiveLevel());
            } else {
                System.out.format("Logger not found: %s%n", argument);
            }
        }
    }

    @Descriptor("Sets the Kura log level for a given logger, or all loggers if 'ALL'")
    public void setkuraloglevel(@Descriptor("(ALL | <logger name>)") String argName,
            @Descriptor("(ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF)") String argLevel) {
        try {

            // init

            final boolean setAll = argName.equalsIgnoreCase("ALL");
            final Enumeration<?> currentLoggers = getCurrentLoggers();

            // iterate over all loggers

            while (currentLoggers.hasMoreElements()) {
                final Logger logger = (Logger) currentLoggers.nextElement();

                if (setAll || argName.equals(logger.getName())) {

                    // set new level, falling back to current

                    final Level oldLevel = logger.getEffectiveLevel();
                    logger.setLevel(toLevel(argLevel, oldLevel));
                    System.out.format("%s: %s%n", logger.getName(), logger.getEffectiveLevel().toString());

                    if (!setAll) {
                        // we are done
                        return;
                    }
                }
            }

            // we had a request for a specific logger, but could not find it

            if (!setAll) {
                System.out.format("Could not find logger for: %s%n", argName);
            }

        } catch (final Exception e) {
            // the OSGi shell has exception handling capabilities
            throw new IllegalArgumentException("Invalid argument passed to setkuraloglevel", e);
        }
    }
}
