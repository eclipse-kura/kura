/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.script.filter.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class ScriptFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ScriptFilter.class);

    private static final String SCRIPT_PROPERTY_KEY = "script";
    private static final String SCRIPT_CONTEXT_DROP_PROPERTY_KEY = "script.context.drop";

    private CompiledScript script;
    private Bindings bindings;

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private ScriptEngine scriptEngine;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    public void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws ComponentException {
        logger.info("Activating Script Filter...");
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        this.scriptEngine = createEngine();
        this.bindings = createBindings();

        updated(properties);

        logger.info("ActivatingScript Filter... Done");
    }

    public void deactivate() {
        logger.info("Deactivating Script Filter...");
        logger.info("Deactivating Script Filter... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating Script Filter...");

        final String scriptSource = (String) properties.get(SCRIPT_PROPERTY_KEY);

        if (scriptSource == null) {
            logger.warn("Script source is null");
            return;
        }

        this.script = null;
        try {
            this.script = ((Compilable) this.scriptEngine).compile(scriptSource);
        } catch (ScriptException e) {
            logger.warn("Failed to compile script", e);
        }

        if (this.bindings == null || (Boolean) properties.getOrDefault(SCRIPT_CONTEXT_DROP_PROPERTY_KEY, false)) {
            this.bindings = createBindings();
        }

        logger.info("Updating Script Filter... Done");
    }

    @Override
    public synchronized void onWireReceive(WireEnvelope wireEnvelope) {
        if (this.script == null) {
            logger.warn("Failed to compile script");
            return;
        }

        try {
            final WireEnvelopeWrapper inputEnvelopeWrapper = new WireEnvelopeWrapper(
                    new WireRecordListWrapper(wireEnvelope.getRecords()), wireEnvelope.getEmitterPid());
            final OutputWireRecordListWrapper outputEnvelopeWrapper = new OutputWireRecordListWrapper();

            this.bindings.put("input", inputEnvelopeWrapper);
            this.bindings.put("output", outputEnvelopeWrapper);

            this.script.eval(this.bindings);

            final List<WireRecord> result = outputEnvelopeWrapper.getRecords();

            if (result != null) {
                this.wireSupport.emit(result);
            }
        } catch (Exception e) {
            logger.warn("Failed to execute script", e);
        }
    }

    private ScriptEngine createEngine() {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        ScriptEngine scriptEngine = factory.getScriptEngine(className -> false);

        if (scriptEngine == null) {
            throw new IllegalStateException("Failed to get script engine");
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

        bindings.put("newWireRecord", (Supplier<WireRecordWrapper>) WireRecordWrapper::new);

        bindings.put("newBooleanValue", (Function<Boolean, TypedValue<?>>) TypedValues::newBooleanValue);
        bindings.put("newByteArrayValue", (Function<byte[], TypedValue<?>>) TypedValues::newByteArrayValue);
        bindings.put("newDoubleValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newDoubleValue(num.doubleValue()));
        bindings.put("newFloatValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newFloatValue(num.floatValue()));
        bindings.put("newIntegerValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newIntegerValue(num.intValue()));
        bindings.put("newLongValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newLongValue(num.longValue()));
        bindings.put("newStringValue",
                (Function<Object, TypedValue<?>>) obj -> TypedValues.newStringValue(obj.toString()));

        bindings.put("newByteArray", (Function<Integer, byte[]>) size -> new byte[size]);

        for (DataType type : DataType.values()) {
            bindings.put(type.name(), type);
        }

        bindings.remove("exit");
        bindings.remove("quit");

        return bindings;
    }

    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(Wire wire, Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }
}
