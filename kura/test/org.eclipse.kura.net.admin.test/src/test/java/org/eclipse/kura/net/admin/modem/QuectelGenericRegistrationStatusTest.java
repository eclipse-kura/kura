/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.admin.modem;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.net.admin.modem.hspa.HspaModemAtCommands;
import org.eclipse.kura.net.admin.modem.quectel.generic.QuectelGeneric;
import org.eclipse.kura.net.admin.modem.quectel.generic.QuectelGenericAtCommands;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;

public class QuectelGenericRegistrationStatusTest {

    private QuectelGeneric quectelModem;
    private ModemRegistrationStatus modemRegistrationStatus;
    private CommConnection connectionMock;

    @Test
    public void shouldBeUnknownNetworkRegisteredIfModemNotInitializedTest() throws KuraException {
        givenUninitializedQuectelModem();

        whenGetRegistrationStatus(false);

        thenReturnNetworkRegisteredUnknown();
    }

    @Test
    public void shouldBeNotNetworkRegisteredTest() throws KuraException, IOException, URISyntaxException {
        givenQuectelModem();
        givenNetworkNotRegistered();

        whenGetRegistrationStatus(true);

        thenReturnNetworkNotRegistered();
    }

    @Test
    public void shouldBeNetworkRegisteredDeniedTest() throws KuraException, IOException, URISyntaxException {
        givenQuectelModem();
        givenNetworkRegisteredDenied();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegisteredDenied();
    }

    @Test
    public void shouldBeNetworkRegisteredUnknownTest() throws KuraException, IOException, URISyntaxException {
        givenQuectelModem();
        givenNetworkRegisteredUnknown();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegisteredUnknown();
    }

    @Test
    public void shouldBeNetworkRegisteredTest() throws KuraException, IOException, URISyntaxException {
        givenQuectelModem();
        givenNetworkRegistered();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegistered();
    }

    @Test
    public void shouldBeNetworkRegisteredRoamingTest() throws KuraException, IOException, URISyntaxException {
        givenQuectelModem();
        givenNetworkRegisteredRoaming();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegisteredRoaming();
    }

    private void givenUninitializedQuectelModem() {
        ModemDevice modemMock = mock(UsbModemDevice.class);
        this.quectelModem = new QuectelGeneric(modemMock, "MyAwesomePlatform", null);
    }

    private void givenQuectelModem() throws KuraException, IOException, URISyntaxException {
        List<String> ports = new ArrayList<>();
        ports.add("/dev/ttyUSB123");
        ports.add("/dev/ttyUSB456");
        ports.add("/dev/ttyUSB789");
        UsbModemDevice modemMock = mock(UsbModemDevice.class);
        when(modemMock.getSerialPorts()).thenReturn(ports);
        when(modemMock.getProductId()).thenReturn("0125");
        when(modemMock.getVendorId()).thenReturn("2c7c");
        when(modemMock.getProductName()).thenReturn("EX25");

        this.connectionMock = mock(CommConnection.class);
        when(this.connectionMock.getURI()).thenReturn(CommURI.parseString("comm://port"));
        when(this.connectionMock.sendCommand(HspaModemAtCommands.AT.getCommand().getBytes(), 1000, 100))
                .thenReturn("OK".getBytes());
        this.quectelModem = new QuectelGeneric(modemMock, "MyAwesomePlatform", null) {

            @Override
            public String getSerialNumber() throws KuraException {
                return "serialNumber";
            }

            @Override
            public String getMobileSubscriberIdentity(boolean recompute) throws KuraException {
                return "IMSI";
            }

            @Override
            public String getIntegratedCirquitCardId(boolean recompute) throws KuraException {
                return "ICCID";
            }

            @Override
            public String getModel() throws KuraException {
                return "model";
            }

            @Override
            public String getManufacturer() throws KuraException {
                return "manufacturer";
            }

            @Override
            public String getRevisionID() throws KuraException {
                return "revisionID";
            }

            @Override
            public boolean isGpsSupported() throws KuraException {
                return true;
            }

            @Override
            public int getSignalStrength(boolean recompute) throws KuraException {
                return -100;
            }

            @Override
            public String getFirmwareVersion() throws KuraException {
                return "1.2.3.4";
            }

            @Override
            protected CommConnection openSerialPort(String port) throws KuraException {
                return connectionMock;
            }

        };
    }

