/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.eclipse.kura.camel.runner.ServiceDependency.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyRunner<C> {

    private static final Logger logger = LoggerFactory.getLogger(DependencyRunner.class);

    public interface Listener<C> {

        public void ready(List<Handle<C>> dependencies);

        public void notReady();
    }

    private final ArrayList<ServiceDependency<?, C>> dependencies;
    private final DependencyRunner.Listener<C> listener;

    private List<Handle<C>> dependencyHandles;

    private boolean lastState = false;

    private final Runnable update = new Runnable() {

        @Override
        public void run() {
            DependencyRunner.this.update();
        }
    };
    private boolean working;

    public DependencyRunner(final List<ServiceDependency<?, C>> dependencies,
            final DependencyRunner.Listener<C> listener) {
        Objects.requireNonNull(dependencies);
        Objects.requireNonNull(listener);

        this.dependencies = new ArrayList<>(dependencies);
        this.listener = listener;
    }

    public void start() {
        this.working = true;

        try {
            this.dependencyHandles = new LinkedList<>();
            for (final ServiceDependency<?, C> dep : this.dependencies) {
                this.dependencyHandles.add(dep.start(this.update));
            }
        } finally {
            this.working = false;
        }

        // trigger first time

        if (isReady()) {
            triggerUpdate(true);
        }
    }

    public void stop() {
        this.working = true;
        try {
            for (final Handle<C> dep : this.dependencyHandles) {
                dep.stop();
            }
        } finally {
            this.working = false;
        }

        triggerUpdate(false);
    }

    private void update() {
        logger.debug("update");

        if (this.working) {
            logger.debug("update - start/stop in progress");
            return;
        }

        boolean state = isReady();
        triggerUpdate(state);
    }

    private void triggerUpdate(final boolean state) {
        logger.debug("triggerUpdate - state: {}, lastState: {}", state, this.lastState);

        if (this.lastState == state) {
            return;
        }

        this.lastState = state;

        if (state) {
            this.listener.ready(Collections.unmodifiableList(this.dependencyHandles));
        } else {
            this.listener.notReady();
        }
    }

    private boolean isReady() {
        for (final Handle<C> dep : this.dependencyHandles) {
            boolean satisfied = dep.isSatisfied();
            logger.debug("Dependency - {}, satisied: {}", dep, satisfied);
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }
}