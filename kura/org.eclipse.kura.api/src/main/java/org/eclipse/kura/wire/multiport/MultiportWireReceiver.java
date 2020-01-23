/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.wire.multiport;

import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.wireadmin.Consumer;

/**
 * The MultiportWireReceiver interface Represents a wire component which is a data
 * consumer that can receive produced or emitted values from upstream
 * {@link WireEmitter}.
 *
 * @since 1.4
 */
@ConsumerType
public interface MultiportWireReceiver extends Consumer, WireComponent {

}
