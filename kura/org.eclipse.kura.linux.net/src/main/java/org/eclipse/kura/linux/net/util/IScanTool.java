/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.linux.net.util;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.wifi.WifiAccessPoint;

public interface IScanTool {

	public List<WifiAccessPoint> scan() throws KuraException;
}
