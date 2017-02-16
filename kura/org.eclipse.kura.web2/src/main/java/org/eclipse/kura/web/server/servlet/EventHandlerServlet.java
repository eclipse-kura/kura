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
package org.eclipse.kura.web.server.servlet;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.util.base.StringUtil.isNullOrEmpty;
import static org.eclipse.kura.wire.WireSupport.EMIT_EVENT_TOPIC;
import static org.osgi.service.event.EventConstants.EVENT_TOPIC;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class EventHandlerServlet is responsible for interacting between Event
 * Admin and Javascript through Server Sent Events (SSE). This is mainly required
 * for Kura Wires to delegate the emit events.
 */
public final class EventHandlerServlet extends HttpServlet {

    /** The Logger Instance. */
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerServlet.class);

    /** Session Timeout in Seconds - 5 minutes */
    private static final int MAX_INACTIVE_INTERVAL = 5 * 60;

    /**
     * Maximum size of the queue to maintain the events. The queue does not care if any
     * event maintained by the queue is rejected. As long as a consumer thread can consume
     * events from the queue, the system behavior is considered as expected.
     */
    private static final int MAX_SIZE_OF_QUEUE = 10;

    /** Serial Version */
    private static final long serialVersionUID = -8962416452919656283L;

    /** Bundle Context */
    private BundleContext bundleContext;

    /** Used to track the new sessions */
    private Map<String, HttpSession> requests;

    /** {@inheritDoc} */
    @Override
    public void init() throws ServletException {
        super.init();
        this.bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        this.requests = new ConcurrentHashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        super.destroy();
        this.requests.clear();
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
        // track the new session
        final String sessionId = request.getParameter("session");

        // set the response headers for SSE
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*"); // required for IE9

        final HttpSession session = request.getSession(false);
        // timeout is required if the browser beforeunload event is not fired in exceptional circumstances.
        // If there exists a session in which a browser is opened, the beforeunload event is expected to
        // be fired as soon as the browser or the tab is closed. But if by any chance, this beforeunload
        // event is not fired, the timeout of a session will notice the disconnection.
        session.setMaxInactiveInterval(MAX_INACTIVE_INTERVAL);

        if (!isNullOrEmpty(sessionId)) {
            this.requests.put(sessionId, session);
        }

        // destroy the session if exists. It actually destroys the current session which initiated
        // the destruction request
        final String sessionToDestroy = request.getParameter("logout");
        if (!isNullOrEmpty(sessionToDestroy) && this.requests.containsKey(sessionId)) {
            final HttpSession storedSession = this.requests.get(sessionId);
            storedSession.invalidate();
            this.requests.remove(sessionId);
            return;
        }

        final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>(MAX_SIZE_OF_QUEUE);
        final ServletOutputStream outputStream = response.getOutputStream();
        final PrintStream printStream = new PrintStream(outputStream);
        final AtomicReference<Closeable> closeableReference = new AtomicReference<>(outputStream);

        // the asynchronous task to retrieve and remove head element from the queue
        final CompletableFuture<Void> elementRemovalFuture = CompletableFuture.runAsync(() -> {
            try {
                boolean sessionExistsAndValid = checkSessionValidity(sessionId);
                // if session exists and valid, remove head element from the queue
                while (sessionExistsAndValid) {
                    final String data = eventQueue.poll(2, TimeUnit.SECONDS);
                    if (isNull(data)) {
                        printStream.print(":\n\n");
                    } else {
                        printStream.printf("data: %s%n%n", data);
                    }
                    printStream.flush();
                    sessionExistsAndValid = checkSessionValidity(sessionId);
                }
            } catch (final InterruptedException ex) {
                logger.warn("Element removal timeout...", ex);
                response.setStatus(HttpServletResponse.SC_OK);
            }
        });

        final ServiceRegistration<?> registration = registerEventHandler(EMIT_EVENT_TOPIC, eventQueue, sessionId,
                closeableReference, elementRemovalFuture);
        // unregisters event handler service and close the servlet output stream
        final CompletableFuture<Void> cleanupFuture = elementRemovalFuture.handle((ok, exeption) -> {
            registration.unregister();
            try {
                outputStream.close();
            } catch (final IOException ex) {
                logger.warn("Outport Stream closing issue...", ex);
            }
            return null;
        });

        // returns the result if available or throw exception
        try {
            cleanupFuture.join();
        } catch (RuntimeException re) {
            // expecting if the queue is not processed
        }
    }

    private boolean checkSessionValidity(final String sessionId) {
        boolean sessionAvailable = this.requests.containsKey(sessionId);
        if (sessionAvailable) {
            HttpSession storedSession = this.requests.get(sessionId);
            try {
                storedSession.getCreationTime();
                return true;
            } catch (IllegalStateException ise) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Registers the event handler
     *
     * @param topic
     *            the topic to track changes
     * @param eventQueue
     *            the shared queue
     * @param sessionId
     *            the session ID to track
     * @param output
     *            the output stream
     * @param future
     *            the SSE task's future handle
     * @return the service registration ID
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private ServiceRegistration<?> registerEventHandler(final String topic, final BlockingQueue<String> eventQueue,
            final String sessionId, final AtomicReference<Closeable> output, final CompletableFuture<?> future) {
        requireNonNull(topic, "Topic must not be null");
        requireNonNull(eventQueue, "Provided Queue must not be null");
        requireNonNull(sessionId, "Provided Session ID must not be null");
        requireNonNull(output, "Output Stream must not be null");
        requireNonNull(future, "Future reference must not be null");

        // event handler properties
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(EVENT_TOPIC, topic);
        // register the handler as a service instance
        return this.bundleContext.registerService(EventHandler.class, event -> {
            synchronized (EventHandlerServlet.class) {
                final String eventData = String.valueOf(event.getProperty("emitter"));

                // the event data can only be added to the queue if and only if it satisfies the following:
                // 1. The initiated session request is valid (the session is not expired)
                // 2. the queue has space to enqueue the event data
                final HttpSession storedSession = this.requests.get(sessionId);
                if (this.requests.containsKey(sessionId) && nonNull(storedSession) && eventQueue.offer(eventData)) {
                    return;
                }

                // session must be invalidated or it is already invalidated
                if (storedSession != null) {
                    storedSession.invalidate();
                }
                this.requests.remove(sessionId);

                // It signifies that there exists no consumer thread (browser or tab closed).
                // We must cancel the future instance that the current SSE worker thread
                // working on.
                final Closeable closeable = output.getAndSet(null);
                if (isNull(closeable)) {
                    // SSE worker thread already been killed
                    return;
                }
                try {
                    future.cancel(true);
                    closeable.close();
                } catch (final IOException ex) {
                    logger.warn("Output Stream closing issue..." + ex);
                }
            }
        } , props);
    }

}