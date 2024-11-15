/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.nm.position;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.position.GNSSType;
import org.eclipse.kura.position.NmeaPosition;
import org.freedesktop.dbus.types.Variant;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMLocationParser {

    private static final Logger logger = LoggerFactory.getLogger(MMLocationParser.class);

    private static final double KNOTS_TO_MS = 1 / 1.94384449;

    private int gnssTypeUpdateCounter = 0;
    private static final int GNSSTYPE_RESET_COUNTER = 50;

    private Double latitudeDegrees = 0.0;
    private Double longitudeDegrees = 0.0;
    private Double altitudeMeters = 0.0;
    private Double speedMetersPerSecond = 0.0;
    private Double trackDegrees = 0.0;

    private int fixQuality;
    private int nrSatellites;
    private double mDOP;
    private double mPDOP;
    private double mHDOP;
    private double mVDOP;
    private int m3Dfix;
    private char latitudeHemisphere = 0;
    private char longitudeHemisphere = 0;
    private char validFix = 0;

    private LocalTime time;
    private LocalDate date;
    private Set<GNSSType> gnssTypes = new HashSet<>();
    private Boolean isFix = false;

    public LocalDateTime getLocalDateTime() {
        if (!Objects.isNull(this.time) && !Objects.isNull(this.date)) {
            return LocalDateTime.of(date, time);
        }

        return null;
    }

    /*
     * Conversions are required since org.osgi.util.position.Position requires values in radians for latitude, longitude
     * and track.
     */
    public Position getPosition() {
        return Objects.requireNonNull(new Position(new Measurement(Math.toRadians(this.latitudeDegrees), Unit.rad),
                new Measurement(Math.toRadians(this.longitudeDegrees), Unit.rad),
                new Measurement(this.altitudeMeters, Unit.m), new Measurement(this.speedMetersPerSecond, Unit.m_s),
                new Measurement(Math.toRadians(this.trackDegrees), Unit.rad)));
    }

    public Set<GNSSType> getGnssTypes() {
        return Objects.requireNonNull(this.gnssTypes);
    }

    public boolean isFixed() {
        return this.isFix;
    }

    public NmeaPosition getNmeaPosition() {
        return new NmeaPosition(this.latitudeDegrees, this.longitudeDegrees, this.altitudeMeters,
                this.speedMetersPerSecond, this.trackDegrees, this.fixQuality, this.nrSatellites, this.mDOP, this.mPDOP,
                this.mHDOP, this.mVDOP, this.m3Dfix, this.validFix, this.latitudeHemisphere, this.longitudeHemisphere);
    }

    public String getNmeaTime() {
        return this.time.toString();
    }

    public String getNmeaDate() {
        return this.date.toString();
    }

    /*
     * Used by MMPositionProvider to invalidate position in case ModemManager doesn't provide any data, neither the nmea
     * nor the raw. Usually, the Nmea data is always provided, while the Raw is provided only if Nmea has fix.
     */

    public void setInvalidFix() {
        this.isFix = false;

        this.latitudeDegrees = 0.0;
        this.longitudeDegrees = 0.0;
        this.altitudeMeters = 0.0;
        this.speedMetersPerSecond = 0.0;
        this.trackDegrees = 0.0;

        LocalDateTime utcDateTime = LocalDateTime.now(ZoneOffset.UTC);
        this.time = utcDateTime.toLocalTime();
        this.date = utcDateTime.toLocalDate();
    }

    public void parseRawLocation(Variant<?> rawLocationVariant) {
        Map<String, Variant<?>> locationData = (Map<String, Variant<?>>) rawLocationVariant.getValue();
        for (Map.Entry<String, Variant<?>> rawEntry : locationData.entrySet()) {

            switch (rawEntry.getKey()) {
            case "latitude":
                this.latitudeDegrees = (Double) rawEntry.getValue().getValue();
                break;

            case "longitude":
                this.longitudeDegrees = (Double) rawEntry.getValue().getValue();
                break;

            case "altitude":
                this.altitudeMeters = (Double) rawEntry.getValue().getValue();
                break;

            // time comes in format HHmmss.SS, so we cut the string after the dot to extract only the util information
            case "utc-time":
                String utcTime = ((String) rawEntry.getValue().getValue());
                this.time = LocalTime.parse(utcTime.split("\\.")[0], DateTimeFormatter.ofPattern("HHmmss"));
                break;

            default:
                // DO NOTHING
            }
        }
    }

    public void parseNmeaLocation(Variant<?> nmeaLocationVariant) {
        try {
            String locationString = ((CharSequence) nmeaLocationVariant.getValue()).toString();

            List<String> nmeaSentences = Arrays.asList(locationString.split("\\r?\\n|\\r")).stream()
                    .filter(sentence -> !sentence.isEmpty()).collect(Collectors.toList());

            for (String sentence : nmeaSentences) {

                int starpos = sentence.indexOf('*');
                final List<String> tokens = Arrays.asList(sentence.substring(0, starpos).split(","));

                String sentenceGnss = sentence.substring(1, 3);

                this.gnssTypes.add(getGnssTypeFromSentenceId(sentenceGnss));

                String sentenceType = sentence.substring(3, 6);

                switch (sentenceType) {

                case "GSA":
                    parseGsaSentence(tokens);
                    break;

                case "RMC":
                    parseRmcSentence(tokens);
                    break;

                case "GGA":
                    parseGgaSentence(tokens);
                    break;

                default:
                    // Do Nothing

                }
            }

        } catch (Exception ex) {
            logger.error("Failed to parse NMEA sentence: ", ex);
            this.isFix = false;
        }
    }

    private void parseGgaSentence(List<String> gsaTokens) {
        if (gsaTokens.size() > 9) {
            if (!gsaTokens.get(7).isEmpty()) {
                this.nrSatellites = Integer.parseInt(gsaTokens.get(7));
            }
            if (!gsaTokens.get(8).isEmpty()) {
                this.mDOP = Double.parseDouble(gsaTokens.get(8));
            }
        } else {
            setInvalidFix();
        }
    }

    private void parseGsaSentence(List<String> gsaTokens) {

        if (gsaTokens.size() > 5) {
            if (!gsaTokens.get(2).isEmpty()) {

                int fixType = Integer.parseInt(gsaTokens.get(2));
                this.fixQuality = fixType;
                if (fixType == 2 || fixType == 3) {
                    this.isFix = true;
                } else {
                    this.isFix = false;
                }
            }

            if (!gsaTokens.get(15).isEmpty()) {
                this.mPDOP = Double.parseDouble(gsaTokens.get(15));
            }
            if (!gsaTokens.get(16).isEmpty()) {
                this.mHDOP = Double.parseDouble(gsaTokens.get(16));
            }
            if (!gsaTokens.get(17).isEmpty()) {
                this.mVDOP = Double.parseDouble(gsaTokens.get(17));
            }
        } else {
            setInvalidFix();
        }
    }

    /*
     * Date is received in format dd-M-yy, so we convert it to yy-M-dd
     */
    private void parseRmcSentence(List<String> rmcTokens) {
        if (rmcTokens.size() > 9) {
            if (!rmcTokens.get(9).isEmpty()) {
                this.date = LocalDate.parse(rmcTokens.get(9), DateTimeFormatter.ofPattern("ddMyy"));
            }
            if (!rmcTokens.get(7).isEmpty()) {
                this.speedMetersPerSecond = Double.parseDouble(rmcTokens.get(7)) * KNOTS_TO_MS;
            }
            if (!rmcTokens.get(8).isEmpty()) {
                this.trackDegrees = Double.parseDouble(rmcTokens.get(8));
            }
            if (!rmcTokens.get(4).isEmpty()) {
                this.latitudeHemisphere = rmcTokens.get(4).charAt(0);
            }
            if (!rmcTokens.get(6).isEmpty()) {
                this.longitudeHemisphere = rmcTokens.get(6).charAt(0);
            }
            if (!rmcTokens.get(2).isEmpty()) { // check validity
                parseRmcFixValidity(rmcTokens.get(2));
            } else {
                this.validFix = 'V';
                this.isFix = false;
            }
        } else {
            setInvalidFix();
        }
    }

    private void parseRmcFixValidity(String validityToken) {
        this.validFix = validityToken.charAt(0);
        if (!"A".equals(validityToken)) {
            this.isFix = false;
        } else {
            this.isFix = true;
        }
    }

    private GNSSType getGnssTypeFromSentenceId(String type) {

        if (this.gnssTypeUpdateCounter > GNSSTYPE_RESET_COUNTER) {
            this.gnssTypes.clear();
            this.gnssTypeUpdateCounter = 0;
        }

        return sentenceIdToGnssType(type);
    }

    /*
     * Also 'GN' is a possible GNSSType, representing the Mixed GNSS System (GPS+GALILEO or GPS+GLONASS for example).
     * 
     * But, if the device is capable to emit GN sentences, it must emit also the single-id ones. So we are still able to
     * extract the specific GNSS System. See {@link
     * https://receiverhelp.trimble.com/alloy-gnss/en-us/NMEA-0183messages_GNS.html}
     * 
     * As example, if the device emits GN messages due to a GP/GA combination, it will emit three sentences: GN, GP, GA.
     * 
     * Info about the correlation GNSS System / NMEA Sentence at
     * {@link https://en.wikipedia.org/wiki/NMEA_0183#NMEA_sentence_format}
     * 
     */
    private GNSSType sentenceIdToGnssType(String type) {

        switch (type) {

        case "GP":
            return GNSSType.GPS;

        case "BD":
        case "GB":
            return GNSSType.BEIDOU;

        case "GA":
            return GNSSType.GALILEO;

        case "GL":
            return GNSSType.GLONASS;

        case "GI":
            return GNSSType.IRNSS;

        case "GQ":
            return GNSSType.QZSS;

        default:
            return GNSSType.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return "ModemManagerProvider [isFix=" + isFix + ", latitude=" + latitudeDegrees + ", longitude="
                + longitudeDegrees + ", altitude=" + altitudeMeters + ", speed=" + speedMetersPerSecond + ", timestamp="
                + time + ", date=" + date + ", gnssType=" + gnssTypes + "]";
    }
}
