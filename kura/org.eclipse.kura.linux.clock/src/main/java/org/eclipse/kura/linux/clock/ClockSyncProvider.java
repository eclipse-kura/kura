/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.clock;

import java.util.Date;

import org.eclipse.kura.KuraException;

public interface ClockSyncProvider {

    public void init(ClockServiceConfig clockServiceConfig, ClockSyncListener listener) throws KuraException;

    public void start() throws KuraException;

    public void stop() throws KuraException;

    public Date getLastSync();

}
