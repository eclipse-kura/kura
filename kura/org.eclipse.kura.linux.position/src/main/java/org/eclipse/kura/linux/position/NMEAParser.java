/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.position;

import static java.lang.Math.toRadians;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;

/**
 * Implements NMEA sentences parser functions.
 *
 */
public class NMEAParser {

    private int fixQuality;
    private String timeNmea;
    private String dateNmea;
    private double longNmea;
    private double latNmea;
    private double speedNmea;
    private double altNmea;
    private double trackNmea;
    private double dopNmea;
    private double pdopNmea;
    private double hdopNmea;
    private double vdopNmea;
    private int fix3DNmea;
    private int nrSatellites;
    private boolean validPosition;
    private char validFix = 0;
    private char latitudeHemisphere = 0;
    private char longitudeHemisphere = 0;

    /**
     * Fill the fields of GPS position depending of the type of the sentence
     *
     * @param sentence
     *            most recent sentence String from GPS modem
     */
    public boolean parseSentence(String sentence) throws ParseException {

        if (!computeNMEACksum(sentence)) {
            throw new ParseException(Code.BAD_CHECKSUM);
        }

        // first remove the end "*"+chksum
        int starpos = sentence.indexOf('*');
        final List<String> tokens = Arrays.asList(sentence.substring(0, starpos).split(","));

        /*
         * Starting from 4.0 NMEA specs the GPS device can send messages representing different talkers
         *
         * $GP = GPS
         * $GS = Glonass
         * $GN = GNSS, that is GPS + Glonass + possibly others
         */
        if (!tokens.get(0).startsWith("$G")) {
            // Not a valid token. Return.
            throw new ParseException(Code.INVALID);
        }

        if (tokens.get(0).endsWith("GGA")) {
            parseGGASentence(tokens);
        } else if (tokens.get(0).endsWith("GLL")) {
            parseGLLSentence(tokens);
        } else if (tokens.get(0).endsWith("RMC")) {
            parseRMCSentence(tokens);
        } else if (tokens.get(0).endsWith("GSA")) {
            parseGSASentence(tokens);
        } else if (tokens.get(0).endsWith("VTG")) {
            parseVTGSentence(tokens);
        } else if (!tokens.get(0).endsWith("GSV") && sentence.indexOf("FOM") == -1 && sentence.indexOf("PPS") == -1) {
            throw new ParseException(Code.UNRECOGNIZED);
        }

        return this.validPosition;
    }

    private void parseVTGSentence(List<String> tokens) {
        if (tokens.size() > 7 && !tokens.get(7).isEmpty()) {
            // conversion km/h in m/s : 1 km/h -> 0,277777778 m/s
            this.speedNmea = Double.parseDouble(tokens.get(7)) * 0.277777778;
        }
    }

    private void parseGSASentence(List<String> tokens) {
        if (tokens.size() > 5) {
            // Check only last 3 items for validity
            checkPosition(tokens.subList(tokens.size() - 3, tokens.size()), 3);
            if (!tokens.get(2).isEmpty()) {
                this.fix3DNmea = Integer.parseInt(tokens.get(2));
                if (this.fix3DNmea == 1) {
                    this.validPosition = false;
                }
            }
            int index = tokens.size() - 3;
            if (!tokens.get(index).isEmpty()) {
                this.pdopNmea = Double.parseDouble(tokens.get(index));
            }
            if (!tokens.get(index + 1).isEmpty()) {
                this.hdopNmea = Double.parseDouble(tokens.get(index + 1));
            }
            if (!tokens.get(index + 2).isEmpty()) {
                this.vdopNmea = Double.parseDouble(tokens.get(index + 2));
            }
        } else {
            this.validPosition = false;
        }
    }

