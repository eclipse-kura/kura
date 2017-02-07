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
package org.eclipse.kura.internal.wire.logger;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.wire.logger.LoggingVerbosity.VERBOSE;
import static org.eclipse.kura.internal.wire.logger.LoggingVerbosity.QUIET;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.LoggerFactory;

/**
 * The Class Logger is the specific Wire Component to log a list of {@link WireRecord}s
 * as received in {@link WireEnvelope}
 */
public final class Logger implements WireReceiver, ConfigurableComponent {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static final String DEFAULT_LOG_LEVEL = QUIET.name();

    private static final String PROP_LOG_LEVEL = "log.verbosity";

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private Map<String, Object> properties;

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
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
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(message.activatingLogger());
        this.properties = properties;
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        logger.debug(message.activatingLoggerDone());
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
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
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingLogger());
        // remained for debugging purposes
        logger.debug(message.deactivatingLoggerDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.info(message.wireEnvelopeReceived(wireEnvelope.getEmitterPid()));

        if (VERBOSE.name().equals(getLoggingLevel())) {
            logger.info("Record List content: ");
            for (WireRecord record : wireEnvelope.getRecords()) {
                logger.info("  Record content: ");

                for (Entry<String, TypedValue<?>> entry : record.getProperties().entrySet()) {
                    logger.info("    {} : {}", entry.getKey(), entry.getValue().getValue());
                }
            }
            logger.info("");
        }
    }

    private String getLoggingLevel() {
        String logLevel = DEFAULT_LOG_LEVEL;
        final Object configuredLogLevel = this.properties.get(PROP_LOG_LEVEL);
        if (configuredLogLevel != null && configuredLogLevel instanceof String) {
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
}