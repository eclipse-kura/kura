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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.kura.linux.position.options.PositionServiceOptions;
import org.eclipse.kura.linux.position.provider.GpsDeviceAvailabilityListener;
import org.eclipse.kura.linux.position.provider.LockStatusListener;
import org.eclipse.kura.linux.position.provider.NMEAParser;
import org.eclipse.kura.linux.position.provider.NMEAParser.ParseException;
import org.eclipse.kura.linux.position.provider.PositionProvider;
import org.eclipse.kura.linux.position.provider.PositionProviderType;
import org.eclipse.kura.nm.ModemManagerDbusWrapper;
import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.eclipse.kura.position.GNSSType;
import org.eclipse.kura.position.NmeaPosition;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.modemmanager1.modem.Location;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMPositionProvider implements PositionProvider {

    private static final Logger logger = LoggerFactory.getLogger(MMPositionProvider.class);

    private DateTimeFormatter nmeaDateTimePattern = DateTimeFormatter.ofPattern("ddMMyy hhmmss");

    private final DBusConnection dbusConnection;
    private final ModemManagerDbusWrapper mmWrapper;

    Map<String, Modem> gpsEnabledModems;

    LockStatusListener gpsDeviceListener;

    ScheduledExecutorService positionRefreshTask;
    private int refreshRate;

    NMEAParser nmeaParser = new NMEAParser();

    public MMPositionProvider() throws DBusException {
        this.dbusConnection = DBusConnection.getConnection(DBusConnection.DEFAULT_SYSTEM_BUS_ADDRESS);
        this.mmWrapper = new ModemManagerDbusWrapper(this.dbusConnection);
    }

    @Override
    public void start() {

        this.positionRefreshTask = null;

        this.gpsEnabledModems = getGpsEnabledMap();

        if (!this.gpsEnabledModems.isEmpty()) {
            this.positionRefreshTask = Executors.newSingleThreadScheduledExecutor();
            this.positionRefreshTask.scheduleAtFixedRate(this::getModemManagerLocation, 0, refreshRate,
                    TimeUnit.SECONDS);
        }

    }

    @Override
    public void stop() {
        this.positionRefreshTask.shutdownNow();
        try {
            this.positionRefreshTask.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            this.positionRefreshTask = null;
        }

    }

    @Override
    public Position getPosition() {
        return this.nmeaParser.getPosition();
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        return this.nmeaParser.getNmeaPosition();
    }

    @Override
    public String getNmeaTime() {
        return this.nmeaParser.getTimeNmea();
    }

    @Override
    public String getNmeaDate() {
        return this.nmeaParser.getDateNmea();
    }

    @Override
    public LocalDateTime getDateTime() {
        String nmeaDateTime = this.getNmeaDate() + " " + this.getNmeaTime();
        return LocalDateTime.parse(nmeaDateTime, nmeaDateTimePattern);
    }

    @Override
    public boolean isLocked() {
        return this.nmeaParser.isValidPosition();
    }

    @Override
    public String getLastSentence() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(PositionServiceOptions configuration, LockStatusListener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {

        this.gpsDeviceListener = gpsDeviceListener;
        this.refreshRate = configuration.getModemManagerRefreshRate();

    }

    @Override
    public PositionProviderType getType() {
        return PositionProviderType.MODEM_MANAGER;
    }

    @Override
    public Set<GNSSType> getGnssTypes() {
        return this.nmeaParser.getGnssTypes();
    }

    private Map<String, Modem> getGpsEnabledMap() {

        Map<String, Modem> gpsEnabledMap = new HashMap<>();

        for (Map.Entry<String, Modem> entryMap : this.mmWrapper.getEnabledModems().entrySet()) {
            try {
                String modemPath = entryMap.getKey();
                Modem modem = entryMap.getValue();

                Location location = dbusConnection.getRemoteObject("org.freedesktop.ModemManager1", modemPath,
                        Location.class);

                Properties locationProps = dbusConnection.getRemoteObject("org.freedesktop.ModemManager1",
                        location.getObjectPath(), Properties.class);

                Set<MMModemLocationSource> sources = MMModemLocationSource.toMMModemLocationSourceFromBitMask(
                        locationProps.Get("org.freedesktop.ModemManager1.Modem.Location", "Enabled"));
                if (sources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA)
                        && sources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW)) {
                    gpsEnabledMap.put(modemPath, modem);
                }
            } catch (DBusException e) {
                logger.debug("Impossible to retrieve information regarding modem: {}", entryMap.getKey());
            }
        }

        return gpsEnabledMap;
    }

    private void getModemManagerLocation() {

        for (Map.Entry<String, Modem> entry : this.gpsEnabledModems.entrySet()) {
            try {
                Location location = dbusConnection.getRemoteObject("org.freedesktop.ModemManager1", entry.getKey(),
                        Location.class);
                Map<UInt32, Variant<?>> locationData = location.GetLocation();

                for (Map.Entry<UInt32, Variant<?>> locationEntry : locationData.entrySet()) {
                    if (MMModemLocationSource.toMMModemLocationSource(locationEntry.getKey())
                            .equals(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA)) {

                        parseNmeaLocation(locationEntry.getValue());
                    }
                }
            } catch (DBusException ex) {
                logger.warn("Impossible to retrieve location from modem {}", entry.getKey());
            }
        }
    }

    private void parseNmeaLocation(Variant<?> locationVariant) {
        String locationString = ((CharSequence) locationVariant.getValue()).toString();

        List<String> nmeaSentences = Arrays.asList(locationString.split("\\r?\\n|\\r")).stream().filter(sentence -> {
            return !sentence.isEmpty();
        }).collect(Collectors.toList());

        nmeaSentences.stream().forEach(sentence -> {
            try {
                this.nmeaParser.parseSentence(sentence);
                if (this.gpsDeviceListener != null) {
                    this.gpsDeviceListener.newNmeaSentence(sentence);
                }
            } catch (ParseException e) {
                logger.error("Error parsing sentence {}", sentence.substring(0, 6));
            }
        });

    }

}
