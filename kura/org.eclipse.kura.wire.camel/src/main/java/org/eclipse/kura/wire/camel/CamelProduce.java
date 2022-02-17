/*******************************************************************************
 * Copyright (c) 2018, 2022 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  heyoulin <heyoulin@gmail.com>
 *******************************************************************************/
package org.eclipse.kura.wire.camel;

import static org.apache.camel.builder.DefaultFluentProducerTemplate.on;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.eclipse.kura.wire.WireEnvelope;

public class CamelProduce extends AbstractReceiverWireComponent {

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private FluentProducerTemplate template = null;

    private volatile boolean templateAvailable = false;

    @Override
    protected void processReceive(final CamelContext context, final String endpointUri, final WireEnvelope envelope)
            throws Exception {

        checkTemplate(context);

        if (!templateAvailable) {
            createTemplate(context);
        }
        template //
                .withBody(envelope) //
                .to(endpointUri) //
                .asyncSend();
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        final Lock wlock = this.rwLock.writeLock();
        wlock.lock();
        try {
            closeTemplate();
        } finally {
            wlock.unlock();
        }
    }

    private void checkTemplate(final CamelContext context) {
        final Lock rlock = this.rwLock.readLock();
        rlock.lock();
        try {
            templateAvailable = template != null && template.getCamelContext() == context;
        } finally {
            rlock.unlock();
        }
    }

    private void createTemplate(final CamelContext context) {
        final Lock wlock = this.rwLock.writeLock();
        wlock.lock();
        try {
            closeTemplate();
            template = on(context);
        } finally {
            wlock.unlock();
        }
    }

    private void closeTemplate() {
        if (template != null) {
            try {
                template.stop();
            } catch (Exception ignored) {
            } finally {
                template = null;
            }
        }
    }

}
