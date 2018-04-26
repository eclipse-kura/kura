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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionException;
import org.junit.Test;
import org.osgi.service.io.ConnectionFactory;

public class GpsDeviceTest implements GpsDevice.Listener {

    private static final double EPS = 0.000001;
    private static final int NUM = 7;

    private CountDownLatch latch = new CountDownLatch(NUM);
    private GpsDevice gps;

    private boolean[] visits;

    private final class BlockingSerialPortInputStream extends InputStream {

        private boolean isClosed;

        @Override
        public int read() throws IOException {
            long end = System.currentTimeMillis() + 2000;
            long sleepTime;
            while ((sleepTime = end - System.currentTimeMillis()) > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // ignore interruption
                }
            }
            return -1;
        }

        @Override
        public void close() throws IOException {
            isClosed = true;
        }

        public boolean isClosed() {
            return isClosed;
        }
    }

    @Test(expected = PositionException.class)
    public void testConfigureConnectionISNull() throws PositionException, IOException, InterruptedException {
        final CommURI commUri = new CommURI.Builder("1").withBaudRate(9600).withStopBits(1).withParity(0)
                .withOpenTimeout(2000).withDataBits(8).build();

        CommConnection connMock = mock(CommConnection.class);
        when(connMock.openInputStream()).thenReturn(null);

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        gps = new GpsDevice(connFactoryMock, commUri, this);
    }

    @Test
    public void testConfigureConnectionReadException() throws PositionException, IOException, InterruptedException {
        // IOException => disconnect and close the stream

        final CommURI commUri = new CommURI.Builder("1").withBaudRate(9600).withStopBits(1).withParity(0)
                .withOpenTimeout(2000).withDataBits(8).build();

        InputStream is = mock(InputStream.class);
        when(is.read()).thenThrow(new IOException("test"));
        when(is.read(anyObject())).thenThrow(new IOException("test"));
        when(is.read(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("test"));

        CommConnection connMock = mock(CommConnection.class);
        when(connMock.openInputStream()).thenReturn(is);

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        gps = new GpsDevice(connFactoryMock, commUri, this);

        // wait > 1s for disconnect() call
        Thread.sleep(1200);

        verify(is, times(1)).close();
    }

    @Test
    public void testCloseInputStream() throws IOException, PositionException {
        final CommURI commUri = new CommURI.Builder("1").withBaudRate(9600).withStopBits(1).withParity(0)
                .withOpenTimeout(2000).withDataBits(8).build();

        final BlockingSerialPortInputStream is = new BlockingSerialPortInputStream();

        CommConnection connMock = mock(CommConnection.class);
        when(connMock.openInputStream()).thenReturn(is);

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        gps = new GpsDevice(connFactoryMock, commUri, this);
        gps.disconnect();

        assertFalse(gps.isConnected());
        assertTrue(is.isClosed());
    }

    @Test
    public void testConfigureConnection() throws PositionException, IOException, InterruptedException {
        // test proper call with a few good NMEA sentences and a few corrupted ones
        visits = Arrays.copyOf(new boolean[] { true }, NUM + 1);

        final CommURI commUri = new CommURI.Builder("1").withBaudRate(9600).withStopBits(1).withParity(0)
                .withDataBits(8).build();

        String nmeaStr = "$GPGGA,121041.000,4655.3772,N,01513.6390,E,1,06,1.7,478.3,M,44.7,M,,0000*5d\n"
                + "$GPGSA,A,3,25,23,07,27,20,04,,,,,,,4.9,1.7,4.6*39\n"
                + "$GPRMC,121041.000,A,4655.3772,N,01513.6390,E,0.31,319.55,220517,,*7\n"
                + "$GNVTG,,,,,,,12.34,,,,*4a\n" + "$GNTXT,some text with failing checksum,*4a\n"
                + "$GNTXT,some text with proper checksum,*5d\n" + "$HNINV,invalid,*26\n";
        @SuppressWarnings("resource")
        InputStream is = new SequenceInputStream(new ByteArrayInputStream(nmeaStr.getBytes()),
                new BlockingSerialPortInputStream());

        CommConnection connMock = mock(CommConnection.class);
        when(connMock.openInputStream()).thenReturn(is);

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);

        gps = new GpsDevice(connFactoryMock, commUri, this);

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

    @Override
    public void onLockStatusChanged(boolean hasLock) {
        // TODO Auto-generated method stub

    }

}
