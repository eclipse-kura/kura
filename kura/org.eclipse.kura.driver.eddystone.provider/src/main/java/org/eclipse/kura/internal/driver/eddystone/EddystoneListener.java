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
package org.eclipse.kura.internal.driver.eddystone;

import org.eclipse.kura.channel.listener.ChannelListener;

public class EddystoneListener {

    private final String channelName;
    private final ChannelListener listener;
    private final EddystoneFrameType eddystoneFrameType;

    public EddystoneListener(String channelName, ChannelListener listener, EddystoneFrameType eddystoneFrameType) {
        this.channelName = channelName;
        this.listener = listener;
        this.eddystoneFrameType = eddystoneFrameType;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public ChannelListener getListener() {
        return this.listener;
    }

    public EddystoneFrameType getEddystoneFrameType() {
        return this.eddystoneFrameType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.channelName == null ? 0 : this.channelName.hashCode());
        result = prime * result + (this.eddystoneFrameType == null ? 0 : this.eddystoneFrameType.hashCode());
        result = prime * result + (this.listener == null ? 0 : this.listener.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EddystoneListener other = (EddystoneListener) obj;
        if (this.channelName == null) {
            if (other.channelName != null) {
                return false;
            }
        } else if (!this.channelName.equals(other.channelName)) {
            return false;
        }
        if (this.eddystoneFrameType != other.eddystoneFrameType) {
            return false;
        }
        if (this.listener == null) {
            if (other.listener != null) {
                return false;
            }
        } else if (!this.listener.equals(other.listener)) {
            return false;
        }
        return true;
    }

}
