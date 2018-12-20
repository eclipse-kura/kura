/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.service.GwtEventService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class GwtEventServiceImpl extends OsgiRemoteServiceServlet implements GwtEventService, EventHandler {

    private static final long serialVersionUID = 4948177265652519828L;

    private static final int MAX_EVENT_COUNT = 50;

    private final LinkedList<String> topics = new LinkedList<>();
    private final LinkedList<GwtEventInfo> events = new LinkedList<>();
    private ServiceRegistration<EventHandler> registration;

    public GwtEventServiceImpl() {
        for (ForwardedEventTopic topic : ForwardedEventTopic.values()) {
            this.topics.add(topic.toString());
        }
    }

    @Override
    public synchronized void handleEvent(Event event) {

        GwtEventInfo eventInfo = serialize(event);

        if (this.events.size() >= MAX_EVENT_COUNT) {
            this.events.removeLast();
        }

        this.events.push(eventInfo);

        notifyAll();
    }

    private List<GwtEventInfo> getEvents(long fromTimestamp) {
        LinkedList<GwtEventInfo> result = new LinkedList<>();

        Iterator<GwtEventInfo> i = this.events.iterator();

        while (i.hasNext()) {
            GwtEventInfo next = i.next();
            if (Long.parseLong(next.getTimestamp()) <= fromTimestamp) {
                break;
            }
            result.push(next);
        }

        return result;
    }

    public void start() {
        stop();

        Dictionary<String, Object> map = new Hashtable<>();

        map.put(EventConstants.EVENT_TOPIC, this.topics.toArray(new String[this.topics.size()]));

        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        this.registration = bundleContext.registerService(EventHandler.class, this, map);
    }

    public void stop() {
        if (this.registration != null) {
            this.registration.unregister();
            this.registration = null;
        }
    }

    @Override
    public synchronized List<GwtEventInfo> getNextEvents(String fromTimestamp) {
        long timestamp = Long.parseLong(fromTimestamp);

        List<GwtEventInfo> result = getEvents(timestamp);

        if (!result.isEmpty()) {
            return result;
        }

        try {
            this.wait(POLL_TIMEOUT_SECONDS * 1000);
        } catch (InterruptedException e) {
            return new LinkedList<>();
        }

        return getEvents(timestamp);
    }

    @Override
    public synchronized String getLastEventTimestamp() {
        if (this.events.isEmpty()) {
            return "0";
        }
        return this.events.getFirst().getTimestamp();
    }

    public GwtEventInfo serialize(Event event) {
        GwtEventInfo result = new GwtEventInfo(event.getTopic());

        for (String property : event.getPropertyNames()) {
            if ("event".equals(property)) {
                continue;
            }

            Object obj = event.getProperty(property);
            result.set(property, obj != null ? obj.toString() : null);
        }

        return result;
    }

}
