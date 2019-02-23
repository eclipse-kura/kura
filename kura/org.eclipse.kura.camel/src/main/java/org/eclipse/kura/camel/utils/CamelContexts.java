/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.utils;

import static org.eclipse.kura.camel.runner.ScriptRunner.create;

import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.runner.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CamelContexts {

    private static final Logger logger = LoggerFactory.getLogger(CamelContexts.class);

    private CamelContexts() {
    }

    /**
     * Run an init script on a camel context
     * 
     * @param context
     *            the context to work on
     * @param initCode
     *            the init code, may be {@code null} or empty
     * @param classLoader
     *            the classloader to use for the script engine, may be {@code null}
     * @throws ScriptException
     *             if calling the script fails
     */
    public static void scriptInitCamelContext(final CamelContext context, final String initCode,
            final ClassLoader classLoader) throws ScriptException {

        // pre-flight check

        if (initCode == null || initCode.isEmpty()) {
            return;
        }

        try {

            // setup runner

            final ScriptRunner runner = create(classLoader, "JavaScript", initCode);

            // setup arguments

            final SimpleBindings bindings = new SimpleBindings();
            bindings.put("camelContext", context);

            // perform call

            runner.run(bindings);
        } catch (final Exception e) {
            logger.warn("Failed to run init code", e);
            throw e;
        }
    }

}
