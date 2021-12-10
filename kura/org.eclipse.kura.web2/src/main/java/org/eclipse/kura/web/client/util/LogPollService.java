/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.shared.model.GwtLogEntry;
import org.eclipse.kura.web.shared.service.GwtLogService;
import org.eclipse.kura.web.shared.service.GwtLogServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.StatusCodeException;

public class LogPollService {

    private static final int POLL_TIMEOUT = 2000;
    private static final int ON_FAILURE_RESEND_DELAY = 2000;
    private static final int RESEND_DELAY = 100;
    private static final int RESEND_DELAY_NO_UPDATES = 5000;

    private Timer resendTimer;
    private final List<LogListener> listeners = new LinkedList<>();
    private static LogPollService instance = new LogPollService();
    private final GwtLogServiceAsync gwtLogService = GWT.create(GwtLogService.class);

    private final Logger logger = Logger.getLogger("LogPollService");
    private int rpcCount = 0;

    private int lastReadEntryId = 0;

    private LogPollService() {
        ((ServiceDefTarget) this.gwtLogService).setRpcRequestBuilder(new TimeoutRequestBuilder());
    }

    public static void startLogPolling() {
        startResendTimer(RESEND_DELAY);
    }

    public static void stopLogPolling() {
        stopResendTimer();
    }

    public static void subscribe(LogListener listener) {
        instance.listeners.add(listener);
    }

    public static void unsubscribe(LogListener listener) {
        instance.listeners.remove(listener);
    }

    public interface LogListener {

        public void onLogsReceived(List<GwtLogEntry> entries);
    }

    private class TimeoutRequestBuilder extends RpcRequestBuilder {

        @Override
        protected RequestBuilder doCreate(String serviceEntryPoint) {
            RequestBuilder builder = super.doCreate(serviceEntryPoint);
            builder.setTimeoutMillis(POLL_TIMEOUT);
            return builder;
        }
    }

    private static void startResendTimer(int timeout) {
        stopResendTimer();

        instance.resendTimer = new Timer() {

            @Override
            public void run() {
                instance.gwtLogService.readLogs(instance.lastReadEntryId, instance.eventCallback);
            }
        };
        instance.resendTimer.schedule(timeout);
    }

    private static void stopResendTimer() {
        if (instance.resendTimer != null) {
            instance.resendTimer.cancel();
        }

        instance.resendTimer = null;
    }

    private final AsyncCallback<List<GwtLogEntry>> eventCallback = new AsyncCallback<List<GwtLogEntry>>() {

        @Override
        public void onFailure(Throwable caught) {
            if (caught instanceof StatusCodeException) {
                final StatusCodeException statusCodeException = (StatusCodeException) caught;
                if (statusCodeException.getStatusCode() == 401) {
                    FailureHandler.handle(caught);
                }
            }

            startResendTimer(ON_FAILURE_RESEND_DELAY);
        }

        @Override
        public void onSuccess(List<GwtLogEntry> result) {
            LogPollService.this.logger.log(Level.INFO,
                    () -> "RPC successful. Count: " + LogPollService.instance.rpcCount++);

            int delay = RESEND_DELAY;

            if (result != null && !result.isEmpty()) {

                for (LogListener listener : LogPollService.instance.listeners) {
                    listener.onLogsReceived(result);
                }

                LogPollService.instance.lastReadEntryId = result.get(result.size() - 1).getId();

                stopResendTimer();
            } else {
                delay = RESEND_DELAY_NO_UPDATES;
            }

            startResendTimer(delay);
        }
    };
}
