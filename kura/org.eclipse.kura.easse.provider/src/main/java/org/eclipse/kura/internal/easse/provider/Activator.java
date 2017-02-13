/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.easse.provider;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Activator instance for tracking HTTP Service to register Event Source Servlet
 */
public final class Activator implements BundleActivator {

    /** Logger Instance */
    private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);

    /** OSGi HTTP Service Tracker */
    private ServiceTracker<HttpService, HttpService> httpTracker;

    /** {@inheritDoc} */
    @Override
    public void start(final BundleContext context) throws Exception {
        this.httpTracker = new ServiceTracker<HttpService, HttpService>(context, HttpService.class, null) {

            /** {@inheritDoc} */
            @Override
            public HttpService addingService(final ServiceReference<HttpService> reference) {
                // HTTP service is available, register Event Source servlet
                final HttpService httpService = this.context.getService(reference);
                try {
                    httpService.registerServlet("/sse", new SseEventSourceServlet(), null, null);
                } catch (final Exception exception) {
                    LOG.error("Event Source servlet registration failed...", exception);
                }
                return httpService;
            }

            /** {@inheritDoc} */
            @Override
            public void removedService(final ServiceReference<HttpService> reference, final HttpService service) {
                // HTTP service is not available, unregister Event Source servlet
                try {
                    service.unregister("/sse");
                } catch (final IllegalArgumentException exception) {
                    // servlet registration failed
                }
            }
        };
        // start tracking all HTTP services...
        this.httpTracker.open();
    }

    /** {@inheritDoc} */
    @Override
    public void stop(final BundleContext context) throws Exception {
        // stop tracking all HTTP services...
        this.httpTracker.close();
    }
}