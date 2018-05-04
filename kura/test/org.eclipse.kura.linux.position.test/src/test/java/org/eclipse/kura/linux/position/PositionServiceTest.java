/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionListener;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionLostEvent;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.position.Position;

public class PositionServiceTest {

    private static final double EPS = 0.000001;

    private static final class PositionServiceTestFixture {

        final EventAdmin eventAdmin;
        final ConnectionFactory connectionFactory;
        final ModemGpsStatusTracker modemGpsStatusTracker;
        final GpsDeviceTracker gpsDeviceTracker;
        final PositionServiceImpl ps;

        public PositionServiceTestFixture(final String nmeaStrings) throws IOException {
            this.eventAdmin = mock(EventAdmin.class);
            this.modemGpsStatusTracker = mock(ModemGpsStatusTracker.class);
            this.gpsDeviceTracker = getMockGpsDeviceTracker();
            this.connectionFactory = getMockConnectionFactory(nmeaStrings);
            this.ps = new PositionServiceImpl();

            this.ps.setEventAdmin(this.eventAdmin);
            this.ps.setModemGpsStatusTracker(this.modemGpsStatusTracker);
            this.ps.setGpsDeviceTracker(this.gpsDeviceTracker);
            this.ps.setConnectionFactory(this.connectionFactory);
        }

        public PositionServiceTestFixture() throws IOException {
            this("");
        }
    }

    private static GpsDeviceTracker getMockGpsDeviceTracker() {
        final GpsDeviceTracker tracker = mock(GpsDeviceTracker.class);
        when(tracker.track(anyObject())).thenAnswer(invocation -> invocation.getArgumentAt(0, CommURI.class));
        return tracker;
    }

    private static ConnectionFactory getMockConnectionFactory(final String nmeaStrings) throws IOException {
        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        InputStream is = new ByteArrayInputStream(nmeaStrings.getBytes());
        when(connMock.openInputStream()).thenReturn(is);

        return connFactoryMock;
    }

    private Map<String, Object> getDefaultProperties() {
        final Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("enabled", true);
        properties.put("static", false);
        properties.put("port", "port");
        properties.put("baudRate", 9600);
        properties.put("stopBits", 0);
        properties.put("parity", 0);
        properties.put("bitsPerWord", 8);

        return properties;
    }

    private static final class UriMatcher extends ArgumentMatcher<String> {

        private final CommURI uri;

        public UriMatcher(final CommURI uri) {
            this.uri = uri;
        }

        @Override
        public boolean matches(Object argument) {
            final String port = (String) argument;

            try {
                final CommURI argUri = CommURI.parseString(port);

                assertEquals(uri.getPort(), argUri.getPort());
                assertEquals(uri.getBaudRate(), argUri.getBaudRate());
                assertEquals(uri.getStopBits(), argUri.getStopBits());
                assertEquals(uri.getDataBits(), argUri.getDataBits());
                assertEquals(uri.getParity(), argUri.getParity());
                assertEquals(uri.getFlowControl(), argUri.getFlowControl());
                assertEquals(2000, argUri.getOpenTimeout());
                assertEquals(2000, argUri.getReceiveTimeout());

                return true;
            } catch (URISyntaxException e) {
                fail("Provided uri syntax must be correct");
            }

            return false;
        }

    }

    private static UriMatcher isUri(final CommURI uri) {
        return new UriMatcher(uri);
    }

    private static final ArgumentMatcher<Event> isPositionLockedEvent = new ArgumentMatcher<Event>() {

        @Override
        public boolean matches(Object argument) {
            return argument instanceof PositionLockedEvent;
        }
    };

    private static final ArgumentMatcher<Event> isPositionLostEvent = new ArgumentMatcher<Event>() {

        @Override
        public boolean matches(Object argument) {
            return argument instanceof PositionLostEvent;
        }
    };

    @Test
    public void testActivateDisabled() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", false);
        properties.put("static", true);
        properties.put("longitude", 15.0);
        properties.put("latitude", 46.0);
        properties.put("altitude", 300.0);

