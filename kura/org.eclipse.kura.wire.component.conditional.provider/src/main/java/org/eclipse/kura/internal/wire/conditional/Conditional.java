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
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.wire.conditional.LoggingVerbosity.QUIET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.EmitterPort;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.LoggerFactory;

/**
 * The Class Logger is the specific Wire Component to log a list of {@link WireRecord}s
 * as received in {@link WireEnvelope}
 */
public final class Conditional implements WireReceiver, WireEmitter, ConfigurableComponent {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Conditional.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static final String DEFAULT_LOG_LEVEL = QUIET.name();

    private static final String PROP_LOG_LEVEL = "log.verbosity";

    private volatile WireHelperService wireHelperService;

    private MultiportWireSupport wireSupport;

    private Map<String, Object> properties;

    private EmitterPort thenPort;
    private EmitterPort elsePort;

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
        this.properties = properties;
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this);
        final List<EmitterPort> emitterPorts = this.wireSupport.getEmitterPorts();
        this.thenPort = emitterPorts.get(0);
        this.elsePort = emitterPorts.get(1);
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
        this.properties = properties;
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

        final List<WireRecord> thenRecords = new ArrayList<>();
        final Map<String, TypedValue<?>> thenProperties = new HashMap<>();
        thenProperties.put("port", new StringValue("then"));
        thenRecords.add(new WireRecord(thenProperties));
        // this.thenPort.emit(wireSupport.createWireEnvelope(thenRecords));

        final List<WireRecord> elseRecords = new ArrayList<>();
        final Map<String, TypedValue<?>> elseProperties = new HashMap<>();
        elseProperties.put("port", new StringValue("else"));
        elseRecords.add(new WireRecord(elseProperties));
        this.elsePort.emit(wireSupport.createWireEnvelope(elseRecords));

    }

    private String getLoggingLevel() {
        String logLevel = DEFAULT_LOG_LEVEL;
        final Object configuredLogLevel = this.properties.get(PROP_LOG_LEVEL);
        if (nonNull(configuredLogLevel) && configuredLogLevel instanceof String) {
            logLevel = String.valueOf(configuredLogLevel);
        }
        return logLevel;
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