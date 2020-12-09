/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.runner;

public interface ServiceDependency<T, C> {

    public interface Handle<C> {

        public void stop();

        public boolean isSatisfied();

        public void consume(C context);
    }

    public Handle<C> start(Runnable runnable);
}