        fixture.ps.activate(properties);

        verify(fixture.eventAdmin, times(0)).postEvent((PositionLockedEvent) anyObject());
        assertFalse(fixture.ps.isLocked());

        fixture.ps.deactivate();
    }

    @Test
    public void testActivateStatic() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", true);
        properties.put("latitude", 46.0d);
        properties.put("longitude", 15.0d);
        properties.put("altitude", 300.0d);

        fixture.ps.activate(properties);

        verify(fixture.eventAdmin, times(1)).postEvent((PositionLockedEvent) anyObject());
        assertTrue(fixture.ps.isLocked());

        final Position position = fixture.ps.getPosition();

        assertEquals(46.0d, Math.toDegrees(position.getLatitude().getValue()), EPS);
        assertEquals(15.0d, Math.toDegrees(position.getLongitude().getValue()), EPS);
        assertEquals(300.0d, position.getAltitude().getValue(), EPS);

        final NmeaPosition nmeaPosition = fixture.ps.getNmeaPosition();

        assertEquals(46.0d, nmeaPosition.getLatitude(), EPS);
        assertEquals(15.0d, nmeaPosition.getLongitude(), EPS);
        assertEquals(300, nmeaPosition.getAltitude(), EPS);

        assertNull(fixture.ps.getNmeaDate());
        assertNull(fixture.ps.getNmeaTime());
        assertNull(fixture.ps.getLastSentence());

        fixture.ps.deactivate();
    }

    @Test
    public void testActivateDefault() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        fixture.ps.activate(getDefaultProperties());

        assertFalse(fixture.ps.isLocked());
        assertNotNull(fixture.ps.getPosition());
        assertEquals(0.0, fixture.ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getAltitude().getValue(), EPS);

        assertNotNull(fixture.ps.getGpsDevice());

        fixture.ps.deactivate();
    }

    @Test
    public void testActivateWithEmptyGPSPort() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        final Map<String, Object> properties = getDefaultProperties();
        properties.put("port", "");

        fixture.ps.activate(properties);

        assertFalse(fixture.ps.isLocked());
        assertNotNull(fixture.ps.getPosition());
        assertEquals(0.0, fixture.ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getAltitude().getValue(), EPS);

        assertNull(fixture.ps.getGpsDevice());

        fixture.ps.deactivate();
    }

    @Test
    public void testActivateWithNullGPSPort() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        final Map<String, Object> properties = getDefaultProperties();
        properties.put("port", null);

        fixture.ps.activate(properties);

        assertFalse(fixture.ps.isLocked());
        assertNotNull(fixture.ps.getPosition());
        assertEquals(0.0, fixture.ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getAltitude().getValue(), EPS);

        assertNull(fixture.ps.getGpsDevice());

        fixture.ps.deactivate();
    }

    @Test
    public void testUpdateWithSameConfig() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        fixture.ps.activate(getDefaultProperties());

        assertFalse(fixture.ps.isLocked());
        assertNotNull(fixture.ps.getPosition());
        assertEquals(0.0, fixture.ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getAltitude().getValue(), EPS);

        final GpsDevice device = fixture.ps.getGpsDevice();
        assertNotNull(device);

        fixture.ps.updated(getDefaultProperties());

        assertTrue(device == fixture.ps.getGpsDevice());

        fixture.ps.deactivate();
    }

    @Test
    public void testUseGpsFromConfig() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        final CommURI defaultURI = new CommURI.Builder("port").withBaudRate(9600).withDataBits(8).withStopBits(0)
                .build();

        fixture.ps.activate(getDefaultProperties());

        assertFalse(fixture.ps.isLocked());
        assertNotNull(fixture.ps.getPosition());
        assertEquals(0.0, fixture.ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getAltitude().getValue(), EPS);

        final GpsDevice device = fixture.ps.getGpsDevice();
        assertNotNull(device);

        verify(fixture.connectionFactory, times(1)).createConnection(argThat(isUri(defaultURI)), eq(1), eq(false));
        assertEquals("port", device.getCommURI().getPort());

        fixture.ps.deactivate();
    }

    @Test
    public void testUseModemGps() throws IOException {

        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        final CommURI modemUri = new CommURI.Builder("modemPort").withBaudRate(115200).withStopBits(0).withDataBits(8)
                .withParity(0).build();

        when(fixture.modemGpsStatusTracker.getGpsDeviceUri()).thenReturn(modemUri);

        fixture.ps.activate(getDefaultProperties());

        assertFalse(fixture.ps.isLocked());
        assertNotNull(fixture.ps.getPosition());
        assertEquals(0.0, fixture.ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, fixture.ps.getPosition().getAltitude().getValue(), EPS);

        assertNotNull(fixture.ps.getGpsDevice());
        verify(fixture.connectionFactory, times(1)).createConnection(argThat(isUri(modemUri)), eq(1), eq(false));

        fixture.ps.deactivate();
    }

    @Test
    public void testHandleModemAvailabilityChangeEvent() throws IOException, NoSuchFieldException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        fixture.ps.activate(getDefaultProperties());

        GpsDevice gps = fixture.ps.getGpsDevice();

        assertNotNull(gps);

        when(fixture.gpsDeviceTracker.track(anyObject())).thenReturn(null);
        fixture.ps.onGpsDeviceAvailabilityChanged();

        assertNull(fixture.ps.getGpsDevice());
    }

    @Test
    public void testSwitchToModemGps() throws IOException, NoSuchFieldException, KuraException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        fixture.ps.activate(getDefaultProperties());

        final GpsDevice deviceFromConfig = fixture.ps.getGpsDevice();
        assertNotNull(deviceFromConfig);

        when(fixture.modemGpsStatusTracker.getGpsDeviceUri()).thenReturn(new CommURI.Builder("modemPort")
                .withBaudRate(115200).withStopBits(0).withDataBits(8).withParity(0).build());

        fixture.ps.onGpsDeviceAvailabilityChanged();

        final GpsDevice modemDevice = fixture.ps.getGpsDevice();
        assertNotNull(modemDevice);

        assertEquals("port", deviceFromConfig.getCommURI().getPort());
        assertEquals("modemPort", modemDevice.getCommURI().getPort());

        fixture.ps.deactivate();
    }

    @Test
    public void testSwitchToStatic() throws IOException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        Map<String, Object> properties = getDefaultProperties();

        fixture.ps.activate(properties);
        assertNotNull(fixture.ps.getGpsDevice());

        properties.put("static", true);
        properties.put("latitude", 20.0d);
        properties.put("longitude", 30.0d);
        properties.put("altitude", 40.0d);

        fixture.ps.updated(properties);
        assertNull(fixture.ps.getGpsDevice());

        assertTrue(fixture.ps.isLocked());

        final Position position = fixture.ps.getPosition();
        assertEquals(20.0d, Math.toDegrees(position.getLatitude().getValue()), EPS);
        assertEquals(30.0d, Math.toDegrees(position.getLongitude().getValue()), EPS);
        assertEquals(40.0d, position.getAltitude().getValue(), EPS);

        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLockedEvent));
    }

    @Test
    public void testLockEvents() throws IOException {

        final PositionServiceTestFixture fixture = new PositionServiceTestFixture();

        fixture.ps.activate(getDefaultProperties());

        fixture.ps.onLockStatusChanged(true);
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLockedEvent));

        fixture.ps.onLockStatusChanged(true);
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLockedEvent));

        fixture.ps.onLockStatusChanged(false);
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLostEvent));

        fixture.ps.onLockStatusChanged(false);
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLostEvent));

        fixture.ps.onLockStatusChanged(true);
        verify(fixture.eventAdmin, times(2)).postEvent(argThat(isPositionLockedEvent));

        fixture.ps.deactivate();
    }

    @Test
    public void testPositionListener() throws IOException, InterruptedException {

        final PositionServiceTestFixture fixture = new PositionServiceTestFixture(
                "$GPGGA,121041.000,4655.3772,N,01513.6390,E,1,06,1.7,478.3,M,44.7,M,,0000*5d\n"
                        + "$GPGSA,A,3,25,23,07,27,20,04,,,,,,,4.9,1.7,4.6*39\n"
                        + "$GPRMC,121041.000,A,4655.3772,N,01513.6390,E,0.31,319.55,220517,,*7\n"
                        + "$GNVTG,,,,,,,12.34,,,,*4a\n" + "$GNTXT,some text with failing checksum,*4a\n"
                        + "$GNTXT,some text with proper checksum,*5d\n" + "$HNINV,invalid,*26\n");

        final PositionListener listener = mock(PositionListener.class);
        fixture.ps.registerListener("test", listener);

        fixture.ps.activate(getDefaultProperties());

        Thread.sleep(5000);

        fixture.ps.deactivate();

        verify(listener, times(7)).newNmeaSentence(anyObject());
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLockedEvent));
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLostEvent));
    }

    @Test
    public void testPositionDataFromGps() throws IOException, InterruptedException {
        final PositionServiceTestFixture fixture = new PositionServiceTestFixture(
                "$GPGGA,121041.000,4655.3772,N,01513.6390,E,1,06,1.7,478.3,M,44.7,M,,0000*5d\n"
                        + "$GPGSA,A,3,25,23,07,27,20,04,,,,,,,4.9,1.7,4.6*39\n"
                        + "$GPRMC,121041.000,A,4655.3772,N,01513.6390,E,0.31,319.55,220517,,*7\n"
                        + "$GNVTG,,,,,,,12.34,,,,*4a\n");

        final PositionListener listener = mock(PositionListener.class);
        fixture.ps.registerListener("test", listener);

        fixture.ps.activate(getDefaultProperties());

        Thread.sleep(5000);

        verify(listener, times(4)).newNmeaSentence(anyObject());
        verify(fixture.eventAdmin, times(1)).postEvent(argThat(isPositionLockedEvent));
        verify(fixture.eventAdmin, times(0)).postEvent(argThat(isPositionLostEvent));

        assertTrue(fixture.ps.isLocked());

        final Position position = fixture.ps.getPosition();
        final NmeaPosition nmeaPosition = fixture.ps.getNmeaPosition();
        final String date = fixture.ps.getNmeaDate();
        final String time = fixture.ps.getNmeaTime();
        final String lastSentence = fixture.ps.getLastSentence();

        // from GGA
        assertEquals(1, nmeaPosition.getFixQuality());
        assertEquals(6, nmeaPosition.getNrSatellites());
        assertEquals(1.7, nmeaPosition.getDOP(), EPS);
        assertEquals(478.3, nmeaPosition.getAltitude(), EPS);
        assertEquals(478.3, position.getAltitude().getValue(), EPS);

        // from GSA
        assertEquals(3, nmeaPosition.get3Dfix());
        assertEquals(4.9, nmeaPosition.getPDOP(), EPS);
        assertEquals(1.7, nmeaPosition.getHDOP(), EPS);
        assertEquals(4.6, nmeaPosition.getVDOP(), EPS);

        // from RMC
        assertEquals("121041.000", time);
        assertEquals("220517", date);
        assertEquals(15.227317, nmeaPosition.getLongitude(), EPS);
        assertEquals(46.922953, nmeaPosition.getLatitude(), EPS);
        assertEquals(319.55, nmeaPosition.getTrack(), EPS);
        assertEquals(Math.toRadians(15.227317), position.getLongitude().getValue(), EPS);
        assertEquals(Math.toRadians(46.922953), position.getLatitude().getValue(), EPS);
        assertEquals(Math.toRadians(319.55), position.getTrack().getValue(), EPS);

        // from VTG
        assertEquals(12.34 / 3.6, nmeaPosition.getSpeed(), EPS);
        assertEquals(12.34 / 3.6, position.getSpeed().getValue(), EPS);

        assertEquals("$GNVTG,,,,,,,12.34,,,,*4a\n", lastSentence);

        fixture.ps.deactivate();

    }
}
