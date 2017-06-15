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

import org.junit.Test;


public class NMEAParserTest {

    private static final double EPS = 0.000001;

    @Test
    public void testConvertPositionlat90S() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlat("900.0000", "S");

        assertTrue("Bounds are expected to be respected", pos >= -90.0);
        assertEquals("Expected value within limits", -90.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlat90N() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlat("900.0000", "N");

        assertTrue("Bounds are expected to be respected", pos <= 90.0);
        assertEquals("Expected value within limits", 90.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlat0N() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlat("0000.0000", "N");

        assertEquals("Expected value within limits", 0.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlat451N() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlat("451.2345", "N");

        assertEquals("Expected value within limits", 45.020575, pos, EPS);
    }

    @Test
    public void testConvertPositionlat4559S() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlat("4559.0123", "S");

        assertEquals("Expected value within limits", -45.983538, pos, EPS);
    }

    @Test
    public void testConvertPositionlat559S() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlat("0559.0123", "S");

        assertEquals("Expected value within limits", -5.983538, pos, EPS);
    }

    @Test
    public void testConvertPositionlon180E() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("18000.0000", "E");

        assertTrue("Bounds are expected to be respected", pos <= 180.0);
        assertEquals("Expected value within limits", 180.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlon180W() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("18000.0000", "W");

        assertTrue("Bounds are expected to be respected", pos >= -180.0);
        assertEquals("Expected value within limits", -180.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlon0() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("00000.0000", "W");

        assertEquals("Expected value within limits", 0.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlon1E() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("00100.0000", "E");

        assertEquals("Expected value within limits", 1.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlon11E() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("01100.0000", "E");

        assertEquals("Expected value within limits", 11.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlon111E() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("11100.0000", "E");

        assertEquals("Expected value within limits", 111.0, pos, EPS);
    }

    @Test
    public void testConvertPositionlon111ME() {
        NMEAParser parser = new NMEAParser();

        double pos = parser.convertPositionlon("11101.1000", "E");

        assertEquals("Expected value within limits", 111.018333, pos, EPS);
    }

    @Test
    public void testParseSentenceGGA() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPGGA,121041.000,4655.3772,N,01513.6390,E,1,06,1.7,478.3,M,44.7,M,,0000*5d");

        assertTrue(parser.isValidPosition());
        assertEquals("121041.000", parser.getTimeNmea());
        assertEquals(15.227317, parser.getLongNmea(), EPS);
        assertEquals(46.922953, parser.getLatNmea(), EPS);
        assertEquals(1, parser.getFixQuality());
        assertEquals(6, parser.getNrSatellites());
        assertEquals(1.7, parser.getDOPNmea(), EPS);
        assertEquals(478.3, parser.getAltNmea(), EPS);
    }

    @Test
    public void testParseSentenceGGAInvPos() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPGGA,121041.000,4655.3772,N,01513.6390,E,0,06,1.7,478.3,M,44.7,M,,0000*5d");

        assertFalse(parser.isValidPosition());
        assertEquals("121041.000", parser.getTimeNmea());
        assertEquals(15.227317, parser.getLongNmea(), EPS);
        assertEquals(46.922953, parser.getLatNmea(), EPS);
        assertEquals(0, parser.getFixQuality());
        assertEquals(6, parser.getNrSatellites());
        assertEquals(1.7, parser.getDOPNmea(), EPS);
        assertEquals(478.3, parser.getAltNmea(), EPS);
    }

    @Test
    public void testParseSentenceGLL() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPGLL,4655.3772,N,01513.6390,E,121041.000,A,*0");

        assertTrue(parser.isValidPosition());
        assertEquals("121041.000", parser.getTimeNmea());
        assertEquals(15.227317, parser.getLongNmea(), EPS);
        assertEquals(46.922953, parser.getLatNmea(), EPS);
    }

    @Test
    public void testParseSentenceGSA() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPGSA,A,3,25,23,07,27,20,04,,,,,,,4.9,1.7,4.6*39");

        assertTrue(parser.isValidPosition());
        assertEquals(3, parser.getFix3DNmea());
        assertEquals(4.9, parser.getPDOPNmea(), EPS);
        assertEquals(1.7, parser.getHDOPNmea(), EPS);
        assertEquals(4.6, parser.getVDOPNmea(), EPS);
    }

    @Test
    public void testParseSentenceGSAInvalidPos() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPGSA,A,1,25,23,07,27,20,04,,,,,,,4.9,1.7,4.6*39");

        assertFalse(parser.isValidPosition());
        assertEquals(1, parser.getFix3DNmea());
    }

    @Test
    public void testParseSentenceRMC() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPRMC,121041.000,A,4655.3772,N,01513.6390,E,0.31,319.55,220517,,*7");

        assertTrue(parser.isValidPosition());
        assertEquals("121041.000", parser.getTimeNmea());
        assertEquals("220517", parser.getDateNmea());
        assertEquals(15.227317, parser.getLongNmea(), EPS);
        assertEquals(46.922953, parser.getLatNmea(), EPS);
        assertEquals(0.159478, parser.getSpeedNmea(), EPS);
        assertEquals(319.55, parser.getTrackNmea(), EPS);
    }

    @Test
    public void testParseSentenceRMCInvalidStatus() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GPRMC,121041.000,V,4655.3772,N,01513.6390,E,0.31,319.55,220517,,*7");

        assertFalse(parser.isValidPosition());
        assertEquals("121041.000", parser.getTimeNmea());
        assertEquals("220517", parser.getDateNmea());
        assertEquals(15.227317, parser.getLongNmea(), EPS);
        assertEquals(46.922953, parser.getLatNmea(), EPS);
        assertEquals(0, parser.getFixQuality());
        assertEquals(0.159478, parser.getSpeedNmea(), EPS);
        assertEquals(319.55, parser.getTrackNmea(), EPS);
    }

    @Test
    public void testParseSentenceVTG() {
        NMEAParser parser = new NMEAParser();

        parser.parseSentence("$GNVTG,,,,,,,12.34,,,,*4a");

        assertEquals(12.34 / 3.6, parser.getSpeedNmea(), EPS);
    }

}
