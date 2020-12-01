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
package org.eclipse.kura.wire.multiport;

import org.eclipse.kura.wire.WireComponent;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.wireadmin.Consumer;

/**
 * The MultiportWireReceiver interface Represents a wire component which is a data
 * consumer that can receive produced or emitted values from upstream
 * {@link org.eclipse.kura.wire.WireEmitter}.
 *
 * @since 1.4
 */
@ConsumerType
public interface MultiportWireReceiver extends Consumer, WireComponent {

}
