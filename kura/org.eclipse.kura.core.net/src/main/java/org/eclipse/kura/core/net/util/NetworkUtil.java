/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.net.util;

import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetworkUtil {
	
	private static final Logger s_logger = LoggerFactory.getLogger(NetworkUtil.class);

	public static String calculateNetwork(String ipAddress, String netmask) {
		int ipAddressValue = 0;
		int netmaskValue = 0;

		StringTokenizer st = new StringTokenizer(ipAddress, ".");
		for (int i = 24; i >= 0; i -= 8) {
			ipAddressValue = ipAddressValue | (Integer.parseInt(st.nextToken()) << i);
		}

		st = new StringTokenizer(netmask, ".");
		for (int i = 24; i >= 0; i -= 8) {
			netmaskValue = netmaskValue | (Integer.parseInt(st.nextToken()) << i);
		}

		int network = ipAddressValue & netmaskValue;
		return dottedQuad(network);
	}

	public static String calculateBroadcast(String ipAddress, String netmask) {
		int ipAddressValue = 0;
		int netmaskValue = 0;

		StringTokenizer st = new StringTokenizer(ipAddress, ".");
		for (int i = 24; i >= 0; i -= 8) {
			ipAddressValue = ipAddressValue | (Integer.parseInt(st.nextToken()) << i);
		}

		st = new StringTokenizer(netmask, ".");
		for (int i = 24; i >= 0; i -= 8) {
			netmaskValue = netmaskValue | (Integer.parseInt(st.nextToken()) << i);
		}

		int network = ipAddressValue | (~netmaskValue);
		return dottedQuad(network);
	}
	
	public static String getNetmaskStringForm(int prefix) throws KuraException {
		if(prefix >= 0 && prefix <=32) {
			int mask = ~((1 << (32 - prefix)) - 1);
			return dottedQuad(mask);
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "invalid prefix ");
		}
	}

	public static short getNetmaskShortForm(String netmask) throws KuraException {
		if(netmask == null) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "netmask is null");
		}
		
		int netmaskValue = 0;
		StringTokenizer st = new StringTokenizer(netmask, ".");
		for (int i = 24; i >= 0; i -= 8) {
			netmaskValue = netmaskValue | (Integer.parseInt(st.nextToken()) << i);
		}

		boolean hitZero = false;
		int displayMask = 1 << 31;
		int count = 0;

		for (int c = 1; c<=32; c++) {
			if((netmaskValue & displayMask) == 0) {
				hitZero=true;
			} else {
				if(hitZero) {
					s_logger.error("received invalid mask: " + netmask);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "received invalid mask: " + netmask);
				}

				count++;
			}

			netmaskValue <<= 1;
		}

		return (short)count;
	}

	public static String dottedQuad(int ip) {
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			// process 3 bytes, from high order byte down.
			sb.append(Integer.toString((ip >>> shift) & 0xff));
			sb.append('.');
		}

		sb.append(Integer.toString(ip & 0xff));
		return sb.toString();
	}
	
	public static int convertIp4Address(String ipAddress) {
		String[] splitIpAddress = ipAddress.split("\\.");
        short[] addressBytes = new short[4];

        for(int i=0; i<4; i++) {
                String octet = splitIpAddress[i];
                addressBytes[i] = Short.parseShort(octet);
        }

        return NetworkUtil.packIp4AddressBytes(addressBytes);
	}
	
	public static int packIp4AddressBytes(short[] bytes) {
		int val = 0;
		for (int i = 3; i >=0; i--) {
			val = (val << 8);
			val |= bytes[i];
		}
		return val;
	}

	public static short[] unpackIP4AddressInt(int address) {
		return new short[]{(short)(address & 0xFF), (short)((address >> 8) & 0xFF), (short)((address >> 16) & 0xFF), (short)((address >> 24) & 0xFF)};
	}
	
    public static byte[] convertIP6Address(String fullFormIP6Address) {
        byte[] retVal = new byte[16];
        String[] ip6Split = fullFormIP6Address.split(":");
        for(int i=0; i<8; i++) {;
                String octet = ip6Split[i];
                StringBuffer sb = new StringBuffer();

                while((sb.length()+octet.length()) < 4) {
                        sb.append("0");
                }  
                sb.append(octet);

                retVal[i*2] = (byte)Short.parseShort(sb.toString().substring(0,2),16);
                retVal[i*2+1] = (byte)Short.parseShort(sb.toString().substring(2,4),16);
        }
                
        return retVal;
    }
    
    public static String convertIP6Address(byte[] bytes) {
    	StringBuffer sb = new StringBuffer();
    	for(int i=0; i<16; i=i+2) {
       		sb.append(Integer.toHexString(0xFF & bytes[i]));
       		sb.append(Integer.toHexString(0xFF & bytes[i+1]));
       		if(i!=14) {
       			sb.append(":");
       		}
       	}
    	return sb.toString();
    }
    
	public static String macToString(byte[] mac) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<6; i++) {
			String octet = (Integer.toHexString(mac[i] & 0xFF)).toUpperCase();
			if(octet.length()==1) {
				sb.append("0");
			}
			sb.append(octet);
			if(i != 5) {
				sb.append(":");
			}
		}
		return sb.toString();
	}

	public static byte[] macToBytes(String mac) {
		StringTokenizer st = new StringTokenizer(mac,":");
		byte[] bytes = new byte[6];
		for(int i=0; i<6; i++) {
			bytes[i] = (byte) Integer.parseInt(st.nextToken(), 16);
		}
		
		return bytes;
	}
}
