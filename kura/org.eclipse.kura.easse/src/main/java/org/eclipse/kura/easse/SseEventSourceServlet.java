/*******************************************************************************
 * Copyright (c) 2017 Amit Kumar Mondal and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.easse;

import static org.eclipse.kura.util.base.StringUtil.isNullOrEmpty;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.kura.internal.easse.SseEventPublisher;
import org.eclipse.kura.internal.easse.SseEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component provides a servlet that allows SSE clients to subscribe to OSGi
 * events. The request {@code topic} is considered as an Event Topic
 * filter on an Event Handler service. Hence, {@code topic} cannot be {@code null}
 * or {@code empty}.
 */
public final class SseEventSourceServlet extends EventSourceServlet {

    /** Logger Instance */
    private static final Logger logger = LoggerFactory.getLogger(SseEventSourceServlet.class);

    /** Serial Version */
    private static final long serialVersionUID = -173441936742738853L;

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        super.destroy();
        SseEventPublisher.getInstance().destroy();
    }

    /** {@inheritDoc} */
    @Override
    protected EventSource newEventSource(final HttpServletRequest request) {
        logger.debug("Event Source Request Received");
        final String topic = request.getParameter("topic");
        if (isNullOrEmpty(topic)) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        final SseEventSource eventSource = SseEventSource.of(topic);
        SseEventPublisher.getInstance().addEventSource(eventSource);
        return eventSource;
    }
}