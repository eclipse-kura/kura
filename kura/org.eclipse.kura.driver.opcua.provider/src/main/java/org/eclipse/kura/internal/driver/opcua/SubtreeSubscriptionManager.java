/**
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */

package org.eclipse.kura.internal.driver.opcua;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.internal.driver.opcua.request.ListenParams;
import org.eclipse.kura.internal.driver.opcua.request.ListenRequest;
import org.eclipse.kura.internal.driver.opcua.request.SingleNodeListenParams;
import org.eclipse.kura.internal.driver.opcua.request.SubtreeNodeListenParams;
import org.eclipse.kura.internal.driver.opcua.request.TreeListenParams;
import org.eclipse.kura.type.DataType;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubtreeSubscriptionManager implements ListenerRegistrationRegistry.Listener {

    private static final Logger logger = LoggerFactory.getLogger(SubtreeSubscriptionManager.class);

    private final OpcUaClient client;

    private final ListenerRegistrationRegistry registrations;
    private final ListenerRegistrationRegistry subtreeRegistrations;

    private final SubscriptionManager subscriptionManager;

    private final ChannelNameFormat channelNameFormat;

    private final Set<PendingVisit> pendingVisits = new CopyOnWriteArraySet<>();
    private boolean isClosed;

    public SubtreeSubscriptionManager(final OpcUaOptions options, final OpcUaClient client, final AsyncTaskQueue queue,
            final ListenerRegistrationRegistry registrations) {
        this.client = client;
        this.registrations = registrations;
        this.subtreeRegistrations = new ListenerRegistrationRegistry();
        this.subscriptionManager = new SubscriptionManager(options, client, queue, this.subtreeRegistrations);
        this.channelNameFormat = options.getSubtreeSubscriptionChannelNameFormat();

        synchronized (this) {
            registrations.addRegistrationItemListener(this);
            registrations.getRegisteredListeners().values().stream().flatMap(Collection::stream).forEach(
                    req -> registerChannelListener((TreeListenParams) req.getParameters(), req.getChannelListener()));
        }
    }

    public synchronized void registerChannelListener(final TreeListenParams subtreeParams,
            final ChannelListener listener) {
        if (isClosed()) {
            throw new IllegalStateException("already closed");
        }

        final List<ListenRequest> requests = new ArrayList<>();

        final TreeVisit visit = visitSubtree(subtreeParams, (path, n) -> {
            if (n.getNodeClass() != NodeClass.Variable) {
                return;
            }

            final Optional<NodeId> nodeId = n.getNodeId().local();

            if (logger.isTraceEnabled()) {
                logger.trace("found variable node: {} {}", path, nodeId);
            }

            if (!nodeId.isPresent()) {
                return;
            }

            ReadValueId readValueId = new ReadValueId(nodeId.get(), subtreeParams.getReadValueId().getAttributeId(),
                    null, null);

            final ListenParams nodeParams = new SubtreeNodeListenParams(readValueId, subtreeParams);

            final String channelName = this.channelNameFormat == ChannelNameFormat.BROWSE_PATH ? path
                    : nodeId.get().toParseableString();

            requests.add(new ListenRequest(nodeParams, ChannelRecord.createReadRecord(channelName, DataType.STRING),
                    listener));

        });

        this.pendingVisits.add(new PendingVisit(visit, listener));

        visit.getFuture() //
                .thenAccept(ok -> {
                    synchronized (this) {
                        if (visit.getState() != TreeVisit.State.PENDING) {
                            return;
                        }

                        logger.info("subscribing to {} nodes", requests.size());
                        this.subtreeRegistrations.registerListeners(requests);
                    }
                }) //
                .whenComplete((ok, err) -> removeVisit(visit));
    }

    public synchronized void unregisterChannelListener(final ChannelListener listener) {
        if (isClosed()) {
            throw new IllegalStateException("already closed");
        }

        this.pendingVisits.removeIf(v -> {
            if (v.listener == listener) {
                v.visit.stop();
                return true;
            }
            return false;
        });

        this.subtreeRegistrations.unregisterListener(listener);
    }

    public synchronized CompletableFuture<Void> close() {
        this.isClosed = true;

        this.registrations.removeRegistrationItemListener(this);

        final List<CompletableFuture<Void>> visitFutures = new ArrayList<>();

        for (final PendingVisit pendingVisit : this.pendingVisits) {
            pendingVisit.visit.stop();
            visitFutures.add(pendingVisit.visit.getFuture());
        }

        this.pendingVisits.clear();

        return CompletableFuture.allOf(visitFutures.toArray(new CompletableFuture<?>[visitFutures.size()])) //
                .thenCompose(ok -> this.subscriptionManager.close());
    }

    private synchronized void removeVisit(final TreeVisit visit) {
        this.pendingVisits.removeIf(v -> v.visit == visit);
    }

    private synchronized boolean isClosed() {
        return this.isClosed;
    }

    private TreeVisit visitSubtree(final SingleNodeListenParams rootParams,
            final BiConsumer<String, ReferenceDescription> visitor) {
        final TreeVisit visit = new TreeVisit(this.client, rootParams.getReadValueId().getNodeId(), visitor);

        visit.run();

        return visit;
    }

    private static class PendingVisit {

        private final TreeVisit visit;
        private final ChannelListener listener;

        public PendingVisit(final TreeVisit visit, final ChannelListener listener) {
            this.visit = visit;
            this.listener = listener;
        }
    }

    @Override
    public synchronized void onRegistrationsChanged() {
        // no need
    }

    @Override
    public void onListenerRegistered(ListenRequest request) {
        registerChannelListener((TreeListenParams) request.getParameters(), request.getChannelListener());
    }

    @Override
    public void onListenerUnregistered(ChannelListener listener) {
        unregisterChannelListener(listener);
    }

}
