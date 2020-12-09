/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 ******************************************************************************/
package org.eclipse.kura.wire;

import org.osgi.annotation.versioning.ProviderType;

/**
 * WireComponent is a marker interface representing a generic identity for
 * {@link WireEmitter}s and {@link WireReceiver}s. A Wire Component is a
 * generalization of a component responsible for producing data also known as
 * {@code WireEmitter} and/or consuming data also known as {@code WireReceiver}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface WireComponent {
}
