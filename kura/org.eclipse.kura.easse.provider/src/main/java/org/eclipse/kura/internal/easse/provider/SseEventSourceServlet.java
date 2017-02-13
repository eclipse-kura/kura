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

import static org.eclipse.kura.util.base.StringUtil.isNullOrEmpty;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component provides a servlet that allows Javascript clients to see the
 * Event Admin events. The request {@code topic} is considered as an Event Topic
 * filter on an Event Handler service.
 */
public final class SseEventSourceServlet extends EventSourceServlet {

    /** Serial Version */
    private static final long serialVersionUID = -173441936742738853L;

    /** Logger Instance */
    private static final Logger LOG = LoggerFactory.getLogger(SseEventSourceServlet.class);

    /** {@inheritDoc} */
    @Override
    protected EventSource newEventSource(final HttpServletRequest request) {
        LOG.debug("Event Source Request Received");
        final String topic = request.getParameter("topic");
        if (isNullOrEmpty(topic)) {
            throw new IllegalArgumentException("Topic must not be null or empty");
        }
        final SseEventSource eventSource = new SseEventSource(topic);
        EventPublisher.getInstance().addEventSource(eventSource);
        return eventSource;
    }
}