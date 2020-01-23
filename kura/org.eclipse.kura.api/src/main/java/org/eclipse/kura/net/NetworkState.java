/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

/**
 * The overall state of the networking subsystem.
 */
public enum NetworkState {
    /** Networking state is unknown. */
    UNKNOWN(0),
    /** Networking is inactive and all devices are disabled. */
    ASLEEP(10),
    /** There is no active network connection. */
    DISCONNECTED(20),
    /** Network connections are being cleaned up. */
    DISCONNECTING(30),
    /** A network device is connecting to a network and there is no other available network connection. */
    CONNECTING(40),
    /** A network device is connected, but there is only link-local connectivity. */
    CONNECTED_LOCAL(50),
    /** A network device is connected, but there is only site-local connectivity. */
    CONNECTED_SITE(60),
    /** A network device is connected, with global network connectivity. */
    CONNECTED_GLOBAL(70);

    private int m_code;

    private NetworkState(int code) {
        this.m_code = code;
    }

    public static NetworkState parseCode(int code) {
        for (NetworkState state : NetworkState.values()) {
            if (state.m_code == code) {
                return state;
            }
        }

        return null;
    }
}
