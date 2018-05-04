/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.driver.ibeacon;

import org.eclipse.kura.channel.listener.ChannelListener;

public class IBeaconListener {

    private final String channelName;
    private final ChannelListener listener;

    public IBeaconListener(String channelName, ChannelListener listener) {
        this.channelName = channelName;
        this.listener = listener;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public ChannelListener getListener() {
        return this.listener;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
        result = prime * result + ((listener == null) ? 0 : listener.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IBeaconListener other = (IBeaconListener) obj;
        if (channelName == null) {
            if (other.channelName != null)
                return false;
        } else if (!channelName.equals(other.channelName))
            return false;
        if (listener == null) {
            if (other.listener != null)
                return false;
        } else if (!listener.equals(other.listener))
            return false;
        return true;
    }

}