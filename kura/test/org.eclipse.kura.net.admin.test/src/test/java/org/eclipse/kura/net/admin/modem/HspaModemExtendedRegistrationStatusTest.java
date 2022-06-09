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

public class HspaModemExtendedRegistrationStatusTest {

    private HspaModem hspaModem;
    private CommConnection connectionMock;
    private String lac;
    private String ci;

    @Test
    public void shouldGetExtendedRegistrationStatus() throws KuraException, IOException, URISyntaxException {
        givenHspaModem();
        givenExtendedRegistrationStatusReply();

        whenGetExtendedRegistrationStatus();

        thenLacIsReturned();
        thenCiIsReturned();
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

    private void givenExtendedRegistrationStatusReply() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(HspaModemAtCommands.GET_REGISTRATION_STATUS2.getCommand().getBytes(), 1000,
                100)).thenReturn("OK".getBytes());
        when(this.connectionMock.sendCommand(HspaModemAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000,
                100)).thenReturn("at+cgreg?\n+CGREG: 2,1,6991,08E9829,7\n\nOK".getBytes());
    }

    private void whenGetExtendedRegistrationStatus() throws KuraException {
        this.hspaModem.getExtendedRegistrationStatus();
        this.lac = this.hspaModem.getLAC();
        this.ci = this.hspaModem.getCI();
    }

    private void thenLacIsReturned() {
        assertEquals("27025", this.lac);
    }

    private void thenCiIsReturned() {
        assertEquals("9345065", this.ci);
    }
}
