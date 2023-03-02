/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.status.wifi;

import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represent a WiFi channel, providing the channel number, frequency,
 * status and other useful informations.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiChannel {

    private final int channel;
    private final int frequency;
    private Optional<Boolean> disabled;
    private Optional<Float> attenuation;
    private Optional<Boolean> noInitiatingRadiation;
    private Optional<Boolean> radarDetection;

    public WifiChannel(int channel, int frequency) {
        this.channel = channel;
        this.frequency = frequency;
        this.attenuation = Optional.empty();
        this.noInitiatingRadiation = Optional.empty();
        this.radarDetection = Optional.empty();
        this.disabled = Optional.empty();
    }

    public Integer getChannel() {
        return this.channel;
    }

    public Integer getFrequency() {
        return this.frequency;
    }

    public Optional<Float> getAttenuation() {
        return this.attenuation;
    }

    public void setAttenuation(Float attenuation) {
        this.attenuation = Optional.of(attenuation);
    }

    public Optional<Boolean> getNoInitiatingRadiation() {
        return this.noInitiatingRadiation;
    }

    public void setNoInitiatingRadiation(Boolean noInitiatingRadiation) {
        this.noInitiatingRadiation = Optional.of(noInitiatingRadiation);
    }

    public Optional<Boolean> getRadarDetection() {
        return this.radarDetection;
    }

    public void setRadarDetection(Boolean radarDetection) {
        this.radarDetection = Optional.of(radarDetection);
    }

    public Optional<Boolean> getDisabled() {
        return this.disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = Optional.of(disabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.attenuation, this.channel, this.disabled, this.frequency, this.noInitiatingRadiation,
                this.radarDetection);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        WifiChannel other = (WifiChannel) obj;
        return Objects.equals(this.attenuation, other.attenuation) && this.channel == other.channel
                && Objects.equals(this.disabled, other.disabled) && this.frequency == other.frequency
                && Objects.equals(this.noInitiatingRadiation, other.noInitiatingRadiation)
                && Objects.equals(this.radarDetection, other.radarDetection);
    }

}
