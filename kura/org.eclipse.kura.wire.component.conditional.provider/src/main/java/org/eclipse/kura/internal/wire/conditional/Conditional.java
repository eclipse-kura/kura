/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.conditional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.EmitterPort;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * The Class Conditional is a specific Wire Component to apply a condition
 * on the received {@link WireEnvelope}
 */
public final class Conditional implements WireReceiver, WireEmitter, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(Conditional.class);

    private volatile WireHelperService wireHelperService;

    private MultiportWireSupport wireSupport;

    private EmitterPort thenPort;
    private EmitterPort elsePort;

    private ScriptEngine scriptEngine;
    private Bindings bindings;

    private ConditionalOptions conditionalOptions;
    private Optional<CompiledScript> script = Optional.empty();

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("Activating Conditional component...");
        this.conditionalOptions = new ConditionalOptions(properties);

        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        final List<EmitterPort> emitterPorts = this.wireSupport.getEmitterPorts();
        this.thenPort = emitterPorts.get(0);
        this.elsePort = emitterPorts.get(1);

        this.scriptEngine = createEngine();
        this.bindings = createBindings();

        if (this.scriptEngine == null) {
            throw new ComponentException("Error Getting Conditional Script Engine");
        }

        updated(properties);
        logger.info("Activating Conditional component...done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating Conditional component...");
        this.conditionalOptions = new ConditionalOptions(properties);
        try {
            this.script = Optional.of(tryCompileScript(this.conditionalOptions.getBooleanExpression()));
        } catch (Exception e) {
            logger.warn("Failed to compile boolean expression", e);
            this.script = Optional.empty();
        }
        logger.info("Updating Conditional component...done");
    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info("Deactivating Conditional component...");
        // remained for debugging purposes
        logger.info("Deactivating Conditional component...done");
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        try {

            if (!this.script.isPresent()) {
                logger.warn(
                        "The script compilation failed during component configuration update, please review the script.");
                return;
            }

            final List<WireRecord> inputRecords = wireEnvelope.getRecords();

            final WireRecordListWrapper wireRecordList = new WireRecordListWrapper(inputRecords);
            final String emitterPid = wireEnvelope.getEmitterPid();

            this.bindings.put("input", new WireEnvelopeWrapper(wireRecordList, emitterPid));
            this.bindings.put("records", wireRecordList);
            this.bindings.put("emitterPid", emitterPid);

            final Object decision = this.script.get().eval(this.bindings);

            if (!(decision instanceof Boolean)) {
                logger.warn("Expression result is not a boolean: {}", decision);
                return;
            }

            final WireEnvelope outputEnvelope = this.wireSupport.createWireEnvelope(inputRecords);

            if ((Boolean) decision) {
                this.thenPort.emit(outputEnvelope);
            } else {
                this.elsePort.emit(outputEnvelope);
            }
        } catch (Exception e) {
            logger.warn("Exception while performing decision.", e);
        }
    }

    private CompiledScript tryCompileScript(final String script) throws ScriptException {
        final Compilable engine = ((Compilable) this.scriptEngine);
        return engine.compile(script);
    }

    private ScriptEngine createEngine() {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        ScriptEngine scriptEngine = factory.getScriptEngine(className -> false);

        if (scriptEngine == null) {
            throw new IllegalStateException("Failed to create script engine");
        }

        final Bindings engineScopeBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineScopeBindings != null) {
            engineScopeBindings.remove("exit");
            engineScopeBindings.remove("quit");
        }

        final Bindings globalScopeBindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalScopeBindings != null) {
            globalScopeBindings.remove("exit");
            globalScopeBindings.remove("quit");
        }

        return scriptEngine;
    }

    private Bindings createBindings() {
        Bindings bindings = this.scriptEngine.createBindings();

        bindings.put("logger", logger);

        bindings.remove("exit");
        bindings.remove("quit");

        return bindings;
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }
}