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
    private final Optional<Boolean> disabled;
    private final Optional<Float> attenuation;
    private final Optional<Boolean> noInitiatingRadiation;
    private final Optional<Boolean> radarDetection;

    private WifiChannel(Builder builder) {
        this.channel = builder.channel;
        this.frequency = builder.frequency;
        this.disabled = builder.disabled;
        this.attenuation = builder.attenuation;
        this.noInitiatingRadiation = builder.noInitiatingRadiation;
        this.radarDetection = builder.radarDetection;
    }

    public int getChannel() {
        return this.channel;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public Optional<Float> getAttenuation() {
        return this.attenuation;
    }

    public Optional<Boolean> getNoInitiatingRadiation() {
        return this.noInitiatingRadiation;
    }

    public Optional<Boolean> getRadarDetection() {
        return this.radarDetection;
    }

    public Optional<Boolean> getDisabled() {
        return this.disabled;
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
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        WifiChannel other = (WifiChannel) obj;
        return Objects.equals(this.attenuation, other.attenuation) && this.channel == other.channel
                && Objects.equals(this.disabled, other.disabled) && this.frequency == other.frequency
                && Objects.equals(this.noInitiatingRadiation, other.noInitiatingRadiation)
                && Objects.equals(this.radarDetection, other.radarDetection);
    }

    public static Builder builder(final int channel, final int frequency) {
        return new Builder(channel, frequency);
    }

    public static final class Builder {

        private int channel;
        private int frequency;
        private Optional<Boolean> disabled = Optional.empty();
        private Optional<Float> attenuation = Optional.empty();
        private Optional<Boolean> noInitiatingRadiation = Optional.empty();
        private Optional<Boolean> radarDetection = Optional.empty();

        private Builder(final int channel, final int frequency) {
            this.channel = channel;
            this.frequency = frequency;
        }

        public Builder withChannel(int channel) {
            this.channel = channel;
            return this;
        }

        public Builder withFrequency(int frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder withDisabled(final boolean disabled) {
            this.disabled = Optional.of(disabled);
            return this;
        }

        public Builder withAttenuation(final float attenuation) {
            this.attenuation = Optional.of(attenuation);
            return this;
        }

        public Builder withNoInitiatingRadiation(final boolean noInitiatingRadiation) {
            this.noInitiatingRadiation = Optional.of(noInitiatingRadiation);
            return this;
        }

        public Builder withRadarDetection(final boolean radarDetection) {
            this.radarDetection = Optional.of(radarDetection);
            return this;
        }

        public WifiChannel build() {
            return new WifiChannel(this);
        }
    }

}