    private void parseRMCSentence(List<String> tokens) {
        if (tokens.size() > 9) {
            if (!tokens.get(1).isEmpty()) {
                this.timeNmea = tokens.get(1);
            }
            if (!tokens.get(2).isEmpty()) { // check validity
                this.validFix = tokens.get(2).charAt(0);
                if (!"A".equals(tokens.get(2))) {
                    this.validPosition = false;
                } else {
                    this.validPosition = true;
                }
            } else {
                this.validFix = 'V';
                this.validPosition = false;
            }
            if (!tokens.get(3).isEmpty() && !tokens.get(4).isEmpty()) {
                this.latNmea = convertPositionlat(tokens.get(3), tokens.get(4));
                this.latitudeHemisphere = tokens.get(4).charAt(0);
            }
            if (!tokens.get(5).isEmpty() && !tokens.get(6).isEmpty()) {
                this.longNmea = convertPositionlon(tokens.get(5), tokens.get(6));
                this.longitudeHemisphere = tokens.get(6).charAt(0);
            }
            if (!tokens.get(7).isEmpty()) {
                // conversion speed in knots to m/s : 1 m/s = 1.94384449 knots
                this.speedNmea = Double.parseDouble(tokens.get(7)) / 1.94384449;
            }
            if (!tokens.get(8).isEmpty()) {
                this.trackNmea = Double.parseDouble(tokens.get(8));
            }
            if (!tokens.get(9).isEmpty()) {
                this.dateNmea = tokens.get(9);
            }
        } else {
            this.validPosition = false;
        }
    }

    private void parseGLLSentence(List<String> tokens) {
        if (tokens.size() > 6) {
            checkPosition(tokens, 7);
            if (!tokens.get(1).isEmpty() && !tokens.get(2).isEmpty()) {
                this.latNmea = convertPositionlat(tokens.get(1), tokens.get(2));
                this.latitudeHemisphere = tokens.get(2).charAt(0);
            }
            if (!tokens.get(3).isEmpty() && !tokens.get(4).isEmpty()) {
                this.longNmea = convertPositionlon(tokens.get(3), tokens.get(4));
                this.longitudeHemisphere = tokens.get(4).charAt(0);
            }
            if (!tokens.get(5).isEmpty()) {
                this.timeNmea = tokens.get(5);
            }
            if (!tokens.get(6).isEmpty() && !"A".equals(tokens.get(6))) { // check validity
                this.validPosition = false;
            }
        } else {
            this.validPosition = false;
        }
    }

    private void parseGGASentence(List<String> tokens) {
        if (tokens.size() > 9) {
            checkPosition(tokens, 10);
            if (!tokens.get(1).isEmpty()) {
                this.timeNmea = tokens.get(1);
            }
            if (!tokens.get(2).isEmpty() && !tokens.get(3).isEmpty()) {
                this.latNmea = convertPositionlat(tokens.get(2), tokens.get(3));
                this.latitudeHemisphere = tokens.get(3).charAt(0);
            }
            if (!tokens.get(4).isEmpty() && !tokens.get(5).isEmpty()) {
                this.longNmea = convertPositionlon(tokens.get(4), tokens.get(5));
                this.longitudeHemisphere = tokens.get(5).charAt(0);
            }
            if (!tokens.get(6).isEmpty()) {
                this.fixQuality = Integer.parseInt(tokens.get(6));
                if (this.fixQuality == 0) {
                    this.validPosition = false;
                }
            }
            if (!tokens.get(7).isEmpty()) {
                this.nrSatellites = Integer.parseInt(tokens.get(7));
            }
            if (!tokens.get(8).isEmpty()) {
                this.dopNmea = Double.parseDouble(tokens.get(8));
            }
            if (!tokens.get(9).isEmpty()) {
                this.altNmea = Double.parseDouble(tokens.get(9));
            }
        } else {
            this.validPosition = false;
        }
    }

    private void checkPosition(List<String> tokens, int size) {
        // Create a list containing the indexes of empty tokens
        this.validPosition = tokens.stream().limit(size).noneMatch(String::isEmpty);
    }

