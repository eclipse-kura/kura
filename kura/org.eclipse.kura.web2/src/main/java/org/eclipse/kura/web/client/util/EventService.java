/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.service.GwtEventService;
import org.eclipse.kura.web.shared.service.GwtEventServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public final class EventService {

    private static final int ON_FAILURE_RESEND_DELAY = 5000;
    private static final EventService instance = new EventService();

    private final GwtEventServiceAsync gwtEventService = GWT.create(GwtEventService.class);
    private HashMap<String, LinkedList<Handler>> subscribedHandlers = new HashMap<>();
    private long lastEventTimestamp = 0;
    private Timer resendTimer;

    private class TimeoutRequestBuilder extends RpcRequestBuilder {

        @Override
        protected RequestBuilder doCreate(String serviceEntryPoint) {
            RequestBuilder builder = super.doCreate(serviceEntryPoint);
            builder.setTimeoutMillis(GwtEventService.POLL_TIMEOUT_SECONDS * 1000);
            return builder;
        }
    }

    private EventService() {
        ((ServiceDefTarget) gwtEventService).setRpcRequestBuilder(new TimeoutRequestBuilder());
        gwtEventService.getLastEventTimestamp(new AsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                lastEventTimestamp = Long.parseLong(result);
                gwtEventService.getNextEvents(Long.toString(lastEventTimestamp), eventCallback);
            }

            @Override
            public void onFailure(Throwable caught) {
                // Nothing to do
            }
        });
    }

    private final AsyncCallback<List<GwtEventInfo>> eventCallback = new AsyncCallback<List<GwtEventInfo>>() {

        @Override
        public void onSuccess(List<GwtEventInfo> result) {

            for (GwtEventInfo event : result) {
                processEvent(event);
            }

            stopResendTimer();

            gwtEventService.getNextEvents(Long.toString(lastEventTimestamp), eventCallback);
        }

        @Override
        public void onFailure(Throwable caught) {
            startResendTimer(ON_FAILURE_RESEND_DELAY);
        }
        
        private void processEvent(GwtEventInfo event) {

            if (event == null) {
                return;
            }

            lastEventTimestamp = Long.parseLong(event.getTimestamp());

            LinkedList<Handler> topicHandlers = subscribedHandlers.get(event.getTopic());

            if (topicHandlers != null) {
                for (Handler handler : topicHandlers) {
                    handler.handleEvent(event);
                }
            }
        }
        
        private void startResendTimer(int timeout) {
            stopResendTimer();

            resendTimer = new Timer() {

                @Override
                public void run() {
                    gwtEventService.getNextEvents(Long.toString(lastEventTimestamp), eventCallback);
                }
            };
            resendTimer.schedule(timeout);
        }
        
        private void stopResendTimer() {
            if (resendTimer != null) {
                resendTimer.cancel();
            }

            resendTimer = null;
        }
    };

    public static void subscribe(ForwardedEventTopic topic, Handler handler) {
        LinkedList<Handler> topicHandlers = instance.subscribedHandlers.get(topic.toString());
        if (topicHandlers == null) {
            topicHandlers = new LinkedList<>();
            instance.subscribedHandlers.put(topic.toString(), topicHandlers);
        }
        topicHandlers.push(handler);
    }

    public static void unsubscribe(ForwardedEventTopic topic, Handler handler) {
        LinkedList<Handler> topicHandlers = instance.subscribedHandlers.get(topic.toString());
        if (topicHandlers != null) {
            topicHandlers.remove(handler);
        }
    }

    public interface Handler {
        public void handleEvent(GwtEventInfo eventInfo);
    }
}
