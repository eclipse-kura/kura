/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.linux.net.util;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.wifi.WifiAccessPoint;

@FunctionalInterface
public interface IScanTool {

    public List<WifiAccessPoint> scan() throws KuraException;
}
