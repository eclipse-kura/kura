/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * This interface marks the ports that are receiver ports of the associated Wire Component.
 *
 * @since 1.4
 */
@ConsumerType
public interface ReceiverPort extends Port {

}
