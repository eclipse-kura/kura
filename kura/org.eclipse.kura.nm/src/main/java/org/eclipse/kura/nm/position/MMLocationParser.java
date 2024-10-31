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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.position.GNSSType;
import org.freedesktop.dbus.types.Variant;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMLocationParser {

    private static final Logger logger = LoggerFactory.getLogger(MMLocationParser.class);

    private static final double KNOTS_TO_M_S = 1 / 1.94384449;
    private int gnssTypeUpdateCounter = 0;
    private static final int GNSSTYPE_RESET_COUNTER = 50;

    private Double lat;
    private Double lon;
    private Double alt;
    private Double speed;
    private Double track;

    private LocalTime localTime;
    private LocalDate localDate;
    private Set<GNSSType> gnssTypes = new HashSet<>();
    private Boolean isFix = false;

    public LocalDateTime getLocalDateTime() {
        return Objects.requireNonNull(LocalDateTime.of(localDate, localTime));
    }

    public Position getPosition() {
        return Objects.requireNonNull(new Position(new Measurement(this.lat, Unit.rad),
                new Measurement(this.lon, Unit.rad), new Measurement(this.alt, Unit.m),
                new Measurement(this.speed, Unit.m_s), new Measurement(this.track, Unit.rad)));
    }

    public Set<GNSSType> getGnssTypes() {
        return Objects.requireNonNull(this.gnssTypes);
    }

    public boolean isFixed() {
        return this.isFix;
    }

    public void parseRawLocation(Variant<?> rawLocationVariant) {
        Map<String, Variant<?>> locationData = (Map<String, Variant<?>>) rawLocationVariant.getValue();
        for (Map.Entry<String, Variant<?>> rawEntry : locationData.entrySet()) {

            switch (rawEntry.getKey()) {
            case "latitude":
                this.lat = Math.toRadians((Double) rawEntry.getValue().getValue());
                break;

            case "longitude":
                this.lon = Math.toRadians((Double) rawEntry.getValue().getValue());
                break;

            case "altitude":
                this.alt = (Double) rawEntry.getValue().getValue();
                break;

            // time comes in format HHmmss.SS, so we cut the string after the dot to extract only the util information
            case "utc":
                this.localTime = LocalTime.parse((String) rawEntry.getValue().getValue(),
                        DateTimeFormatter.ofPattern("HHmmss.SS"));
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

                default:
                    // Do Nothing

                }
            }

        } catch (Exception ex) {
            logger.error("Failed to parse NMEA sentence: ", ex);
        }
    }

    private void parseGsaSentence(List<String> gsaTokens) {
        int fixType = Integer.parseInt(gsaTokens.get(2));
        if (fixType == 2 || fixType == 3) {
            this.isFix = true;
        } else {
            this.isFix = false;
        }
    }

    /*
     * Date is received in format dd-M-yy, so we convert it to yy-M-dd
     */
    private void parseRmcSentence(List<String> rmcTokens) {
        this.localDate = LocalDate.parse(rmcTokens.get(9), DateTimeFormatter.ofPattern("ddMyy"));

        this.speed = Double.parseDouble(rmcTokens.get(7)) * KNOTS_TO_M_S;
        this.track = Math.toRadians(Double.parseDouble(rmcTokens.get(8)));
        logger.info("\n\nRMC: {}, {}, {}", rmcTokens.get(8), Double.parseDouble(rmcTokens.get(8)),
                Math.toRadians(Double.parseDouble(rmcTokens.get(8))));
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
}
