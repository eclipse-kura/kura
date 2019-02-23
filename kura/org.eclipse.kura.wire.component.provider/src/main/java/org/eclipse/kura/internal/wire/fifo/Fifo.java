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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

        private Lock lock = new ReentrantLock();

        private Condition producer = lock.newCondition();
        private Condition consumer = lock.newCondition();

        private boolean run = true;
        private ArrayList<WireEnvelope> queue;
        private int queueCapacity;

        private Consumer<WireEnvelope> submitter;

        public FifoEmitterThread(String threadName, int queueCapacity, boolean discardEnvelopes) {
            this.queue = new ArrayList<>();
            this.queueCapacity = queueCapacity;
            setName(threadName);
            if (discardEnvelopes) {
                submitter = getEnvelopeDiscardingSubmitter();
            } else {
                submitter = getEmitterBlockingSubmitter();
            }
        }

        private Consumer<WireEnvelope> getEnvelopeDiscardingSubmitter() {
            return (envelope) -> {
                try {
                    lock.lock();
                    if (!run || queue.size() >= queueCapacity) {
                        logger.debug("envelope discarded");
                        return;
                    } else {
                        queue.add(envelope);
                        producer.signal();
                        logger.debug("envelope submitted");
                    }
                } finally {
                    lock.unlock();
                }
            };
        }

        private Consumer<WireEnvelope> getEmitterBlockingSubmitter() {
            return (envelope) -> {
                try {
                    lock.lock();
                    while (run && queue.size() >= queueCapacity) {
                        consumer.await();
                    }
                    if (!run) {
                        return;
                    }
                    queue.add(envelope);
                    producer.signal();
                    logger.debug("envelope submitted");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while adding new envelope to queue", e);
                } finally {
                    lock.unlock();
                }
            };
        }

        public void shutdown() {
            try {
                lock.lock();
                run = false;
                producer.signalAll();
                consumer.signalAll();
            } finally {
                lock.unlock();
            }
        }

        public void submit(WireEnvelope envelope) {
            submitter.accept(envelope);
        }

        @Override
        public void run() {
            while (run) {
                try {
                    WireEnvelope next = null;
                    try {
                        lock.lock();
                        while (run && queue.isEmpty()) {
                            producer.await();
                        }
                        if (!run) {
                            break;
                        }
                        next = queue.remove(0);
                        consumer.signal();
                    } finally {
                        lock.unlock();
                    }
                    wireSupport.emit(next.getRecords());
                } catch (Exception e) {
                    logger.warn("Unexpected exception while dispatching envelope", e);
                }
            }
            logger.debug("exiting");
        }
    }
}
