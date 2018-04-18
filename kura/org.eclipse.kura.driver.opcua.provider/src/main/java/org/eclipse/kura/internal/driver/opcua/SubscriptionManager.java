/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.kura.internal.driver.opcua.Utils.fillRecord;
import static org.eclipse.kura.internal.driver.opcua.Utils.fillValue;
import static org.eclipse.kura.internal.driver.opcua.Utils.splitInMultipleRequests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.kura.internal.driver.opcua.ListenerRegistrations.Dispatcher;
import org.eclipse.kura.internal.driver.opcua.request.ListenParams;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener;
import org.eclipse.milo.opcua.sdk.client.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionManager implements SubscriptionListener, ListenerRegistrations.Listener {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class);

    private static ExtensionObject defaultEventFilter = ExtensionObject
            .encode(new EventFilter(new SimpleAttributeOperand[] {
                    new SimpleAttributeOperand(Identifiers.BaseEventType,
                            new QualifiedName[] { new QualifiedName(0, BaseEventType.TIME.getBrowseName()) }, AttributeId.Value.uid(), null),
                    new SimpleAttributeOperand(Identifiers.BaseEventType,
                            new QualifiedName[] { new QualifiedName(0,BaseEventType.MESSAGE.getBrowseName()) }, AttributeId.Value.uid(),
                            null) },
                    new ContentFilter(null)));

    private final OpcUaClient client;
    private final OpcUaOptions options;
    private ListenerRegistrations registrations;
    private final AsyncTaskQueue queue;

    private long currentRegistrationState;
    private long targetRegistrationState;

    private State state;

    public SubscriptionManager(final OpcUaOptions options, final OpcUaClient client, final AsyncTaskQueue queue,
            final ListenerRegistrations registrations) {
        this.queue = queue;
        this.options = options;
        this.client = client;
        this.registrations = registrations;
        registrations.addRegistrationItemListener(this);
        this.state = new Unsubscribed();
    }

    @Override
    public synchronized void onRegistrationsChanged() {
        this.targetRegistrationState++;
        queue.push(() -> this.state.updateSubscriptionState());
    }

    @Override
    public synchronized void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
        logger.debug("Subscription transfer failed");
        this.state = new Unsubscribed();
        onRegistrationsChanged();
    }

    public synchronized CompletableFuture<Void> close() {
        registrations.removeRegistrationItemListener(this);
        return this.state.unsubscribe();
    }

    private interface State {

        CompletableFuture<Void> subscribe();

        CompletableFuture<Void> unsubscribe();

        CompletableFuture<Void> updateSubscriptionState();
    }

    private class Subscribed implements State {

        private final Map<ListenParams, MonitoredItemHandler> monitoredItemHandlers = new HashMap<>();
        final UaSubscription subscription;

        Subscribed(final UaSubscription subscription) {
            this.subscription = subscription;
            client.getSubscriptionManager().addSubscriptionListener(SubscriptionManager.this);
        }

        @Override
        public CompletableFuture<Void> subscribe() {
            return completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> unsubscribe() {
            logger.debug("Unsubscribing..");
            final OpcUaSubscriptionManager manager = client.getSubscriptionManager();
            manager.removeSubscriptionListener(SubscriptionManager.this);
            for (final MonitoredItemHandler handler : monitoredItemHandlers.values()) {
                handler.close();
            }
            monitoredItemHandlers.clear();
            return manager.deleteSubscription(subscription.getSubscriptionId()).handle((ok, e) -> {
                if (e != null) {
                    logger.debug("Failed to delete subscription", e);
                }
                logger.debug("Unsubscribing..done");
                synchronized (SubscriptionManager.this) {
                    state = new Unsubscribed();
                }
                return (Void) null;
            });
        }

        @Override
        public CompletableFuture<Void> updateSubscriptionState() {

            synchronized (SubscriptionManager.this) {
                logger.debug("Updating subscription state...");
                final long targetState = SubscriptionManager.this.targetRegistrationState;

                if (currentRegistrationState == targetState) {
                    logger.debug("Target state reached, nothing to do");
                    return completedFuture(null);
                }

                final Consumer<Void> onCompletion = ok -> {
                    logger.debug("Updating subscription state...done");
                    synchronized (this) {
                        currentRegistrationState = targetState;
                    }
                };

                final List<MonitoredItemHandler> toBeCreated = new ArrayList<>();
                final List<MonitoredItemHandler> toBeDeleted = new ArrayList<>();

                registrations.computeDifferences(monitoredItemHandlers.keySet(),
                        item -> toBeCreated.add(new MonitoredItemHandler(item)),
                        item -> toBeDeleted.add(monitoredItemHandlers.get(item)));

                if (toBeCreated.isEmpty() && toBeDeleted.size() == monitoredItemHandlers.size()) {
                    return state.unsubscribe().thenAccept(onCompletion);
                }

                toBeDeleted.removeIf(handler -> {
                    if (!handler.isValid()) {
                        monitoredItemHandlers.remove(handler.getParams());
                        return true;
                    } else {
                        return false;
                    }
                });

                return CompletableFuture.allOf(createMonitoredItems(subscription, toBeCreated),
                        deleteMonitoredItems(subscription, toBeDeleted)).thenAccept(onCompletion);
            }
        }

        protected CompletableFuture<Void> createMonitoredItems(final UaSubscription subscription,
                final List<MonitoredItemHandler> handlers) {
            final List<MonitoredItemCreateRequest> requests = handlers.stream()
                    .map(handler -> handler.getMonitoredItemCreateRequest(client.nextRequestHandle()))
                    .collect(Collectors.toList());

            logger.debug("Creating {} monitored items", handlers.size());

            if (!requests.isEmpty()) {
                final ArrayList<CompletableFuture<?>> tasks = new ArrayList<>();

                splitInMultipleRequests(options.getMaxItemCountPerRequest(), requests.size(),
                        (start, end) -> tasks.add(createMonitoredItems(subscription, requests.subList(start, end),
                                handlers.subList(start, end))));

                return CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[tasks.size()]));
            } else {
                return completedFuture(null);
            }
        }

        protected CompletableFuture<Void> createMonitoredItems(final UaSubscription subscription,
                final List<MonitoredItemCreateRequest> requests, final List<MonitoredItemHandler> handlers) {
            return subscription.createMonitoredItems(TimestampsToReturn.Source, requests).thenAccept(monitoredItems -> {
                for (int i = 0; i < handlers.size(); i++) {
                    final MonitoredItemHandler handler = handlers.get(i);
                    handler.setMonitoredItem(monitoredItems.get(i));
                    synchronized (SubscriptionManager.this) {
                        monitoredItemHandlers.put(handler.getParams(), handler);
                    }
                }
            });
        }

        protected CompletableFuture<Void> deleteMonitoredItems(final UaSubscription subscription,
                final List<MonitoredItemHandler> handlers) {
            final List<UaMonitoredItem> requests = handlers.stream().map(handler -> handler.getMonitoredItem().get())
                    .collect(Collectors.toList());

            logger.debug("Deleting {} monitored items", requests.size());

            if (!requests.isEmpty()) {
                final ArrayList<CompletableFuture<?>> tasks = new ArrayList<>();

                splitInMultipleRequests(options.getMaxItemCountPerRequest(), requests.size(), (start, end) -> tasks
                        .add(subscription.deleteMonitoredItems(requests.subList(start, end)).thenAccept(ok -> {
                            synchronized (SubscriptionManager.this) {
                                for (final MonitoredItemHandler handler : handlers) {
                                    handler.close();
                                    monitoredItemHandlers.remove(handler.getParams());
                                }
                            }
                        })));

                return CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[tasks.size()]));
            } else {
                return completedFuture(null);
            }
        }
    }

    private class Unsubscribed implements State {

        Unsubscribed() {
        }

        @Override
        public CompletableFuture<Void> unsubscribe() {
            return completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> subscribe() {
            logger.debug("Subscribing...");

            return client.getSubscriptionManager().createSubscription(options.getSubsciptionPublishInterval())
                    .thenAccept(subscription -> {
                        logger.debug("Subscribing...done, max notifications per publish: {}",
                                subscription.getMaxNotificationsPerPublish());
                        synchronized (SubscriptionManager.this) {
                            state = new Subscribed(subscription);
                        }
                    });
        }

        @Override
        public CompletableFuture<Void> updateSubscriptionState() {
            if (registrations.isEmpty()) {
                logger.debug("No need to subscribe");
                return CompletableFuture.completedFuture(null);
            }
            return this.subscribe().thenCompose(ok -> state.updateSubscriptionState());
        }
    }

    private class MonitoredItemHandler {

        Optional<UaMonitoredItem> monitoredItem = Optional.empty();
        final Dispatcher dispatcher;

        public MonitoredItemHandler(final ListenParams params) {
            this.dispatcher = registrations.getDispatcher(params);
        }

        public MonitoredItemCreateRequest getMonitoredItemCreateRequest(final UInteger requestHandle) {
            final ListenParams params = dispatcher.getParams();
            final ReadValueId readValueId = params.getReadValueId();
            final boolean isEventNotifier = AttributeId.EventNotifier.uid().equals(readValueId.getAttributeId());
            final MonitoringParameters monitoringParams = new MonitoringParameters(requestHandle,
                    isEventNotifier ? 0.0 : params.getSamplingInterval(), isEventNotifier ? defaultEventFilter : null,
                    UInteger.valueOf(params.getQueueSize()), params.getDiscardOldest());
            return new MonitoredItemCreateRequest(params.getReadValueId(), MonitoringMode.Reporting, monitoringParams);
        }

        public Optional<UaMonitoredItem> getMonitoredItem() {
            return monitoredItem;
        }

        public ListenParams getParams() {
            return dispatcher.getParams();
        }

        public boolean isValid() {
            return monitoredItem.isPresent();
        }

        public void setMonitoredItem(final UaMonitoredItem item) {
            final StatusCode code = item.getStatusCode();
            if (!code.isGood()) {
                logger.debug("Got bad status code for monitored item - code: {}, item: {}", code, item);
                return;
            }
            this.monitoredItem = Optional.of(item);
            item.setEventConsumer(this::dispatchEvent);
            item.setValueConsumer(this::dispatchValue);
        }

        public void dispatchEvent(final Variant[] values) {
            dispatcher.dispatch(record -> {
                fillValue(values[1], record);

                try {
                    record.setTimestamp(((DateTime) values[0].getValue()).getJavaTime());
                } catch (Exception e) {
                    logger.debug("Failed to extract event Time, using locally generated timestamp");
                    record.setTimestamp(System.currentTimeMillis());
                }
            });
        }

        public void dispatchValue(final DataValue value) {
            dispatcher.dispatch(record -> fillRecord(value, record));
        }

        public void close() {
            this.monitoredItem.ifPresent(item -> {
                item.setEventConsumer((Consumer<Variant[]>) null);
                item.setValueConsumer((Consumer<DataValue>) null);
            });
            this.monitoredItem = Optional.empty();
        }
    }
}
