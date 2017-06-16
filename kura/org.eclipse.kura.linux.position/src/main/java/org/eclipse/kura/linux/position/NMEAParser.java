/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
    private static boolean validPosition;

    /**
     * Fill the fields of GPS position depending of the type of the sentence
     *
     * @param sentence
     *            most recent sentence String from GPS modem
     */
    public void parseSentence(String sentence) {
        // first remove the end "*"+chksum
        int starpos = sentence.indexOf('*');
        String s_sentence = sentence.substring(0, starpos);

        String[] tokens = s_sentence.split(",");

        /*
         * Starting from 4.0 NMEA specs the GPS device can send messages representing different talkers
         *
         * $GP = GPS
         * $GS = Glonass
         * $GN = GNSS, that is GPS + Glonass + possibly others
         */
        if (!tokens[0].startsWith("$G")) {
            // Not a valid token. Return.
            return;
        }

        if (tokens[0].endsWith("GGA")) {
            if (tokens.length > 9) {
                validPosition = true;
                if (!tokens[1].isEmpty()) {
                    this.timeNmea = tokens[1];
                } else {
                    validPosition = false;
                }
                if (!tokens[2].isEmpty()) {
                    this.latNmea = convertPositionlat(tokens[2], tokens[3]);
                } else {
                    validPosition = false;
                }
                if (!tokens[4].isEmpty()) {
                    this.longNmea = convertPositionlon(tokens[4], tokens[5]);
                } else {
                    validPosition = false;
                }
                if (!tokens[6].isEmpty()) {
                    this.fixQuality = Integer.parseInt(tokens[6]);
                    if (this.fixQuality == 0) {
                        validPosition = false;
                    }
                } else {
                    validPosition = false;
                }
                if (!tokens[7].isEmpty()) {
                    this.nrSatellites = Integer.parseInt(tokens[7]);
                } else {
                    validPosition = false;
                }
                if (!tokens[8].isEmpty()) {
                    this.dopNmea = Double.parseDouble(tokens[8]);
                } else {
                    validPosition = false;
                }
                if (!tokens[9].isEmpty()) {
                    this.altNmea = Double.parseDouble(tokens[9]);
                } else {
                    validPosition = false;
                }
            } else {
                validPosition = false;
            }
        } else if (tokens[0].endsWith("GLL")) {
            if (tokens.length > 5) {
                validPosition = true;
                if (!tokens[1].isEmpty()) {
                    this.latNmea = convertPositionlat(tokens[1], tokens[2]);
                } else {
                    validPosition = false;
                }
                if (!tokens[3].isEmpty()) {
                    this.longNmea = convertPositionlon(tokens[3], tokens[4]);
                } else {
                    validPosition = false;
                }
                if (!tokens[5].isEmpty()) {
                    this.timeNmea = tokens[5];
                } else {
                    validPosition = false;
                }
                if (!tokens[6].isEmpty()) { // check validity
                    if (!new String("A").equals(tokens[6])) {
                        validPosition = false;
                    }
                } else {
                    validPosition = false;
                }
            } else {
                validPosition = false;
            }
        } else if (tokens[0].endsWith("RMC")) {
            if (tokens.length > 8) {
                validPosition = true;
                if (!tokens[1].isEmpty()) {
                    this.timeNmea = tokens[1];
                }
                if (!tokens[2].isEmpty()) { // check validity
                    if (!new String("A").equals(tokens[2])) {
                        validPosition = false;
                    }
                } else {
                    validPosition = false;
                }
                if (!tokens[3].isEmpty()) {
                    this.latNmea = convertPositionlat(tokens[3], tokens[4]);
                } else {
                    validPosition = false;
                }
                if (!tokens[5].isEmpty()) {
                    this.longNmea = convertPositionlon(tokens[5], tokens[6]);
                } else {
                    validPosition = false;
                }
                if (!tokens[7].isEmpty()) {
                    // conversion speed in knots to m/s : 1 m/s = 1.94384449 knots
                    this.speedNmea = Double.parseDouble(tokens[7]) / 1.94384449;
                }
                if (!tokens[8].isEmpty()) {
                    this.trackNmea = Double.parseDouble(tokens[8]);
                }
                if (!tokens[9].isEmpty()) {
                    this.dateNmea = tokens[9];
                } else {
                    validPosition = false;
                }
            } else {
                validPosition = false;
            }
        } else if (tokens[0].endsWith("GSA")) {
            if (tokens.length > 5) {
                validPosition = true;
                if (!tokens[2].isEmpty()) {
                    this.fix3DNmea = Integer.parseInt(tokens[2]);
                    if (this.fix3DNmea == 1) {
                        validPosition = false;
                    }
                } else {
                    validPosition = false;
                }
                int index = tokens.length - 3;
                if (!tokens[index].isEmpty()) {
                    this.pdopNmea = Double.parseDouble(tokens[index]);
                } else {
                    validPosition = false;
                }
                if (!tokens[index + 1].isEmpty()) {
                    this.hdopNmea = Double.parseDouble(tokens[index + 1]);
                } else {
                    validPosition = false;
                }
                if (!tokens[index + 2].isEmpty()) {
                    this.vdopNmea = Double.parseDouble(tokens[index + 2]);
                } else {
                    validPosition = false;
                }
            } else {
                validPosition = false;
            }
        } else if (tokens[0].endsWith("VTG")) {
            if (tokens.length > 7 && !tokens[7].isEmpty()) {
                // conversion km/h in m/s : 1 km/h -> 0,277777778 m/s;
                this.speedNmea = Double.parseDouble(tokens[7]) * 0.277777778;
            }
        }
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

    public boolean isValidPosition() {
        return validPosition;
    }

}
