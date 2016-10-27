/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * The Class EventHandlerServlet is responsible for interacting between Event
 * Admin and JS through Server Sent Events. This is mainly required for Kura
 * Wires to delegate the emit events.
 */
public final class EventHandlerServlet extends HttpServlet {

    /** Serial Version */
    private static final long serialVersionUID = -8962416452919656283L;

    /**
     * Tracks all the changes associating a flag which signifies if the item has
     * been read by JavaScript
     */
    private final Queue<String> m_sharedCache;

    /**
     * Instantiates a new event handler servlet.
     */
    public EventHandlerServlet() {
        this.m_sharedCache = new ConcurrentLinkedQueue<String>();
        final Dictionary<String, Object> map = new Hashtable<String, Object>();
        map.put(EventConstants.EVENT_TOPIC, WireSupport.EMIT_EVENT_TOPIC);
        final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(EventHandler.class, new EventHandler() {

            /** {@inheritDoc} */
            @Override
            public void handleEvent(final Event event) {
                synchronized (bundleContext) {
                    final String eventData = String.valueOf(event.getProperty("emitter"));
                    EventHandlerServlet.this.m_sharedCache.add(eventData);
                }
            }
        }, map);
    }

    /**
     * Performs a GET request for Server Sent Event Value.
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @throws ServletException
     *             the servlet exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        final PrintWriter writer = response.getWriter();
        while (!this.m_sharedCache.isEmpty()) {
            writer.write("data: " + this.m_sharedCache.poll() + "\n\n");
        }
        writer.flush();
        writer.close();
    }

}