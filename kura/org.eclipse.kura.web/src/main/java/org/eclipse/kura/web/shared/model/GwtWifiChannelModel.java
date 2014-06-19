/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.model;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModel;

public class GwtWifiChannelModel extends BaseModel {
	
	private static final long serialVersionUID = -1471520645150788770L;
	private static final String BAND2400MHZ = "2.4 GHz";
	private static final int FIRST_2400MHZ_CHANNEL = 1;
	private static final int LAST_2400MHZ_CHANNEL = 13;

	public GwtWifiChannelModel () {
		
	}
	
	public GwtWifiChannelModel (String name, int channel, int frequency, String band) {
		
		set("name", name);
		set("channel", channel);
		set("frequency", frequency);
		set("band", band);
	}
	
	public String getName () {
		return (String)get("name");
	}
	
	public int getChannel () {
		Integer iChannel = (Integer)get("channel");
		return iChannel.intValue();
	}
	
	public int getFrequency() {
		Integer iFrequency = (Integer)get("frequency");
		return iFrequency.intValue();
	}
	
	public String getBand() {
		return get("band");
	}
	
	public void setBand(String band) {
		set("band", band);
	}
	
	public static List<GwtWifiChannelModel> getChannels() {
		
		List<GwtWifiChannelModel> alCannels = new ArrayList<GwtWifiChannelModel> ();
		for (int i = FIRST_2400MHZ_CHANNEL; i <= LAST_2400MHZ_CHANNEL; i++) {
			alCannels.add(new GwtWifiChannelModel(formChannelName(i), i, getCannelFrequencyMHz(i), BAND2400MHZ));
		}
		return alCannels;
	}
	
	public String toString() {
		return getName();
	}
	
	private static String formChannelName (int channel) {
		
		StringBuffer sb = new StringBuffer();
		sb.append("Channel ");
		sb.append(channel);
		return sb.toString();
	}
	
	private static int getCannelFrequencyMHz (int channel) {
		
		int frequency = -1;
		if ((channel >=1) && (channel <= 13)) {
			frequency = 2407 + channel * 5;
		}
		return frequency;
	}
}
