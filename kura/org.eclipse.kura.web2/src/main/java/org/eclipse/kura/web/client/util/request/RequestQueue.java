/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.util.request;

import java.util.LinkedList;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/*
 * Allows to serialize asynchronous requests. Can be used to prevent issues related to XSRF tokens and concurrent requests.
 */
public class RequestQueue {

    private static final RequestQueue instance = new RequestQueue();

    private final LinkedList<PendingRequest> requests = new LinkedList<>();
    private PendingRequest pending;

    public static void submit(final Request request) {
        RequestQueue.instance.submitInternal(request, true);
    }

    public static void submit(final Request request, final boolean enableWaitModal) {
        RequestQueue.instance.submitInternal(request, enableWaitModal);
    }

    private void submitInternal(final Request request, final boolean enableWaitModal) {
        this.requests.push(new PendingRequest(request, new RequestContextImpl(enableWaitModal)));
        runNext();
    }

    private void runNext() {
        if (this.pending != null) {
            return;
        }
        if (this.requests.isEmpty()) {
            return;
        }

        this.pending = this.requests.pop();

        try {
            this.pending.request.run(this.pending.context);
        } catch (Exception e) {
            FailureHandler.handle(e);
            EntryClassUi.hideWaitModal();
            this.pending = null;
        }
    }

    private class RequestContextImpl implements RequestContext {

        private int requests = 0;
        private final boolean enableWaitModal;

        public RequestContextImpl(final boolean enableWaitModal) {
            this.enableWaitModal = enableWaitModal;
        }

        private void completed() {
            if (this.enableWaitModal) {
                EntryClassUi.hideWaitModal();
            }
            RequestQueue.this.pending = null;
            runNext();
        }

        private void newRequest() {
            this.requests++;
            if (this.enableWaitModal) {
                EntryClassUi.showWaitModal();
            }
        }

        private void requestCompleted() {
            this.requests--;
            if (this.requests == 0) {
                completed();
            }
        }

        @Override
        public <T> AsyncCallback<T> callback(final SuccessCallback<T> callback) {
            return callback(new AsyncCallback<T>() {

                @Override
                public void onFailure(Throwable caught) {
                    FailureHandler.handle(caught);
                }

                @Override
                public void onSuccess(T result) {
                    callback.onSuccess(result);
                }
            });
        }

        @Override
        public <T> AsyncCallback<T> callback(final AsyncCallback<T> callback) {
            newRequest();
            return new AsyncCallback<T>() {

                @Override
                public void onFailure(Throwable caught) {
                    try {
                        callback.onFailure(caught);
                    } finally {
                        requestCompleted();
                    }
                }

                @Override
                public void onSuccess(T result) {
                    try {
                        callback.onSuccess(result);
                        requestCompleted();
                    } catch (final Exception e) {
                        onFailure(e);
                    }
                }
            };
        }

        @Override
        public <T> AsyncCallback<T> callback() {
            return callback(result -> {
            });
        }

        @Override
        public void defer(final int delayMs, final Runnable action) {
            newRequest();
            final Timer timer = new Timer() {

                @Override
                public void run() {
                    try {
                        action.run();
                    } catch (final Exception e) {
                        FailureHandler.handle(e);
                    } finally {
                        requestCompleted();
                    }
                }
            };
            timer.schedule(delayMs);
        }
    }

    private class PendingRequest {

        final Request request;
        final RequestContext context;

        public PendingRequest(Request request, RequestContext context) {
            this.request = request;
            this.context = context;
        }
    }
}
