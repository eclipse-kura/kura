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
 * The Class {@link EventHandlerServlet} is responsible for interacting between Event
 * Admin and Javascript through Server Sent Events (SSE). This is mainly required
 * for Kura Wires to delegate the emit events.
 */
public final class EventHandlerServlet extends HttpServlet {

    /** The Logger Instance. */
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerServlet.class);

    /** Session Timeout in Seconds - 5 minutes */
    private static final int MAX_INACTIVE_INTERVAL = 5 * 60;
    
    /** Required for IE9 */
    private static byte[] prelude;

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
        final String requestId = request.getParameter("session");

        // set the response headers for SSE
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*"); // required for IE9

        // destroy the session if exists. It actually destroys the current session which initiated
        // the destruction request
        final String sessionToDestroy = request.getParameter("logout");
        if (!isNullOrEmpty(sessionToDestroy)) {
            cleanRequest(requestId);
            return;
        }

        final HttpSession session = request.getSession(false);
        // timeout is required if the browser beforeunload event is not fired in exceptional circumstances.
        // If there exists a session in which a browser is opened, the beforeunload event is expected to
        // be fired as soon as the browser or the tab is closed. But if by any chance, this beforeunload
        // event is not fired, the timeout of a session will notice the disconnection.
        session.setMaxInactiveInterval(MAX_INACTIVE_INTERVAL);

        if (!isNullOrEmpty(requestId) && this.requests.containsKey(requestId)) {
            cleanRequest(requestId);
        }

        if (!isNullOrEmpty(requestId)) {
            this.requests.put(requestId, session);
        }

        final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>(MAX_SIZE_OF_QUEUE);
        final ServletOutputStream outputStream = response.getOutputStream();
        final PrintStream printStream = new PrintStream(outputStream);
        
        //required for low-layer buffering in IE 9. We must send 2K prelude.
        //https://blogs.msdn.microsoft.com/ieinternals/2010/04/05/comet-streaming-in-internet-explorer/
        String userAgent = request.getHeader("User-Agent");
        if (nonNull(userAgent) && userAgent.contains("MSIE 9.")) {
            outputStream.write(getIE9Prelude());
            outputStream.flush();
        }

        // the asynchronous task to retrieve and remove head element from the queue
        final CompletableFuture<Void> elementRemovalFuture = CompletableFuture.runAsync(() -> {
            try {
                boolean requestExistsAndValid = checkRequestValidity(requestId);
                // if session exists and valid, remove head element from the queue
                while (requestExistsAndValid && !printStream.checkError()) {
                    final String data = eventQueue.poll(2, TimeUnit.SECONDS);
                    if (isNull(data)) {
                        printStream.print(":\n\n");
                    } else {
                        logger.debug("Sending data for request: {}", requestId);
                        printStream.printf("data: %s%n%n", data);
                    }
                    requestExistsAndValid = checkRequestValidity(requestId);
                }
            } catch (final InterruptedException ex) {
                logger.warn("Element removal timeout...", ex);
                response.setStatus(HttpServletResponse.SC_OK);
            }
        });

        final ServiceRegistration<?> registration = registerEventHandler(EMIT_EVENT_TOPIC, eventQueue, requestId,
                elementRemovalFuture);
        // unregisters event handler service and close the servlet output stream
        final CompletableFuture<Void> cleanupFuture = elementRemovalFuture.handle((ok, exeption) -> {
            logger.info("Cleaning resources for request: {}", requestId);
            cleanRequest(requestId);
            registration.unregister();
            try {
                printStream.close();
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

    private boolean checkRequestValidity(final String requestId) {
        boolean requestAvailable = this.requests.containsKey(requestId);
        if (requestAvailable) {
            HttpSession storedSession = this.requests.get(requestId);
            try {
                storedSession.getCreationTime();
                return true;
            } catch (IllegalStateException ise) {
            }
        }
        return false;
    }

    /**
     * Registers the event handler
     *
     * @param topic
     *            the topic to track changes
     * @param eventQueue
     *            the shared queue
     * @param requestId
     *            the request ID to track
     * @param output
     *            the output stream
     * @param future
     *            the SSE task's future handle
     * @return the service registration ID
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private ServiceRegistration<?> registerEventHandler(final String topic, final BlockingQueue<String> eventQueue,
            final String requestId, final CompletableFuture<?> future) {
        requireNonNull(topic, "Topic must not be null");
        requireNonNull(eventQueue, "Provided Queue must not be null");
        requireNonNull(requestId, "Provided Session ID must not be null");
        requireNonNull(future, "Future reference must not be null");

        // event handler properties
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(EVENT_TOPIC, topic);
        // register the handler as a service instance
        return this.bundleContext.registerService(EventHandler.class, event -> {
            synchronized (EventHandlerServlet.class) {
                final String eventData = String.valueOf(event.getProperty("emitter"));

                final boolean validRequest = checkRequestValidity(requestId);
                final boolean consumerAlive = eventQueue.offer(eventData);
                if (validRequest && consumerAlive) {
                    return;
                } else {
                    logger.debug("Valid request: {}, consumer alive: {}. Ready to close...", validRequest,
                            consumerAlive);
                    cleanRequest(requestId);

                    future.cancel(true);
                }
            }
        } , props);
    }
    
    /**
     * Returns the MSIE-9 2K prelude.
     */
    private static byte[] getIE9Prelude() {
        if (isNull(prelude)) {
            prelude = new byte[2048];
            prelude[0] = ':';
            for (int i = 1; i < prelude.length - 1; i++)
                prelude[i] = ' ';
            prelude[prelude.length - 1] = '\n';
        }
        return prelude;
    }

    private void cleanRequest(final String requestId) {
        logger.debug("Cleaning request: {}", requestId);
        this.requests.remove(requestId);
    }
}