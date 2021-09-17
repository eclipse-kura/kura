/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

public class GwtWifiChannelModel extends GwtBaseModel {

    private static final long serialVersionUID = -1471520645150788770L;

    private static final String BAND2400MHZ = "2.4 GHz";
    private static final String BAND5000MHZ = "5.0 GHz";

    private static final int FIRST_2400MHZ_CHANNEL = 1;
    private static final int LAST_2400MHZ_CHANNEL = 14;

    private static final int FIRST_5000MHZ_CHANNEL = 32;

    public GwtWifiChannelModel() {

    }

    public GwtWifiChannelModel(int channel, int frequency) {

        set("name", formChannelName(channel));
        set("channel", channel);
        set("frequency", frequency);

        String band = null;

        if (channel >= FIRST_2400MHZ_CHANNEL && channel <= LAST_2400MHZ_CHANNEL)
            band = BAND2400MHZ;
        else if (channel >= FIRST_5000MHZ_CHANNEL)
            band = BAND5000MHZ;

        set("band", band);
    }

    public String getName() {
        return (String) get("name");
    }

    public int getChannel() {
        Integer iChannel = (Integer) get("channel");
        return iChannel.intValue();
    }

    public int getFrequency() {
        Integer iFrequency = (Integer) get("frequency");
        return iFrequency.intValue();
    }

    public String getBand() {
        return get("band");
    }

    public void setBand(String band) {
        set("band", band);
    }

    @Override
    public String toString() {
        return getName();
    }

    private static String formChannelName(int channel) {

        StringBuilder sb = new StringBuilder();
        sb.append("Channel ");
        sb.append(channel);
        return sb.toString();
    }

}
