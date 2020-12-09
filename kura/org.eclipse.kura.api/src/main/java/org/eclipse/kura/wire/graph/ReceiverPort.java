/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.graph;

import java.util.function.Consumer;

import org.eclipse.kura.wire.WireEnvelope;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface marks the ports that are receiver ports of the associated Wire Component.
 *
 * @since 1.4
 */
@ProviderType
public interface ReceiverPort extends Port {

    public void onWireReceive(Consumer<WireEnvelope> consumer);
}
