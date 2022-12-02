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
package org.eclipse.kura.wire.script.tools.filter.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.script.tools.EngineProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterComponent extends EngineProvider implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(FilterComponent.class);

    private Optional<String> sourceScript = Optional.empty();

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    @SuppressWarnings("unchecked")
    public void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws ComponentException {
        logger.info("Activating Filter Component...");

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        updated(properties);

        logger.info("Activating Filter Component... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating Filter Component...");
        closeEngine();
        logger.info("Deactivating Filter Component... Done.");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating Filter Component...");

        FilterComponentOptions options = new FilterComponentOptions(properties);

        this.sourceScript = options.getScriptSource();

        if (options.isScriptContextDrop() || !isEngineInit()) {
            initEngine();
        }

        logger.info("Updating Filter Component... Done");
    }

    @Override
    public synchronized void onWireReceive(WireEnvelope wireEnvelope) {
        if (!this.sourceScript.isPresent()) {
            logger.warn("No source specified! Ignoring received WireEnvelope.");
            return;
        }

        addBinding("input", wireEnvelope);
        evaluate(this.sourceScript.get());
        Optional<List<WireRecord>> recordsToEmit = getBindingAsWireRecordList("output");

        if (recordsToEmit.isPresent()) {
            this.wireSupport.emit(recordsToEmit.get());
        }
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
