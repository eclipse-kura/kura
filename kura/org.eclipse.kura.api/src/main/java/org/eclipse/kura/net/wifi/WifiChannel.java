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
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.net.wifi;

import java.util.Objects;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Wifi channel and Frequency in MHz
 * 
 * @since 2.2
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiChannel {

    /** Wifi channel **/
    private Integer channel;

    /** Wifi frequency in MHz **/
    private Integer frequency;

    private Float attenuation;

    private Boolean noInitiatingRadiation;

    private Boolean radarDetection;

    private Boolean disabled;

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

    /**
     * @since 2.3
     */
    public Boolean isNoInitiatingRadiation() {
        return noInitiatingRadiation;
    }

    /**
     * @since 2.3
     */
    public void setNoInitiatingRadiation(Boolean noInitiatingRadiation) {
        this.noInitiatingRadiation = noInitiatingRadiation;
    }

    /**
     * @since 2.3
     */
    public Boolean isRadarDetection() {
        return radarDetection;
    }

    /**
     * @since 2.3
     */
    public void setRadarDetection(Boolean radarDetection) {
        this.radarDetection = radarDetection;
    }

    /**
     * @since 2.3
     */
    public Boolean isDisabled() {
        return disabled;
    }

    /**
     * @since 2.3
     */
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @since 2.3
     */
    public Float getAttenuation() {
        return attenuation;
    }

    /**
     * @since 2.3
     */
    public void setAttenuation(Float attenuation) {
        this.attenuation = attenuation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, frequency);
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
        WifiChannel other = (WifiChannel) obj;
        return Objects.equals(channel, other.channel) && Objects.equals(frequency, other.frequency);
    }

    @Override
    public String toString() {
        return "WifiChannel [channel=" + channel + ", frequency=" + frequency + ", attenuation=" + attenuation
                + ", noInitiatingRadiation=" + noInitiatingRadiation + ", radarDetection=" + radarDetection
                + ", disabled=" + disabled + "]";
    }

}
