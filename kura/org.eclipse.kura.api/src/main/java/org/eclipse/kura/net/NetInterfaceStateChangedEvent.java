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
package org.eclipse.kura.net;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * Event raised when the state of a network interface has changed.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetInterfaceStateChangedEvent extends Event {

    /** Topic of the NetworkStateChangedEvent */
    public static final String NETWORK_EVENT_INTERFACE_STATE_CHANGED_TOPIC = "org/eclipse/kura/net/NetworkEvent/interface/STATE_CHANGED";

    /** Name of the property to access the network interface name */
    public static final String NETWORK_EVENT_INTERFACE_PROPERTY = "network.interface";

    /** Name of the property to access the new network state */
    public static final String NETWORK_EVENT_NEW_STATE_PROPERTY = "network.state.new";

    /** Name of the property to access the old network state */
    public static final String NETWORK_EVENT_OLD_STATE_PROPERTY = "network.state.old";

    /** Name of the property to access the reason of the change */
    public static final String NETWORK_EVENT_STATE_CHANGE_REASON_PROPERTY = "network.state.change.reason";

    public NetInterfaceStateChangedEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_INTERFACE_STATE_CHANGED_TOPIC, properties);
    }

    /**
     * Returns the network interface name.
     *
     * @return
     */
    public String getInterfaceName() {
        return (String) getProperty(NETWORK_EVENT_INTERFACE_PROPERTY);
    }

    /**
     * Returns the new network interface state.
     *
     * @return
     */
    public NetInterfaceState getNewState() {
        return (NetInterfaceState) getProperty(NETWORK_EVENT_NEW_STATE_PROPERTY);
    }

    /**
     * Returns the old network interface state.
     *
     * @return
     */
    public NetInterfaceState getOldState() {
        return (NetInterfaceState) getProperty(NETWORK_EVENT_OLD_STATE_PROPERTY);
    }

    /**
     * Returns the reason for the state transition.
     *
     * @return
     */
    public Reason getReason() {
        return (Reason) getProperty(NETWORK_EVENT_STATE_CHANGE_REASON_PROPERTY);
    }

    public enum Reason {
        /** The reason for the device state change is unknown. */
        REASON_UNKNOWN,

        /** The state change is normal. */
        REASON_NONE,

        /** The device is now managed. */
        REASON_NOW_MANAGED,

        /** The device is no longer managed. */
        REASON_NOW_UNMANAGED,

        /** The device could not be readied for configuration. */
        REASON_CONFIG_FAILED,

        /** IP configuration could not be reserved (no available address, timeout, etc). */
        REASON_CONFIG_UNAVAILABLE,

        /** The IP configuration is no longer valid. */
        REASON_CONFIG_EXPIRED,

        /** Secrets were required, but not provided. */
        REASON_NO_SECRETS,

        /** The 802.1X supplicant disconnected from the access point or authentication server. */
        REASON_SUPPLICANT_DISCONNECT,

        /** Configuration of the 802.1X supplicant failed. */
        REASON_SUPPLICANT_CONFIG_FAILED,

        /** The 802.1X supplicant quit or failed unexpectedly. */
        REASON_SUPPLICANT_FAILED,

        /** The 802.1X supplicant took too long to authenticate. */
        REASON_SUPPLICANT_TIMEOUT,

        /** The PPP service failed to start within the allowed time. */
        REASON_PPP_START_FAILED,

        /** The PPP service disconnected unexpectedly. */
        REASON_PPP_DISCONNECT,

        /** The PPP service quit or failed unexpectedly. */
        REASON_PPP_FAILED,

        /** The DHCP service failed to start within the allowed time. */
        REASON_DHCP_START_FAILED,

        /** The DHCP service reported an unexpected error. */
        REASON_DHCP_ERROR,

        /** The DHCP service quit or failed unexpectedly. */
        REASON_DHCP_FAILED,

        /** The shared connection service failed to start. */
        REASON_SHARED_START_FAILED,

        /** The shared connection service quit or failed unexpectedly. */
        REASON_SHARED_FAILED,

        /** The AutoIP service failed to start. */
        REASON_AUTOIP_START_FAILED,

        /** The AutoIP service reported an unexpected error. */
        REASON_AUTOIP_ERROR,

        /** The AutoIP service quit or failed unexpectedly. */
        REASON_AUTOIP_FAILED,

        /** Dialing failed because the line was busy. */
        REASON_MODEM_BUSY,

        /** Dialing failed because there was no dial tone. */
        REASON_MODEM_NO_DIAL_TONE,

        /** Dialing failed because there was carrier. */
        REASON_MODEM_NO_CARRIER,

        /** Dialing timed out. */
        REASON_MODEM_DIAL_TIMEOUT,

        /** Dialing failed. */
        REASON_MODEM_DIAL_FAILED,

        /** Modem initialization failed. */
        REASON_MODEM_INIT_FAILED,

        /** Failed to select the specified GSM APN. */
        REASON_GSM_APN_FAILED,

        /** Not searching for networks. */
        REASON_GSM_REGISTRATION_NOT_SEARCHING,

        /** Network registration was denied.* */
        REASON_GSM_REGISTRATION_DENIED,

        /** Network registration timed out. */
        REASON_GSM_REGISTRATION_TIMEOUT,

        /** Failed to register with the requested GSM network. */
        REASON_GSM_REGISTRATION_FAILED,

        /** PIN check failed. */
        REASON_GSM_PIN_CHECK_FAILED,

        /** Necessary firmware for the device may be missing. */
        REASON_FIRMWARE_MISSING,

        /** The device was removed. */
        REASON_REMOVED,

        /** NetworkManager went to sleep. */
        REASON_SLEEPING,

        /** The device's active connection was removed or disappeared. */
        REASON_CONNECTION_REMOVED,

        /** A user or client requested the disconnection. */
        REASON_USER_REQUESTED,

        /** The device's carrier/link changed. */
        REASON_CARRIER,

        /** The device's existing connection was assumed. */
        REASON_CONNECTION_ASSUMED,

        /** The 802.1x supplicant is now available. */
        REASON_SUPPLICANT_AVAILABLE,

        /** The modem could not be found. */
        REASON_MODEM_NOT_FOUND,

        /** The Bluetooth connection timed out or failed. */
        REASON_BT_FAILED,

        /** GSM Modem's SIM Card not inserted. */
        REASON_GSM_SIM_NOT_INSERTED,

        /** GSM Modem's SIM Pin required. */
        REASON_GSM_SIM_PIN_REQUIRED,

        /** GSM Modem's SIM Puk required. */
        REASON_GSM_SIM_PUK_REQUIRED,

        /** GSM Modem's SIM wrong */
        REASON_GSM_SIM_WRONG,

        /** InfiniBand device does not support connected mode. */
        REASON_INFINIBAND_MODE,

        /** A dependency of the connection failed. */
        REASON_DEPENDENCY_FAILED,

        /** Problem with the RFC 2684 Ethernet over ADSL bridge. */
        REASON_BR2684_FAILED;
    }
}
