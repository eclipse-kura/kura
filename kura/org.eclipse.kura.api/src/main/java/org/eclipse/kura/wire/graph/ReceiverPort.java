/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
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
