/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class Timer represents a Wire Component which triggers a ticking event on
 * every interval as configured. It fires the event on every tick.
 */
public class Timer implements WireEmitter, ConfigurableComponent {

    private static final Logger logger = LogManager.getLogger(Timer.class);

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    private Optional<TimerExecutor> timerExecutor = Optional.empty();

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext ctx, final Map<String, Object> properties) {
        logger.debug("Activating Timer...");

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) ctx.getServiceReference());

        doUpdate(new TimerOptions(properties));

        logger.debug("Activating Timer... Done");
    }

    protected void updated(final Map<String, Object> properties) {
        logger.debug("Updating Timer...");

        doUpdate(new TimerOptions(properties));

        logger.debug("Updating Timer... Done");
    }

    protected void deactivate() {
        logger.debug("Dectivating Timer...");

        shutdownExecutor();

        logger.debug("Dectivating Timer... Done");
    }

    private void doUpdate(final TimerOptions options) {

        shutdownExecutor();

        try {
            if ("SIMPLE".equalsIgnoreCase(options.getType())) {
                this.timerExecutor = Optional.of(new SimpleTimerExecutor(options, this.wireSupport));
            } else {
                this.timerExecutor = Optional.of(new CronTimerExecutor(options, this.wireSupport));
            }
        } catch (final Exception e) {
            logger.warn("failed to start timer", e);
        }
    }

    private void shutdownExecutor() {
        if (this.timerExecutor.isPresent()) {
            this.timerExecutor.get().shutdown();
            this.timerExecutor = Optional.empty();
        }
    }

    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    static void emit(final WireSupport wireSupport) {

        final TypedValue<Long> timestamp = TypedValues.newLongValue(System.currentTimeMillis());

        final WireRecord timerWireRecord = new WireRecord(Collections.singletonMap("TIMER", timestamp));

        wireSupport.emit(Collections.singletonList(timerWireRecord));
    }

}
