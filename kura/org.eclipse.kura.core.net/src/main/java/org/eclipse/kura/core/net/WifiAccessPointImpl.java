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
package org.eclipse.kura.core.net;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;

public class WifiAccessPointImpl implements WifiAccessPoint 
{
	private String            ssid;
	private byte[]            hardwareAddress;
	private long              frequency;
	private WifiMode          mode;
	private List<Long>        bitrate;
	private int               strength;
	private EnumSet<WifiSecurity> wpaSecurity;
	private EnumSet<WifiSecurity> rsnSecurity;
	private List<String> capabilities;
	
	public WifiAccessPointImpl(String ssid) {
		this.ssid = ssid;
	}
	
	public String getSSID() {
		return ssid;
	}

	public byte[] getHardwareAddress() {
		return hardwareAddress;
	}

	public void setHardwareAddress(byte[] hardwareAddress) {
		this.hardwareAddress = hardwareAddress;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public WifiMode getMode() {
		return mode;
	}

	public void setMode(WifiMode mode) {
		this.mode = mode;
	}

	public List<Long> getBitrate() {
		return bitrate;
	}

	public void setBitrate(List<Long> bitrate) {
		this.bitrate = bitrate;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = strength;
	}

	public EnumSet<WifiSecurity> getWpaSecurity() {
		return wpaSecurity;
	}

	public void setWpaSecurity(EnumSet<WifiSecurity> wpaSecurity) {
		this.wpaSecurity = wpaSecurity;
	}

	public EnumSet<WifiSecurity> getRsnSecurity() {
		return rsnSecurity;
	}

	public void setRsnSecurity(EnumSet<WifiSecurity> rsnSecurity) {
		this.rsnSecurity = rsnSecurity;
	}
	
	public List<String> getCapabilities() {
		return this.capabilities;
	}
	
	public void setCapabilities(List<String> capabilities) {
		this.capabilities = capabilities;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ssid=").append(ssid);
		if(hardwareAddress != null && hardwareAddress.length > 0) {
			sb.append(" :: hardwareAddress=")
			.append(NetworkUtil.macToString(hardwareAddress));
		}
		sb.append(" :: frequency=").append(frequency)
		.append(" :: mode=").append(mode);
		if(bitrate != null && bitrate.size() > 0) {
			sb.append(" :: bitrate=");
			for(Long rate : bitrate) {
				sb.append(rate).append(" ");
			}
		}
		sb.append(" :: strength=").append(strength);
		if(wpaSecurity != null && wpaSecurity.size() > 0) {
			sb.append(" :: wpaSecurity=");
			for(WifiSecurity security : wpaSecurity) {
				sb.append(security).append(" ");
			}
		}
		if(rsnSecurity != null && rsnSecurity.size() > 0) {
			sb.append(" :: rsnSecurity=");
			for(WifiSecurity security : rsnSecurity) {
				sb.append(security).append(" ");
			}
		}
		return sb.toString();
	}
}
