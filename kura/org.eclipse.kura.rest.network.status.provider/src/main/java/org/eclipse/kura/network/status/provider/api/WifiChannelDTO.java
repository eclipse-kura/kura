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
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

import org.eclipse.kura.net.status.wifi.WifiChannel;

@SuppressWarnings("unused")
public class WifiChannelDTO {

    private final int channel;
    private final int frequency;
    private final Boolean disabled;
    private final Float attenuation;
    private final Boolean noInitiatingRadiation;
    private final Boolean radarDetection;

    public WifiChannelDTO(final WifiChannel channel) {
        this.channel = channel.getChannel();
        this.frequency = channel.getFrequency();
        this.disabled = channel.getDisabled().orElse(null);
        this.attenuation = channel.getAttenuation().orElse(null);
        this.noInitiatingRadiation = channel.getNoInitiatingRadiation().orElse(null);
        this.radarDetection = channel.getRadarDetection().orElse(null);
    }
}
