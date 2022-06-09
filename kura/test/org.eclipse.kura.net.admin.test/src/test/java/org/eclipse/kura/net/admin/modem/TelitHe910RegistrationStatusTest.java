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
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910AtCommands;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;

public class TelitHe910RegistrationStatusTest {

    private TelitHe910 telitModem;
    private ModemRegistrationStatus modemRegistrationStatus;
    private CommConnection connectionMock;

    @Test
    public void shouldBeUnknownNetworkRegisteredIfModemNotInitializedTest() throws KuraException {
        givenUninitializedTelitModem();

        whenGetRegistrationStatus(false);

        thenReturnNetworkRegisteredUnknown();
    }

    @Test
    public void shouldBeNotNetworkRegisteredTest() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenNetworkNotRegistered();

        whenGetRegistrationStatus(true);

        thenReturnNetworkNotRegistered();
    }

    @Test
    public void shouldBeNetworkRegisteredDeniedTest() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenNetworkRegisteredDenied();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegisteredDenied();
    }

    @Test
    public void shouldBeNetworkRegisteredUnknownTest() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenNetworkRegisteredUnknown();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegisteredUnknown();
    }

    @Test
    public void shouldBeNetworkRegisteredTest() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenNetworkRegistered();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegistered();
    }

    @Test
    public void shouldBeNetworkRegisteredRoamingTest() throws KuraException, IOException, URISyntaxException {
        givenTelitModem();
        givenNetworkRegisteredRoaming();

        whenGetRegistrationStatus(true);

        thenReturnNetworkRegisteredRoaming();
    }

    private void givenUninitializedTelitModem() {
        ModemDevice modemMock = mock(UsbModemDevice.class);
        this.telitModem = new TelitHe910(modemMock, "MyAwesomePlatform", null);
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
        when(this.connectionMock.sendCommand(HspaModemAtCommands.AT.getCommand().getBytes(), 1000, 100))
                .thenReturn("OK".getBytes());
        this.telitModem = new TelitHe910(modemMock, "MyAwesomePlatform", null) {

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
        when(this.connectionMock.sendCommand(TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000,
                100)).thenReturn("at+cgreg?\n+CGREG: 2,0\n\nOK".getBytes());
    }

    private void givenNetworkRegistered() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000,
                100)).thenReturn("at+cgreg?\n+CGREG: 2,1,\"6993\",\"0938F20\",7\n\nOK".getBytes());
    }

    private void givenNetworkRegisteredDenied() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000,
                100)).thenReturn("at+cgreg?\n+CGREG: 2,3\n\nOK".getBytes());
    }

    private void givenNetworkRegisteredUnknown() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000,
                100)).thenReturn("at+cgreg?\n+CGREG: 2,4\n\nOK".getBytes());
    }

    private void givenNetworkRegisteredRoaming() throws KuraException, IOException {
        when(this.connectionMock.sendCommand(TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000,
                100)).thenReturn("at+cgreg?\n+CGREG: 2,5\n\nOK".getBytes());
    }

    private void whenGetRegistrationStatus(boolean recompute) throws KuraException {
        this.modemRegistrationStatus = this.telitModem.getRegistrationStatus(recompute);
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
