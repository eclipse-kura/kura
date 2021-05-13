/*******************************************************************************
 * Copyright (c) 2021 Sterwen-Technology and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Sterwen-Technology
 ******************************************************************************/

package org.eclipse.kura.net.wifi;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Wifi channel and Frequency in MHz
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiChannel {
    /** Wifi channel **/
    private Integer channel;

    /** Wifi frequency in MHz **/
    private Integer frequency;

    public WifiChannel(int channel, Integer frequency) {
        this.channel = channel;
        this.frequency = frequency;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("channel:").append(channel).append(", frequency:").append(frequency).append(" MHz");
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 29;
        int result = super.hashCode();

        result = prime * result + (this.channel == null ? 0 : this.channel.hashCode());
        result = prime * result + (this.frequency == null ? 0 : this.frequency.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WifiChannel)) {
            return false;
        }

        WifiChannel other = (WifiChannel) obj;
        if (!compare(this.channel, other.channel) || !compare(this.frequency, other.frequency)) {
            return false;
        }
        return true;
    }

    private boolean compare(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }
}