    private void givenNetworkNotRegistered() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(),
                1000, 100)).thenReturn("at+cgreg?\n+CGREG: 2,0\n\nOK".getBytes());
    }

    private void givenNetworkRegistered() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(),
                1000, 100)).thenReturn("at+cgreg?\n+CGREG: 2,1,\"6993\",\"0938F20\",7\n\nOK".getBytes());
        when(this.connectionMock
                .sendCommand(QuectelGenericAtCommands.GET_QUERY_NETWORK_INFORMATION.getCommand().getBytes(), 1000, 100))
                        .thenReturn("at+qnwinfo\n+QNWINFO: \"WCDMA\",\"22299\",\"WCDMA 900\",3063\n\nOK".getBytes());
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTERED_NETWORK.getCommand().getBytes(),
                1000, 100)).thenReturn(
                        "at+qspn\n+QSPN: \"vodafone IT\",\"vodafone IT\",\"\",0,\"22210\"\n\nOK".getBytes());
        when(this.connectionMock.sendCommand(
                QuectelGenericAtCommands.GET_EXTENDED_REGISTRATION_STATUS.getCommand().getBytes(), 1000, 100))
                        .thenReturn("OK".getBytes());
        when(this.connectionMock.sendCommand(
                QuectelGenericAtCommands.GET_EXTENDED_REGISTRATION_STATUS.getCommand().getBytes(), 1000, 100))
                        .thenReturn("OK".getBytes());
    }

    private void givenNetworkRegisteredDenied() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(),
                1000, 100)).thenReturn("at+cgreg?\n+CGREG: 2,3\n\nOK".getBytes());
    }

    private void givenNetworkRegisteredUnknown() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(),
                1000, 100)).thenReturn("at+cgreg?\n+CGREG: 2,4\n\nOK".getBytes());
    }

    private void givenNetworkRegisteredRoaming() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(),
                1000, 100)).thenReturn("at+cgreg?\n+CGREG: 2,5,\"6993\",\"0938F20\",7\n\nOK".getBytes());
        when(this.connectionMock
                .sendCommand(QuectelGenericAtCommands.GET_QUERY_NETWORK_INFORMATION.getCommand().getBytes(), 1000, 100))
                        .thenReturn("at+qnwinfo\n+QNWINFO: \"WCDMA\",\"22299\",\"WCDMA 900\",3063\n\nOK".getBytes());
        when(this.connectionMock.sendCommand(QuectelGenericAtCommands.GET_REGISTERED_NETWORK.getCommand().getBytes(),
                1000, 100)).thenReturn(
                        "at+qspn\n+QSPN: \"vodafone IT\",\"vodafone IT\",\"\",0,\"22210\"\n\nOK".getBytes());
        when(this.connectionMock.sendCommand(
                QuectelGenericAtCommands.GET_EXTENDED_REGISTRATION_STATUS.getCommand().getBytes(), 1000, 100))
                        .thenReturn("OK".getBytes());
        when(this.connectionMock.sendCommand(
                QuectelGenericAtCommands.GET_EXTENDED_REGISTRATION_STATUS.getCommand().getBytes(), 1000, 100))
                        .thenReturn("OK".getBytes());
    }

    private void whenGetRegistrationStatus(boolean recompute) throws KuraException {
        this.modemRegistrationStatus = this.quectelModem.getRegistrationStatus(recompute);
    }

    private void thenReturnNetworkNotRegistered() {
        assertEquals(ModemRegistrationStatus.NOT_REGISTERED, this.modemRegistrationStatus);
    }

    private void thenReturnNetworkRegisteredDenied() {
        assertEquals(ModemRegistrationStatus.REGISTRATION_DENIED, this.modemRegistrationStatus);
    }

    private void thenReturnNetworkRegistered() {
        assertEquals(ModemRegistrationStatus.REGISTERED_HOME, this.modemRegistrationStatus);
    }

    private void thenReturnNetworkRegisteredUnknown() {
        assertEquals(ModemRegistrationStatus.UNKNOWN, this.modemRegistrationStatus);
    }

    private void thenReturnNetworkRegisteredRoaming() {
        assertEquals(ModemRegistrationStatus.REGISTERED_ROAMING, this.modemRegistrationStatus);
    }

}
