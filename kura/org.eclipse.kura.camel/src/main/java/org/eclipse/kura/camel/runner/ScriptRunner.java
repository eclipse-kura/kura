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
package org.eclipse.kura.camel.runner;

import java.util.Objects;
import java.util.concurrent.Callable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A runner for scripts which takes care of class loader issues in OSGi
 * <br>
 * This class helps running java.script script inside an OSGi container.
 * There are some oddities of Rhino and Nashorn this runner takes care of
 * so that at least the standard "JavaScript" language works inside of OSGi.
 * <br>
 * In order to execute a script use:
 * <pre>
 * ScriptRunner runner = ScriptRunner.create(ServiceClass.class.getClassLoader(), "JavaScript", "callFooBar();" );
 * runner.run();
 * </pre>
 */
public abstract class ScriptRunner {

    private static class EvalScriptRunner extends ScriptRunner {

        private final ClassLoader classLoader;
        private final ScriptEngine engine;
        private final String script;

        public EvalScriptRunner(final ClassLoader classLoader, final ScriptEngine engine, final String script) {
            this.classLoader = classLoader;
            this.engine = engine;
            this.script = script;
        }

        @Override
        public Object run() throws ScriptException {
            return runWithClassLoader(this.classLoader, new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    return EvalScriptRunner.this.engine.eval(EvalScriptRunner.this.script);
                }
            });
        }

        @Override
        public Object run(final Bindings bindings) throws ScriptException {
            return runWithClassLoader(this.classLoader, new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    return EvalScriptRunner.this.engine.eval(EvalScriptRunner.this.script, bindings);
                }
            });
        }

        @Override
        public Object run(final ScriptContext context) throws ScriptException {
            return runWithClassLoader(this.classLoader, new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    return EvalScriptRunner.this.engine.eval(EvalScriptRunner.this.script, context);
                }
            });
        }
    }

    private ScriptRunner() {
    }

    public abstract Object run() throws ScriptException;

    public abstract Object run(Bindings bindings) throws ScriptException;

    public abstract Object run(ScriptContext context) throws ScriptException;

    public static ScriptRunner create(final ClassLoader classLoader, final String scriptEngineName, final String script)
            throws ScriptException {

        final ScriptEngine engine = createEngine(classLoader, scriptEngineName);
        return new EvalScriptRunner(classLoader, engine, script);

    }

    private static ScriptEngineManager createManager(final ClassLoader classLoader) throws ScriptException {
        return runWithClassLoader(classLoader, new Callable<ScriptEngineManager>() {

            @Override
            public ScriptEngineManager call() throws Exception {
                // passing null here since this will trigger
                // the script engine lookup to use java basic
                // support for JavaScript
                return new ScriptEngineManager(null);
            }
        });
    }

    private static ScriptEngine createEngine(final ClassLoader classLoader, final ScriptEngineManager manager,
            final String engineName) throws ScriptException {
        Objects.requireNonNull(manager);
        Objects.requireNonNull(engineName);

        return runWithClassLoader(classLoader, new Callable<ScriptEngine>() {

            @Override
            public ScriptEngine call() throws Exception {
                return manager.getEngineByName(engineName);
            }
        });
    }

    private static ScriptEngine createEngine(final ClassLoader classLoader, final String engineName)
            throws ScriptException {
        return createEngine(classLoader, createManager(classLoader), engineName);
    }

    /**
     * Run a Callable while swapping the context class loader
     *
     * @param classLoader
     *            the class loader to set while calling the code
     * @param code
     *            the code to call
     * @return the return value of the code
     * @throws ScriptException
     *             if anything goes wrong
     */
    public static <T> T runWithClassLoader(final ClassLoader classLoader, final Callable<T> code)
            throws ScriptException {
        if (classLoader == null) {
            try {
                return code.call();
            } catch (ScriptException e) {
                throw e;
            } catch (Exception e) {
                throw new ScriptException(e);
            }
        }

        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return code.call();
        } catch (ScriptException e) {
            throw e;
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }
}
