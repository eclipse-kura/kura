/**
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TreeVisit {

    public enum State {
        PENDING,
        CANCELLED
    }

    private static final Logger logger = LoggerFactory.getLogger(TreeVisit.class);
    private static final int BROWSE_RESULT_MASK = BrowseResultMask.BrowseName.getValue()
            | BrowseResultMask.TypeDefinition.getValue() | BrowseResultMask.NodeClass.getValue();
    private static final ExpandedNodeId FOLDER_TYPE_EXPANDED_NODEID = new ExpandedNodeId(Identifiers.FolderType,
            "http://opcfoundation.org/UA/", 0);

    private final NodeId rootId;
    private final BiConsumer<String, ReferenceDescription> visitor;
    private final OpcUaClient client;

    private CompletableFuture<Void> future;

    private volatile State state = State.PENDING;

    public TreeVisit(final OpcUaClient client, final NodeId rootId,
            final BiConsumer<String, ReferenceDescription> visitor) {
        this.rootId = rootId;
        this.visitor = visitor;
        this.client = client;
    }

    private CompletableFuture<Void> visitRefs(final String rootPath, //
            final ReferenceDescription[] refs, //
            final ByteString continuationPoint, //
            final List<CompletableFuture<Void>> childrenVisits) {
        if (logger.isDebugEnabled()) {
            logger.debug("processing {} refs of {}", refs.length, rootPath);
        }

        for (final ReferenceDescription ref : refs) {

            if (getState() != State.PENDING) {
                return CompletableFuture.completedFuture(null);
            }

            final String path = rootPath + '/' + ref.getBrowseName().getName();

            this.visitor.accept(path, ref);

            final Optional<NodeId> nodeId = ref.getNodeId().local();

            if (nodeId.isPresent() && FOLDER_TYPE_EXPANDED_NODEID.equals(ref.getTypeDefinition())) {
                childrenVisits.add(visitSubtree(nodeId.get(), path));
            }
        }

        if (continuationPoint.isNotNull()) {
            if (logger.isDebugEnabled()) {
                logger.debug("continuing to visit {}", rootPath);
            }
            return this.client.browseNext(false, continuationPoint) //
                    .thenCompose(r -> visitRefs(rootPath, r.getReferences(), r.getContinuationPoint(), childrenVisits));
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("finished to visit {}", rootPath);
            }
            return CompletableFuture.allOf(childrenVisits.toArray(new CompletableFuture<?>[childrenVisits.size()]));
        }
    }

    private CompletableFuture<Void> visitSubtree(final NodeId root, final String rootPath) {

        if (logger.isDebugEnabled()) {
            logger.debug("browsing {}", root);
        }

        if (getState() != State.PENDING) {
            return CompletableFuture.completedFuture(null);
        }

        final BrowseDescription browse = new BrowseDescription(root, BrowseDirection.Forward, Identifiers.References,
                true, UInteger.valueOf(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
                UInteger.valueOf(BROWSE_RESULT_MASK));

        final List<CompletableFuture<Void>> childrenVisits = new ArrayList<>();

        return this.client
                .browse(new ViewDescription(NodeId.NULL_VALUE, DateTime.MIN_VALUE, UInteger.valueOf(0)),
                        UInteger.valueOf(50), Collections.singletonList(browse)) //
                .thenApply(r -> r.getResults()[0]) //
                .thenCompose(r -> visitRefs(rootPath, r.getReferences(), r.getContinuationPoint(), childrenVisits));
    }

    public CompletableFuture<Void> run() {
        logger.info("browsing {}...", this.rootId);

        this.future = visitSubtree(this.rootId, "") //
                .whenComplete((ok, err) -> logger.info("browsing {}...done", this.rootId));

        return this.future;
    }

    public CompletableFuture<Void> getFuture() {
        return this.future;
    }

    public void stop() {
        this.state = State.CANCELLED;
    }

    public State getState() {
        return this.state;
    }
}