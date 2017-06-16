/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionException;
import org.eclipse.kura.position.PositionListener;
import org.junit.Test;
import org.osgi.service.io.ConnectionFactory;


public class GpsDeviceTest implements PositionListener {

    private static final double EPS = 0.000001;
    private static final int NUM = 7;

    private CountDownLatch latch = new CountDownLatch(NUM);
    private GpsDevice gps;

    private boolean[] visits;

    @Test
    public void testConfigureConnectionISNull() throws PositionException, IOException, InterruptedException {
        // helper for reproducing null stream debug message

        gps = new GpsDevice();

        Collection<PositionListener> listeners = new ArrayList<PositionListener>();
        listeners.add(this);
        gps.setListeners(listeners);

        Properties connectionConfig = new Properties();
        connectionConfig.setProperty("port", "1");
        connectionConfig.setProperty("baudRate", "9600");
        connectionConfig.setProperty("stopBits", "1");
        connectionConfig.setProperty("parity", "0");
        connectionConfig.setProperty("bitsPerWord", "8");

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        when(connMock.openInputStream()).thenReturn(null);

        gps.configureConnection(connFactoryMock, connectionConfig);

        // wait > 1s
        Thread.sleep(1200);

        gps.disconnect();
    }

    @Test
    public void testConfigureConnectionReadException() throws PositionException, IOException, InterruptedException {
        // IOException => disconnect and close the stream

        gps = new GpsDevice();

        Collection<PositionListener> listeners = new ArrayList<PositionListener>();
        listeners.add(this);
        gps.setListeners(listeners);

        Properties connectionConfig = new Properties();
        connectionConfig.setProperty("port", "1");
        connectionConfig.setProperty("baudRate", "9600");
        connectionConfig.setProperty("stopBits", "1");
        connectionConfig.setProperty("parity", "0");
        connectionConfig.setProperty("bitsPerWord", "8");

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        InputStream is = mock(InputStream.class);
        when(connMock.openInputStream()).thenReturn(is);

        when(is.read()).thenThrow(new IOException("test"));

        gps.configureConnection(connFactoryMock, connectionConfig);

        // wait > 1s for disconnect() call
        Thread.sleep(1200);

        verify(is, times(1)).close();
    }

    @Test
    public void testConfigureConnection() throws PositionException, IOException, InterruptedException {
        // test proper call with a few good NMEA sentences and a few corrupted ones
        visits = Arrays.copyOf(new boolean[] { true }, NUM + 1);

        gps = new GpsDevice();

        Collection<PositionListener> listeners = new ArrayList<PositionListener>();
        listeners.add(this);
        gps.setListeners(listeners);

        Properties connectionConfig = new Properties();
        connectionConfig.setProperty("port", "1");
        connectionConfig.setProperty("baudRate", "9600");
        connectionConfig.setProperty("stopBits", "1");
        connectionConfig.setProperty("parity", "0");
        connectionConfig.setProperty("bitsPerWord", "8");

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        String nmeaStr = "$GPGGA,121041.000,4655.3772,N,01513.6390,E,1,06,1.7,478.3,M,44.7,M,,0000*5d\n"
                + "$GPGSA,A,3,25,23,07,27,20,04,,,,,,,4.9,1.7,4.6*39\n"
                + "$GPRMC,121041.000,A,4655.3772,N,01513.6390,E,0.31,319.55,220517,,*7\n"
                + "$GNVTG,,,,,,,12.34,,,,*4a\n" + "$GNTXT,some text with failing checksum,*4a\n"
                + "$GNTXT,some text with proper checksum,*5d\n" + "$HNINV,invalid,*26\n";
        InputStream is = new ByteArrayInputStream(nmeaStr.getBytes());
        when(connMock.openInputStream()).thenReturn(is);

        gps.configureConnection(connFactoryMock, connectionConfig);

        latch.await(1, TimeUnit.SECONDS);

        gps.disconnect();

        for (int i = visits.length - 1; i >= 0; i--) {
            assertTrue("Sentence " + i, visits[i]);
        }
    }

    @Override
    public void newNmeaSentence(String nmeaSentence) {
        int max = visits.length - 1;

        long count = latch.getCount();
        latch.countDown();

        if (count == max) {
            // nothing to do, yet, because listeners are called first
            assertFalse(gps.isValidPosition());
        } else if (count == max - 1) {
            // result of GGA
            assertTrue(gps.isValidPosition());
            assertEquals("121041.000", gps.getTimeNmea());
            assertEquals(Math.toRadians(15.227317), gps.getPosition().getLongitude().getValue(), EPS);
            assertEquals(Math.toRadians(46.922953), gps.getPosition().getLatitude().getValue(), EPS);

            NmeaPosition nmeaPosition = gps.getNmeaPosition();
            assertEquals(1, nmeaPosition.getFixQuality());
            assertEquals(6, nmeaPosition.getNrSatellites());
            assertEquals(1.7, nmeaPosition.getDOP(), EPS);
            assertEquals(478.3, nmeaPosition.getAltitude(), EPS);
            assertEquals(0.0, nmeaPosition.getPDOP(), EPS);
            assertEquals(0.0, nmeaPosition.getHDOP(), EPS);
            assertEquals(0.0, nmeaPosition.getVDOP(), EPS);
        } else if (count == max - 2) {
            // result of GSA
            assertTrue(gps.isValidPosition());

            NmeaPosition nmeaPosition = gps.getNmeaPosition();
            assertEquals(3, nmeaPosition.get3Dfix());
            assertEquals(4.9, nmeaPosition.getPDOP(), EPS);
            assertEquals(1.7, nmeaPosition.getHDOP(), EPS);
            assertEquals(4.6, nmeaPosition.getVDOP(), EPS);
        } else if (count == max - 3) {
            // result of RMC
            assertEquals("121041.000", gps.getTimeNmea());
            assertEquals("220517", gps.getDateNmea());

            NmeaPosition nmeaPosition = gps.getNmeaPosition();
            assertEquals(15.227317, nmeaPosition.getLongitude(), EPS);
            assertEquals(46.922953, nmeaPosition.getLatitude(), EPS);
            assertEquals(0.159478, nmeaPosition.getSpeed(), EPS);
            assertEquals(319.55, nmeaPosition.getTrack(), EPS);
        } else if (count == max - 4) {
            // result of VTG
            assertEquals(12.34, gps.getNmeaPosition().getSpeedKmh(), EPS);
        } else {
            // results of TXTs and others
        }

        // no failures => sentence parsed OK
        visits[(int) count] = true;
    }

}
