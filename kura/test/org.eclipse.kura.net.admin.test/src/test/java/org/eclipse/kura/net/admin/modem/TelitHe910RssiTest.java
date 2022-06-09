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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModemAtCommands;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;

public class TelitHe910RssiTest {

    private TelitHe910 telitModem;
    private CommConnection connectionMock;
    private int rssi;
    private boolean exceptionCaught;

    @Test
    public void shouldGetRssi() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenRssiReply();

        whenGetRssi(true);

        thenRssiIsReturned();
    }

    @Test
    public void shouldGetDefaultRssi() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();

        whenGetRssi(false);

        thenDefaultRssiIsReturned();
    }

    @Test
    public void shouldGetRssiWithOldMethod() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenRssiReply();

        whenGetRssiWithOldMethod();

        thenRssiIsReturned();
    }

    @Test
    public void shouldThrowExceptionWhenModemNotReachable() throws KuraException, IOException, URISyntaxException {
        givenHspaModemNotReachable();

        whenGetRssi(true);

        thenExceptionIsCaught();
    }

    private void givenTelitModem() throws KuraException, IOException, URISyntaxException {
        List<String> ports = new ArrayList<>();
        ports.add("/dev/ttyUSB123");
        ports.add("/dev/ttyUSB456");
        ports.add("/dev/ttyUSB789");
        ports.add("/dev/ttyUSB987");
        UsbModemDevice modemMock = mock(UsbModemDevice.class);
        when(modemMock.getSerialPorts()).thenReturn(ports);
        when(modemMock.getProductId()).thenReturn("0021");
        when(modemMock.getVendorId()).thenReturn("1bc7");
        when(modemMock.getProductName()).thenReturn("HE910");

        this.connectionMock = mock(CommConnection.class);
        when(this.connectionMock.getURI()).thenReturn(CommURI.parseString("comm://port"));
        when(this.connectionMock.sendCommand(TelitModemAtCommands.AT.getCommand().getBytes(), 1000, 100))
                .thenReturn("OK".getBytes());
        this.telitModem = new TelitHe910(modemMock, "MyAwesomePlatform", null) {

            @Override
            public boolean isGpsSupported() throws KuraException {
                return true;
            }

            @Override
            protected CommConnection openSerialPort(String port) throws KuraException {
                return connectionMock;
            }
        };
    }

    private void givenHspaModemNotReachable() throws KuraException, IOException, URISyntaxException {
        this.exceptionCaught = false;
        List<String> ports = new ArrayList<>();
        ports.add("/dev/ttyUSB123");
        ports.add("/dev/ttyUSB456");
        ports.add("/dev/ttyUSB789");
        ports.add("/dev/ttyUSB987");
        UsbModemDevice modemMock = mock(UsbModemDevice.class);
        when(modemMock.getSerialPorts()).thenReturn(ports);
        when(modemMock.getProductId()).thenReturn("0021");
        when(modemMock.getVendorId()).thenReturn("1bc7");
        when(modemMock.getProductName()).thenReturn("HE910");

        this.connectionMock = mock(CommConnection.class);
        when(this.connectionMock.getURI()).thenReturn(CommURI.parseString("comm://port"));
        when(this.connectionMock.sendCommand(TelitModemAtCommands.AT.getCommand().getBytes(), 1000, 100))
                .thenReturn("ERROR".getBytes());
        this.telitModem = new TelitHe910(modemMock, "MyAwesomePlatform", null) {

            @Override
            public boolean isGpsSupported() throws KuraException {
                return true;
            }

            @Override
            protected CommConnection openSerialPort(String port) throws KuraException {
                return connectionMock;
            }
        };
    }

    private void givenRssiReply() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(TelitModemAtCommands.GET_SIGNAL_STRENGTH.getCommand().getBytes(), 1000,
                100)).thenReturn("at+csq\n+CSQ: 14,99\n\nOK".getBytes());
    }

    private void whenGetRssi(boolean recompute) {
        try {
            this.rssi = this.telitModem.getSignalStrength(recompute);
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenGetRssiWithOldMethod() throws KuraException {
        this.rssi = this.telitModem.getSignalStrength();
    }

    private void thenRssiIsReturned() {
        assertEquals(-85, this.rssi);
    }

    private void thenDefaultRssiIsReturned() {
        assertEquals(-113, this.rssi);
    }

    private void thenExceptionIsCaught() {
        assertTrue(this.exceptionCaught);
    }
}
