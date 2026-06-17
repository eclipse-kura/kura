/*******************************************************************************
 * Copyright (c) 2020, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.util.wire.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.LoggerFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
@Component(
    name = "org.eclipse.kura.util.wire.test.TestEmitterReceiver",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { org.eclipse.kura.configuration.ConfigurableComponent.class,
            org.eclipse.kura.wire.WireComponent.class,
            org.osgi.service.wireadmin.Consumer.class,
            org.osgi.service.wireadmin.Producer.class },
    property = {
        "service.pid=com.eurotech.framework.wire.AvroSerializer",
        "kura.ui.service.hide:Boolean=true",
        "input.cardinality.minimum:Integer=1",
        "input.cardinality.maximum:Integer=1",
        "input.cardinality.default:Integer=1",
        "output.cardinality.minimum:Integer=1",
        "output.cardinality.maximum:Integer=1",
        "output.cardinality.default:Integer=1" })
@Designate(ocd = TestEmitterReceiverOptions.class, factory = true)
public class TestEmitterReceiver implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestEmitterReceiver.class);

    private WireHelperService wireHelperService;
    private MultiportWireSupport wireSupport;

    private Consumer<WireEnvelope> consumer;

    @Activate
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("activating...");
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                getWireComponentServiceReference(componentContext));
        logger.info("activating...done");
    }

    @Modified
    public void updated(final Map<String, Object> properties) {
        logger.info("updating...");
        logger.info("updating...done");
    }

    @Reference(name = "WireHelperService", service = org.eclipse.kura.wire.WireHelperService.class, unbind = "-")
    protected void bindWireHelperService(WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    @SuppressWarnings("unchecked")
    private ServiceReference<WireComponent> getWireComponentServiceReference(ComponentContext componentContext) {
        return (ServiceReference<WireComponent>) componentContext.getServiceReference();
    }

    @Override
    public void consumersConnected(Wire[] arg0) {
        this.wireSupport.consumersConnected(arg0);
    }

    @Override
    public Object polled(Wire arg0) {
        return this.wireSupport.polled(arg0);
    }

    @Override
    public void producersConnected(Wire[] arg0) {
        this.wireSupport.producersConnected(arg0);
    }

    @Override
    public void updated(Wire arg0, Object arg1) {
        this.wireSupport.updated(arg0, arg1);
    }

    @Override
    public void onWireReceive(WireEnvelope arg0) {
        this.consumer.accept(arg0);
    }

    public MultiportWireSupport getWireSupport() {
        return this.wireSupport;
    }

    public void setConsumer(Consumer<WireEnvelope> consumer) {
        this.consumer = consumer;
    }

    public CompletableFuture<WireEnvelope> nextEnvelope() {
        final CompletableFuture<WireEnvelope> result = new CompletableFuture<>();

        setConsumer(result::complete);

        return result;
    }

    public void emit(final WireEnvelope envelope, final int port) {
        this.wireSupport.getEmitterPorts().get(port).emit(envelope);
    }

    public void emit(final WireEnvelope envelope) {
        this.emit(envelope, 0);
    }

    public void emit(final List<WireRecord> records, final int port) {
        this.emit(this.wireSupport.createWireEnvelope(records), port);
    }

    public void emit(final List<WireRecord> records) {
        this.emit(records, 0);
    }

    public void emit(final WireRecord record, final int port) {
        this.emit(Collections.singletonList(record), port);
    }

    public void emit(final WireRecord record) {
        this.emit(record, 0);
    }

    public void emit(final Map<String, TypedValue<?>> recordProperties, final int port) {
        this.emit(Collections.singletonList(new WireRecord(recordProperties)), port);
    }

    public void emit(final Map<String, TypedValue<?>> recordProperties) {
        this.emit(recordProperties, 0);
    }

    public void emit(final String propertyKey, final TypedValue<?> value, final int port) {
        this.emit(Collections.singletonMap(propertyKey, value), port);
    }

    public void emit(final String propertyKey, final TypedValue<?> value) {
        this.emit(propertyKey, value, 0);
    }

    public void emit(final int port) {
        this.emit(Collections.emptyMap(), port);
    }

    public void emit() {
        this.emit(0);
    }
}
