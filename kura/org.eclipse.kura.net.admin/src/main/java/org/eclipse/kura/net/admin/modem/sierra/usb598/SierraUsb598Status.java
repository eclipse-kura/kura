/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

    // status hash tables
    private static Map<Integer, String> ChannelState = new Hashtable<>(); // channel state
    private static Map<Integer, String> BandClass = new Hashtable<>(); // current band class
    private static Map<Integer, String> ActivationStatus = new Hashtable<>(); // activation
    private static Map<Integer, String> RoamingStatus = new Hashtable<>(); // roaming
    private static Map<Integer, String> ServiceIndication = new Hashtable<>(); // service
    // indication
    private static Map<Integer, String> CallStatus = new Hashtable<>(); // call status
    private static Map<Integer, String> PowerMode = new Hashtable<>(); // power mode

    static {
        // channel state
        ChannelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_NOT_ACQUIRED.getStatusCode()),
                "Not Acquired");
        ChannelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_ACQUIRED.getStatusCode()), "Acquired");
        ChannelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_SCANNING.getStatusCode()), "Scanning");

        // current band class
        BandClass.put(Integer.valueOf(SierraUsb598StatusCodes.BANDCLASS_CELLULAR.getStatusCode()), "Cellular");
        BandClass.put(Integer.valueOf(SierraUsb598StatusCodes.BANDCLASS_PCS.getStatusCode()), new String("PCS"));

        // activation status
        ActivationStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ACTSTAT_NOT_ACTIVATED.getStatusCode()),
                "not activated");
        ActivationStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ACTSTAT_ACTIVATED.getStatusCode()), "activated");

        // roaming status
        RoamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_NOT_ROAMING.getStatusCode()), "Not roaming");
        RoamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_W_SID.getStatusCode()),
                "Roaming with guaranteed SID");
        RoamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_WO_SID.getStatusCode()),
                "Roaming w/o guaranteed SID");

        // service indication
        ServiceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_NO.getStatusCode()), "No service");
        ServiceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_CDMA.getStatusCode()), "Digital CDMA");
        ServiceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_GPS.getStatusCode()), "GPS");

        // call status
        CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_DISCONNECTED.getStatusCode()), "Disconnected");
        CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_CONNECTING.getStatusCode()), "Connecting");
        CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_CONNECTED.getStatusCode()), "Connected");
        CallStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_DORMANT.getStatusCode()),
                "Dormant Packet Call");

        // power mode
        PowerMode.put(Integer.valueOf(SierraUsb598StatusCodes.PMODE_LPM.getStatusCode()), "Low Power Mode");
        PowerMode.put(Integer.valueOf(SierraUsb598StatusCodes.PMODE_ONLINE.getStatusCode()), "Online");

    }

    /**
     * Reports channel state
     *
     * @param chanState
     * @return channel state
     */
    public static String getChannelState(int chanState) {
        Object o = ChannelState.get(Integer.valueOf(chanState));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports band class
     *
     * @param bandClass
     * @return band class
     */
    public static String getBandClass(int bandClass) {
        Object o = BandClass.get(Integer.valueOf(bandClass));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports roaming status
     *
     * @param roamingStatus
     * @return roaming status
     */
    public static String getRoamingStatus(int roamingStatus) {
        Object o = RoamingStatus.get(Integer.valueOf(roamingStatus));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports activation status
     *
     * @param activationStatus
     *            as <code>String</code>
     * @return
     */
    public static String getActivationStatus(int activationStatus) {
        Object o = ActivationStatus.get(Integer.valueOf(activationStatus));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports service indication
     *
     * @param serviceIndication
     * @return service indication
     */
    public static String getServiceIndication(int serviceIndication) {
        Object o = ServiceIndication.get(Integer.valueOf(serviceIndication));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports call status
     *
     * @param callStatus
     * @return call status
     */
    public static String getCallStatus(int callStatus) {
        Object o = CallStatus.get(Integer.valueOf(callStatus));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports Power Mode
     *
     * @param powerMode
     * @return power mode string
     */
    public static String getPowerMode(int powerMode) {
        Object o = PowerMode.get(Integer.valueOf(powerMode));
        return o != null ? (String) o : NOT_AVAIL;
    }
}
