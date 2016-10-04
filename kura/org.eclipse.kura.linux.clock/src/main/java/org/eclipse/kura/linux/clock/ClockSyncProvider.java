/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.util.Date;
import java.util.Map;

import org.eclipse.kura.KuraException;

public interface ClockSyncProvider {

    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException;

    public void start() throws KuraException;

    public void stop() throws KuraException;

    public Date getLastSync();

}
