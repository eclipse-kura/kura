/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.util.LinuxIfconfig;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkState;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbDeviceType;
import org.eclipse.kura.usb.UsbService;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class NetworkServiceImplTest {

    private NetworkServiceImpl networkService;
    private UsbService usbService;
    private EventAdmin eventAdmin;
    private LinuxNetworkUtil linuxNetworkUtil;
    private NetworkState networkState;
    private NetInterfaceState netInterfaceState;
    private List<String> interfaceNames;
    private List<NetInterface<? extends NetInterfaceAddress>> networkInterfaces;

    @Test
    public void shouldBeUnknownConnected() throws KuraException {
        givenNetworkService();

        whenGetState();

        thenStateIsUnknownConnected();
    }

    @Test
    public void shouldBeUnknownInterfaceState() throws KuraException {
        givenNetworkService();

        whenGetInterfaceState("eth0");

        thenNetInterfaceStateIsUnknown();
    }

    @Test
    public void shouldListInterfaceNames() throws KuraException {
        givenNetworkService();
        givenMultipleInterfaces(true, true);

        whenGetAllNetworkInterfaceNames();

        thenNetworkNamesListSize(3);
    }

    @Test
    public void shouldListNetworkInterfaces() throws KuraException {
        givenNetworkService();
        givenMultipleInterfaces(true, true);
        List<Event> events = givenModemAddedEvent(6);

        whenReceivedEvents(events);
        whenGetNetworkInterfaces();

        thenNetworkInterfacesListSize(3);
    }

    @Test
    public void shouldAddValidUsbModem() throws KuraException {
        givenNetworkService();
        List<Event> events = givenModemAddedEvent(6);
        givenPppInterface("ppp0", true, true);

        whenReceivedEvents(events);
        whenGetNetworkInterfaces();

        thenNetworkInterfacesListSize(1);
    }

    private void givenNetworkService() {
        this.linuxNetworkUtil = mock(LinuxNetworkUtil.class);
        this.usbService = mock(UsbService.class);
        this.eventAdmin = mock(EventAdmin.class);
        this.networkService = new NetworkServiceImpl();
        this.networkService.setLinuxNetworkUtil(this.linuxNetworkUtil);
        this.networkService.setUsbService(usbService);
        this.networkService.setEventAdmin(eventAdmin);
    }

    private void givenPppInterface(String interfaceName, boolean isUp, boolean isLinkUp) throws KuraException {
        List<String> interfaces = new ArrayList<>();
        interfaces.add(interfaceName);
        when(this.linuxNetworkUtil.getAllInterfaceNames()).thenReturn(interfaces);
        when(this.linuxNetworkUtil.isLinkUp(NetInterfaceType.MODEM, interfaceName)).thenReturn(isLinkUp);

        LinuxIfconfig eth0Ifconfig = new LinuxIfconfig(interfaceName);
        eth0Ifconfig.setType(NetInterfaceType.MODEM);
        eth0Ifconfig.setUp(isUp);
        when(this.linuxNetworkUtil.getInterfaceConfiguration(interfaceName)).thenReturn(eth0Ifconfig);
    }

    private void givenMultipleInterfaces(boolean isUp, boolean isLinkUp) throws KuraException {
        List<String> interfaces = new ArrayList<>();
        interfaces.add("eth0");
        interfaces.add("wlan1");
        interfaces.add("ppp0");
        when(this.linuxNetworkUtil.getAllInterfaceNames()).thenReturn(interfaces);
        when(this.linuxNetworkUtil.isLinkUp(NetInterfaceType.ETHERNET, "eth0")).thenReturn(isLinkUp);
        when(this.linuxNetworkUtil.isLinkUp(NetInterfaceType.WIFI, "wlan1")).thenReturn(isLinkUp);
        when(this.linuxNetworkUtil.isLinkUp(NetInterfaceType.MODEM, "ppp2")).thenReturn(isLinkUp);

        LinuxIfconfig eth0Ifconfig = new LinuxIfconfig("eth0");
        eth0Ifconfig.setType(NetInterfaceType.ETHERNET);
        eth0Ifconfig.setUp(isUp);
        when(this.linuxNetworkUtil.getInterfaceConfiguration("eth0")).thenReturn(eth0Ifconfig);

        LinuxIfconfig wlan1Ifconfig = new LinuxIfconfig("wlan1");
        wlan1Ifconfig.setType(NetInterfaceType.WIFI);
        wlan1Ifconfig.setUp(isUp);
        when(this.linuxNetworkUtil.getInterfaceConfiguration("wlan1")).thenReturn(wlan1Ifconfig);

        LinuxIfconfig ppp0Ifconfig = new LinuxIfconfig("ppp0");
        ppp0Ifconfig.setType(NetInterfaceType.MODEM);
        ppp0Ifconfig.setUp(isUp);
        when(this.linuxNetworkUtil.getInterfaceConfiguration("ppp0")).thenReturn(ppp0Ifconfig);
    }

    private List<Event> givenModemAddedEvent(int numberOfEvents) {
        List<Event> events = new ArrayList<>();
        for (int index = 0; index < numberOfEvents; index++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY, "1bc7");
            properties.put(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY, "0036");
            properties.put(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY, "1-6");
            properties.put(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, "Telit_LE910_V2");
            properties.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, "/dev/ttyACM" + index);
            properties.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_TTY_DEVICE);
            properties.put(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY, "1");
            properties.put(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY, "6");
            events.add(new Event(UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC, properties));
        }
        return events;
    }

    private void whenGetState() throws KuraException {
        this.networkState = this.networkService.getState();
    }

    private void whenGetInterfaceState(String interfaceName) throws KuraException {
        this.netInterfaceState = this.networkService.getState(interfaceName);
    }

    private void whenGetAllNetworkInterfaceNames() throws KuraException {
        this.interfaceNames = this.networkService.getAllNetworkInterfaceNames();
    }

    private void whenGetNetworkInterfaces() throws KuraException {
        this.networkInterfaces = this.networkService.getNetworkInterfaces();
    }

    private void whenReceivedEvents(List<Event> events) {
        events.forEach(this.networkService::handleEvent);
    }

    private void thenStateIsUnknownConnected() {
        assertEquals(NetworkState.UNKNOWN, this.networkState);
    }

    private void thenNetInterfaceStateIsUnknown() {
        assertEquals(NetInterfaceState.UNKNOWN, this.netInterfaceState);
    }

    private void thenNetworkNamesListSize(int size) {
        assertEquals(size, this.interfaceNames.size());
    }

    private void thenNetworkInterfacesListSize(int size) {
        assertEquals(size, this.networkInterfaces.size());
    }
}
