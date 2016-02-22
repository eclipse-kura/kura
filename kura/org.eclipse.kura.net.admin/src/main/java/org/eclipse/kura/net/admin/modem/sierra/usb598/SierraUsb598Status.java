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
package org.eclipse.kura.net.admin.modem.sierra.usb598;

import java.util.Hashtable;
import java.util.Map;

public class SierraUsb598Status {

	private static final String NOT_AVAIL = "N/A";
	/*
	private enum SierraUsb598StatusCodes {
		
		NOT_AVAILABLE(-1),

		// channel state return codes
		CHANSTATE_NOT_ACQUIRED(0x0000),
		CHANSTATE_ACQUIRED(0x0001),
		CHANSTATE_SCANNING(0x0005),

		// current band class return codes
		BANDCLASS_CELLULAR(0x0000),
		BANDCLASS_PCS(0x0001),

		// activation status return codes
		ACTSTAT_NOT_ACTIVATED(0x0000),
		ACTSTAT_ACTIVATED(0x0001),

		// roaming status return codes
		ROAMSTAT_NOT_ROAMING(0x0000),
		ROAMSTAT_W_SID(0x0001),
		ROAMSTAT_WO_SID(0x0002),

		// service indication return codes
		SRVCIND_NO(0x0000),
		SRVCIND_CDMA(0x0002),
		SRVCIND_GPS(0x0003),

		// call status return codes
		CALLSTAT_DISCONNECTED(0),
		CALLSTAT_CONNECTING(1),
		CALLSTAT_CONNECTED(2),
		CALLSTAT_DORMANT(3),

		// power mode return codes
		PMODE_LPM(0x0000), // Low Power Mode
		PMODE_ONLINE(0x0001); // Online
		
		private int m_statusCode = 0;
		
		private SierraUsb598StatusCodes(int statusCode) {
			m_statusCode = statusCode;
		}
		
		public int getStatusCode () {
			return m_statusCode;
		}
	}
	*/

	// status hash tables
	private static Map<Integer, String> s_ChannelState = new Hashtable<Integer, String>(); // channel state
	private static Map<Integer, String> s_BandClass = new Hashtable<Integer, String>(); // current band class
	private static Map<Integer, String> s_ActivationStatus = new Hashtable<Integer, String>(); // activation
	private static Map<Integer, String> s_RoamingStatus = new Hashtable<Integer, String>(); // roaming
	private static Map<Integer, String> s_ServiceIndication = new Hashtable<Integer, String>(); // service
															// indication
	private static Map<Integer, String> s_CallStatus = new Hashtable<Integer, String>(); // call status
	private static Map<Integer, String> s_PowerMode = new Hashtable<Integer, String>(); // power mode

	static {
		// channel state
		s_ChannelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_NOT_ACQUIRED.getStatusCode()), new String(
				"Not Acquired"));
		s_ChannelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_ACQUIRED.getStatusCode()), new String(
				"Acquired"));
		s_ChannelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_SCANNING.getStatusCode()), new String(
				"Scanning"));

		// current band class
		s_BandClass
				.put(Integer.valueOf(SierraUsb598StatusCodes.BANDCLASS_CELLULAR.getStatusCode()), new String("Cellular"));
		s_BandClass.put(Integer.valueOf(SierraUsb598StatusCodes.BANDCLASS_PCS.getStatusCode()), new String("PCS"));

		// activation status
		s_ActivationStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ACTSTAT_NOT_ACTIVATED.getStatusCode()), new String(
				"not activated"));
		s_ActivationStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ACTSTAT_ACTIVATED.getStatusCode()), new String(
				"activated"));

		// roaming status
		s_RoamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_NOT_ROAMING.getStatusCode()), new String(
				"Not roaming"));
		s_RoamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_W_SID.getStatusCode()), new String(
				"Roaming with guaranteed SID"));
		s_RoamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_WO_SID.getStatusCode()), new String(
				"Roaming w/o guaranteed SID"));

		// service indication
		s_ServiceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_NO.getStatusCode()), new String(
				"No service"));
		s_ServiceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_CDMA.getStatusCode()), new String(
				"Digital CDMA"));
		s_ServiceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_GPS.getStatusCode()), new String("GPS"));

		// call status
		s_CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_DISCONNECTED.getStatusCode()), new String(
				"Disconnected"));
		s_CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_CONNECTING.getStatusCode()), new String(
				"Connecting"));
		s_CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_CONNECTED.getStatusCode()), new String(
				"Connected"));
		s_CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_DORMANT.getStatusCode()), new String(
				"Dormant Packet Call"));

		// power mode
		s_PowerMode.put(Integer.valueOf(SierraUsb598StatusCodes.PMODE_LPM.getStatusCode()), "Low Power Mode");
		s_PowerMode.put(Integer.valueOf(SierraUsb598StatusCodes.PMODE_ONLINE.getStatusCode()), "Online");

	}

	/**
	 * Reports channel state
	 * 
	 * @param chanState
	 * @return channel state
	 */
	public static String getChannelState(int chanState) {
		Object o = s_ChannelState.get(Integer.valueOf(chanState));
		return (o != null) ? (String) o : NOT_AVAIL;
	}

	/**
	 * Reports band class
	 * 
	 * @param bandClass
	 * @return band class
	 */
	public static String getBandClass(int bandClass) {
		Object o = s_BandClass.get(Integer.valueOf(bandClass));
		return (o != null) ? (String) o : NOT_AVAIL;
	}

	/**
	 * Reports roaming status
	 * 
	 * @param roamingStatus
	 * @return roaming status
	 */
	public static String getRoamingStatus(int roamingStatus) {
		Object o = s_RoamingStatus.get(Integer.valueOf(roamingStatus));
		return (o != null) ? (String) o : NOT_AVAIL;
	}

	/**
	 * Reports activation status
	 * 
	 * @param activationStatus
	 *            as <code>String</code>
	 * @return
	 */
	public static String getActivationStatus(int activationStatus) {
		Object o = s_ActivationStatus.get(Integer.valueOf(activationStatus));
		return (o != null) ? (String) o : NOT_AVAIL;
	}

	/**
	 * Reports service indication
	 * 
	 * @param serviceIndication
	 * @return service indication
	 */
	public static String getServiceIndication(int serviceIndication) {
		Object o = s_ServiceIndication.get(Integer.valueOf(serviceIndication));
		return (o != null) ? (String) o : NOT_AVAIL;
	}

	/**
	 * Reports call status
	 * 
	 * @param callStatus
	 * @return call status
	 */
	public static String getCallStatus(int callStatus) {
		Object o = s_CallStatus.get(Integer.valueOf(callStatus));
		return (o != null) ? (String) o : NOT_AVAIL;
	}

	/**
	 * Reports Power Mode
	 * 
	 * @param powerMode
	 * @return power mode string
	 */
	public static String getPowerMode(int powerMode) {
		Object o = s_PowerMode.get(Integer.valueOf(powerMode));
		return (o != null) ? (String) o : NOT_AVAIL;
	}
}
