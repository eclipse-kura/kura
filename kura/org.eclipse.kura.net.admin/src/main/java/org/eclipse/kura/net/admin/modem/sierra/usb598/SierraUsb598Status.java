/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.modem.sierra.usb598;

import java.util.Hashtable;
import java.util.Map;

public class SierraUsb598Status {

    private static final String NOT_AVAIL = "N/A";

    // status hash tables
    private static Map<Integer, String> channelState = new Hashtable<>(); // channel state
    private static Map<Integer, String> bandClass = new Hashtable<>(); // current band class
    private static Map<Integer, String> activationStatus = new Hashtable<>(); // activation
    private static Map<Integer, String> roamingStatus = new Hashtable<>(); // roaming
    private static Map<Integer, String> serviceIndication = new Hashtable<>(); // service
    // indication
    private static Map<Integer, String> callStatus = new Hashtable<>(); // call status
    private static Map<Integer, String> powerMode = new Hashtable<>(); // power mode

    static {
        // channel state
        channelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_NOT_ACQUIRED.getStatusCode()),
                "Not Acquired");
        channelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_ACQUIRED.getStatusCode()), "Acquired");
        channelState.put(Integer.valueOf(SierraUsb598StatusCodes.CHANSTATE_SCANNING.getStatusCode()), "Scanning");

        // current band class
        bandClass.put(Integer.valueOf(SierraUsb598StatusCodes.BANDCLASS_CELLULAR.getStatusCode()), "Cellular");
        bandClass.put(Integer.valueOf(SierraUsb598StatusCodes.BANDCLASS_PCS.getStatusCode()), new String("PCS"));

        // activation status
        activationStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ACTSTAT_NOT_ACTIVATED.getStatusCode()),
                "not activated");
        activationStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ACTSTAT_ACTIVATED.getStatusCode()), "activated");

        // roaming status
        roamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_NOT_ROAMING.getStatusCode()), "Not roaming");
        roamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_W_SID.getStatusCode()),
                "Roaming with guaranteed SID");
        roamingStatus.put(Integer.valueOf(SierraUsb598StatusCodes.ROAMSTAT_WO_SID.getStatusCode()),
                "Roaming w/o guaranteed SID");

        // service indication
        serviceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_NO.getStatusCode()), "No service");
        serviceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_CDMA.getStatusCode()), "Digital CDMA");
        serviceIndication.put(Integer.valueOf(SierraUsb598StatusCodes.SRVCIND_GPS.getStatusCode()), "GPS");

        // call status
        callStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_DISCONNECTED.getStatusCode()), "Disconnected");
        callStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_CONNECTING.getStatusCode()), "Connecting");
        callStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_CONNECTED.getStatusCode()), "Connected");
        callStatus.put(Integer.valueOf(SierraUsb598StatusCodes.CALLSTAT_DORMANT.getStatusCode()),
                "Dormant Packet Call");

        // power mode
        powerMode.put(Integer.valueOf(SierraUsb598StatusCodes.PMODE_LPM.getStatusCode()), "Low Power Mode");
        powerMode.put(Integer.valueOf(SierraUsb598StatusCodes.PMODE_ONLINE.getStatusCode()), "Online");

    }
    
    private SierraUsb598Status() {
        
    }

    /**
     * Reports channel state
     *
     * @param chanState
     * @return channel state
     */
    public static String getChannelState(int chanState) {
        Object o = channelState.get(Integer.valueOf(chanState));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports band class
     *
     * @param bandClass
     * @return band class
     */
    public static String getBandClass(int bandClass) {
        Object o = SierraUsb598Status.bandClass.get(Integer.valueOf(bandClass));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports roaming status
     *
     * @param roamingStatus
     * @return roaming status
     */
    public static String getRoamingStatus(int roamingStatus) {
        Object o = SierraUsb598Status.roamingStatus.get(Integer.valueOf(roamingStatus));
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
        Object o = SierraUsb598Status.activationStatus.get(Integer.valueOf(activationStatus));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports service indication
     *
     * @param serviceIndication
     * @return service indication
     */
    public static String getServiceIndication(int serviceIndication) {
        Object o = SierraUsb598Status.serviceIndication.get(Integer.valueOf(serviceIndication));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports call status
     *
     * @param callStatus
     * @return call status
     */
    public static String getCallStatus(int callStatus) {
        Object o = SierraUsb598Status.callStatus.get(Integer.valueOf(callStatus));
        return o != null ? (String) o : NOT_AVAIL;
    }

    /**
     * Reports Power Mode
     *
     * @param powerMode
     * @return power mode string
     */
    public static String getPowerMode(int powerMode) {
        Object o = SierraUsb598Status.powerMode.get(Integer.valueOf(powerMode));
        return o != null ? (String) o : NOT_AVAIL;
    }
}
