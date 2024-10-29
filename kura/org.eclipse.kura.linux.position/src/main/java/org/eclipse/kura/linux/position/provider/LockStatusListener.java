/*******************************************************************************
 * Copyright (c) 2018, 2024 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.linux.position.provider;

import org.eclipse.kura.position.PositionListener;

public interface LockStatusListener extends PositionListener {

    public void onLockStatusChanged(final boolean hasLock);
}