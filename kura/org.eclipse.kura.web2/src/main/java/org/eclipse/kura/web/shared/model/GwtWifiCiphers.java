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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum GwtWifiCiphers implements Serializable,IsSerializable {
    netWifiCiphers_NONE,
    netWifiCiphers_CCMP_TKIP,
    netWifiCiphers_CCMP,
    netWifiCiphers_TKIP
}
