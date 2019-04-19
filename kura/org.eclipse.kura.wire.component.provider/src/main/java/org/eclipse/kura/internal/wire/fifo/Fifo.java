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
package org.eclipse.kura.internal.wire.fifo;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

public class Fifo implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final String DISCARD_ENVELOPES_PROP_NAME = "discard.envelopes";
    private static final String QUEUE_CAPACITY_PROP_NAME = "queue.capacity";

    private static final Logger logger = LogManager.getLogger(Fifo.class);

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private FifoEmitterThread emitterThread;

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

    @SuppressWarnings("unchecked")
    public void activate(final Map<String, Object> properties, ComponentContext componentContext) {
        logger.info("Activating Fifo...");
        wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        updated(properties);
        logger.info("Activating Fifo... Done");
    }

    public void deactivate() {
        logger.info("Dectivating Fifo...");
        stopEmitterThread();
        logger.info("Dectivating Fifo... Done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("Updating Fifo...");

        String threadName = (String) properties.getOrDefault(ConfigurationService.KURA_SERVICE_PID, "Fifo")
                + "-EmitterThread";
        int queueCapacity = (Integer) properties.getOrDefault(QUEUE_CAPACITY_PROP_NAME, 50);
        boolean discardEnvelopes = (Boolean) properties.getOrDefault(DISCARD_ENVELOPES_PROP_NAME, false);

        restartEmitterThread(threadName, queueCapacity, discardEnvelopes);

        logger.info("Updating Fifo... Done");
    }

    private synchronized void stopEmitterThread() {
        if (emitterThread != null) {
            emitterThread.shutdown();
            emitterThread = null;
        }
    }

    private synchronized void restartEmitterThread(String threadName, int queueCapacity, boolean discardEnvelopes) {
        stopEmitterThread();

        logger.debug("Creating new emitter thread: {}, queue capacity: {}, discard envelopes: {}", threadName,
                queueCapacity, discardEnvelopes);
        emitterThread = new FifoEmitterThread(threadName, queueCapacity, discardEnvelopes);
        emitterThread.start();
    }

    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");
        if (emitterThread != null) {
            emitterThread.submit(wireEnvelope);
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

    private class FifoEmitterThread extends Thread {

        private boolean discardEnvelopes;
        private AtomicBoolean run = new AtomicBoolean(true);
        private ArrayBlockingQueue<WireEnvelope> queue;

        private Consumer<WireEnvelope> submitter;

        public FifoEmitterThread(String threadName, int queueCapacity, boolean discardEnvelopes) {
            this.queue = new ArrayBlockingQueue<>(queueCapacity, true);
            this.discardEnvelopes = discardEnvelopes;
            setName(threadName);
            if (discardEnvelopes) {
                submitter = getEnvelopeDiscardingSubmitter();
            } else {
                submitter = getEmitterBlockingSubmitter();
            }
        }

        private Consumer<WireEnvelope> getEnvelopeDiscardingSubmitter() {
            return envelope -> {
                if (!run.get()) {
                    return;
                }
                if (!queue.offer(envelope))
                    logger.debug("envelope discarded");
                else
                    logger.debug("envelope submitted");
            };
        }

        private Consumer<WireEnvelope> getEmitterBlockingSubmitter() {
            return envelope -> {
                try {
                    if (!run.get()) {
                        return;
                    }
                    queue.put(envelope);
                    logger.debug("envelope submitted");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while adding new envelope to queue", e);
                }
            };
        }

        public void shutdown() {
            run.set(false);
            Thread.currentThread().interrupt();
        }

        public void submit(WireEnvelope envelope) {
            submitter.accept(envelope);
        }

        @Override
        public void run() {
            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    WireEnvelope next = null;
                    if (!run.get()) {
                        break;
                    }
                    if (discardEnvelopes)
                        next = queue.poll();
                    else
                        next = queue.take();
                    if (next != null)
                        wireSupport.emit(next.getRecords());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.warn("Unexpected exception while dispatching envelope", e);
                }
            }
            logger.debug("exiting");
        }
    }
}
