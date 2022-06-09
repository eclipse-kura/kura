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
import org.eclipse.kura.net.admin.modem.hspa.HspaModem;
import org.eclipse.kura.net.admin.modem.hspa.HspaModemAtCommands;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;

public class HspaModemRssiTest {

    private HspaModem hspaModem;
    private CommConnection connectionMock;
    private int rssi;

    @Test
    public void shouldGetRssi() throws KuraException, IOException, URISyntaxException {
        givenHspaModem();
        givenRssiReply();

        whenGetRssi(true);

        thenRssiIsReturned();
    }

    @Test
    public void shouldGetDefaultRssi() throws KuraException, IOException, URISyntaxException {
        givenHspaModem();

        whenGetRssi(false);

        thenDefaultRssiIsReturned();
    }

    private void givenHspaModem() throws KuraException, IOException, URISyntaxException {
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
        this.hspaModem = new HspaModem(modemMock, "MyAwesomePlatform", null) {

            @Override
            protected CommConnection openSerialPort(String port) throws KuraException {
                return connectionMock;
            }
        };
    }

    private void givenRssiReply() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(HspaModemAtCommands.GET_SIGNAL_STRENGTH.getCommand().getBytes(), 1000,
                100)).thenReturn("at+csq\n+CSQ: 14,99\n\nOK".getBytes());
    }

    private void whenGetRssi(boolean recompute) throws KuraException {
        this.rssi = this.hspaModem.getSignalStrength(recompute);
    }

    private void thenRssiIsReturned() {
        assertEquals(-85, this.rssi);
    }

    private void thenDefaultRssiIsReturned() {
        assertEquals(-113, this.rssi);
    }
}
