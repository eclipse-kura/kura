/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;

public interface MockInterface<T extends NetInterfaceAddress> extends NetInterface<T> {
}
