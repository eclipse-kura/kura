/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.ssl;

import org.eclipse.kura.ssl.SslServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SslServiceListeners implements SslServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(SslServiceListeners.class);

    private final ServiceTracker<SslServiceListener, SslServiceListener> listenersTracker;

    public SslServiceListeners(ServiceTracker<SslServiceListener, SslServiceListener> listenersTracker) {
        super();
        this.listenersTracker = listenersTracker;
    }

    @Override
    public void onConfigurationUpdated() {
        openOnce();

        Object[] listeners = this.listenersTracker.getServices();
        if (listeners != null && listeners.length != 0) {
            for (Object listener : listeners) {
                try {
                    ((SslServiceListener) listener).onConfigurationUpdated();
                } catch (Throwable t) {
                    logger.error("Unexpected Throwable", t);
                }
            }
        }
    }

    public synchronized void close() {
        if (this.listenersTracker.getTrackingCount() != -1) {
            this.listenersTracker.close();
        }
    }

    private synchronized void openOnce() {
        if (this.listenersTracker.getTrackingCount() == -1) {
            this.listenersTracker.open();
        }
    }

}
