/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others 
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
package org.eclipse.kura.wire.script.tools.conditional.component;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.graph.EmitterPort;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.eclipse.kura.wire.script.tools.EngineProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Conditional is a specific Wire Component to apply a condition
 * on the received {@link WireEnvelope}
 */
public final class ConditionalComponent extends EngineProvider
        implements WireReceiver, WireEmitter, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalComponent.class);

    private Optional<String> booleanExpression = Optional.empty();

    private WireHelperService wireHelperService;

    private MultiportWireSupport wireSupport;

    private EmitterPort thenPort;
    private EmitterPort elsePort;

    private ConditionalComponentOptions conditionalOptions;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    @SuppressWarnings("unchecked")
    public void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("Activating Conditional Component...");
        this.conditionalOptions = new ConditionalComponentOptions(properties);

        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        final List<EmitterPort> emitterPorts = this.wireSupport.getEmitterPorts();
        this.thenPort = emitterPorts.get(0);
        this.elsePort = emitterPorts.get(1);

        updated(properties);

        logger.info("Activating Conditional Component... Done.");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating Conditional Component...");

        this.conditionalOptions = new ConditionalComponentOptions(properties);

        initEngine();

        this.booleanExpression = this.conditionalOptions.getBooleanExpression();

        logger.info("Updating Conditional Component... Done");
    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info("Deactivating Conditional Component...");
        closeEngine();
        logger.info("Deactivating Conditional Component... Done.");
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        evaluateScriptAndEmitOutput(wireEnvelope);

    }

    private void evaluateScriptAndEmitOutput(WireEnvelope wireEnvelope) {

        if (!this.booleanExpression.isPresent() || this.booleanExpression.get().isEmpty()) {
            logger.warn("No source specified! Ignoring received WireEnvelope.");
            return;
        }

        addBinding("input", wireEnvelope);

        evaluate(this.booleanExpression.get());

        Optional<TypedValue<Boolean>> result = getResultAsBoolean();

        if (!result.isPresent()) {
            logger.error(
                    "Failed to execute conditional logic. Ether no result was produced, or the resulting datatype was not a boolean.");
            return;
        }

        final WireEnvelope outputEnvelope = this.wireSupport.createWireEnvelope(wireEnvelope.getRecords());

        if ((boolean) result.get().getValue()) {
            this.thenPort.emit(outputEnvelope);
        } else {
            this.elsePort.emit(outputEnvelope);
        }
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