    /**
     * @param pos
     *            DDD?MM?.dddd
     * @param direction
     *            N/S, E/W
     * @param degChars
     *            number of characters representing degrees
     * @return
     */
    private double convertPosition(String pos, String direction, int degChars) {
        if (pos.length() < 6) {
            return 0;
        }

        String s = pos.substring(0, degChars);
        double deg = Double.parseDouble(s);
        s = pos.substring(degChars);
        double min = Double.parseDouble(s);
        deg = deg + min / 60;
        if (direction.contains("S") || direction.contains("W")) {
            deg = -deg;
        }
        return deg;
    }

    double convertPositionlat(String pos, String direction) {
        return convertPosition(pos, direction, 2);
    }

    double convertPositionlon(String pos, String direction) {
        return convertPosition(pos, direction, 3);
    }

    boolean computeNMEACksum(String nmeaMessageIn) {
        final int starpos = nmeaMessageIn.indexOf('*');
        final String strChecksum = nmeaMessageIn.substring(starpos + 1, nmeaMessageIn.length() - 1);
        final int parsedChecksum = Integer.parseInt(strChecksum, 16); // Check sum is coded in hex string

        int actualChecksum = 0;
        for (int i = 1; i < starpos; i++) {
            actualChecksum ^= nmeaMessageIn.charAt(i);
        }

        return actualChecksum == parsedChecksum;
    }

    public String getTimeNmea() {
        return this.timeNmea;
    }

    public int getFixQuality() {
        return this.fixQuality;
    }

    public String getDateNmea() {
        return this.dateNmea;
    }

    public double getLongNmea() {
        return this.longNmea;
    }

    public double getLatNmea() {
        return this.latNmea;
    }

    public double getSpeedNmea() {
        return this.speedNmea;
    }

    public double getAltNmea() {
        return this.altNmea;
    }

    public double getTrackNmea() {
        return this.trackNmea;
    }

    public double getDOPNmea() {
        return this.dopNmea;
    }

    public double getPDOPNmea() {
        return this.pdopNmea;
    }

    public double getHDOPNmea() {
        return this.hdopNmea;
    }

    public double getVDOPNmea() {
        return this.vdopNmea;
    }

    public int getFix3DNmea() {
        return this.fix3DNmea;
    }

    public int getNrSatellites() {
        return this.nrSatellites;
    }

    public Measurement getLatitude() {
        return new Measurement(toRadians(this.latNmea), Unit.rad);
    }

    public Measurement getLongitude() {
        return new Measurement(toRadians(this.longNmea), Unit.rad);
    }

    public Measurement getAltitude() {
        return new Measurement(this.altNmea, Unit.m);
    }

    public Measurement getSpeed() {
        return new Measurement(this.speedNmea, Unit.m_s);
    }

    public Measurement getTrack() {
        return new Measurement(toRadians(this.trackNmea), Unit.rad);
    }

    public Position getPosition() {
        return new Position(getLatitude(), getLongitude(), getAltitude(), getSpeed(), getTrack());
    }

    public NmeaPosition getNmeaPosition() {
        return new NmeaPosition(getLatNmea(), getLongNmea(), getAltNmea(), getSpeedNmea(), getTrackNmea(),
                getFixQuality(), getNrSatellites(), getDOPNmea(), getPDOPNmea(), getHDOPNmea(), getVDOPNmea(),
                getFix3DNmea(), getValidFix(), getLatitudeHemisphere(), getLongitudeHemisphere());
    }

    public boolean isValidPosition() {
        return this.validPosition;
    }

    public char getValidFix() {
        return this.validFix;
    }

    public char getLatitudeHemisphere() {
        return this.latitudeHemisphere;
    }

    public char getLongitudeHemisphere() {
        return this.longitudeHemisphere;
    }

    public enum Code {
        INVALID,
        BAD_CHECKSUM,
        UNRECOGNIZED
    }

    public class ParseException extends Exception {

        private static final long serialVersionUID = -1441433820817330483L;
        private final Code code;

        public ParseException(final Code code) {
            this.code = code;
        }

        public Code getCode() {
            return this.code;
        }
    }
}
