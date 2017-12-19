/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.conditional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.EmitterPort;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.LoggerFactory;

/**
 * The Class Logger is the specific Wire Component to log a list of {@link WireRecord}s
 * as received in {@link WireEnvelope}
 */
public final class Conditional implements WireReceiver, WireEmitter, ConfigurableComponent {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Conditional.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private volatile WireHelperService wireHelperService;

    private MultiportWireSupport wireSupport;

    private EmitterPort thenPort;
    private EmitterPort elsePort;

    private static final String LANGUAGE = "javascript";
    private ScriptEngine scriptEngine;
    private Bindings bindings;

    private ConditionalOptions conditionalOptions;

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug(message.activatingLogger());
        this.conditionalOptions = new ConditionalOptions(properties);

        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this);
        final List<EmitterPort> emitterPorts = this.wireSupport.getEmitterPorts();
        this.thenPort = emitterPorts.get(0);
        this.elsePort = emitterPorts.get(1);

        ScriptEngineManager engineManager = new ScriptEngineManager(null);
        this.scriptEngine = engineManager.getEngineByName(LANGUAGE);

        if (this.scriptEngine == null) {
            throw new ComponentException("Error Getting Conditional Script Engine");
        }

        this.bindings = this.scriptEngine.createBindings();
        this.bindings.put("logger", logger);

        logger.debug(message.activatingLoggerDone());
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingLogger());
        this.conditionalOptions = new ConditionalOptions(properties);
        logger.debug(message.updatingLoggerDone());
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingLogger());
        // remained for debugging purposes
        logger.debug(message.deactivatingLoggerDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());

        ConditionalScriptInterface scriptInterface = new ConditionalScriptInterface(wireEnvelope);
        this.bindings.put("wire", scriptInterface);
        Boolean decision;
        try {
            CompiledScript compiledBooleanExpression = this.conditionalOptions
                    .getCompiledBooleanExpression(this.scriptEngine);
            decision = (Boolean) compiledBooleanExpression.eval(this.bindings);

            final List<WireRecord> newWireRecords = new ArrayList<>();
            newWireRecords.addAll(wireEnvelope.getRecords());
            if (decision) {
                this.thenPort.emit(this.wireSupport.createWireEnvelope(newWireRecords));
            } else {
                this.elsePort.emit(this.wireSupport.createWireEnvelope(newWireRecords));
            }
        } catch (ScriptException e) {

        }
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        requireNonNull(wires, message.wiresNonNull());
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