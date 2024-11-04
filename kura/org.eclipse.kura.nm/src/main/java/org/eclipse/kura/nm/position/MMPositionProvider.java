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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.linux.position.options.PositionServiceOptions;
import org.eclipse.kura.linux.position.provider.GpsDeviceAvailabilityListener;
import org.eclipse.kura.linux.position.provider.LockStatusListener;
import org.eclipse.kura.linux.position.provider.PositionProvider;
import org.eclipse.kura.linux.position.provider.PositionProviderType;
import org.eclipse.kura.nm.NMDbusConnector;
import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.eclipse.kura.position.GNSSType;
import org.eclipse.kura.position.NmeaPosition;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.modem.Location;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMPositionProvider implements PositionProvider {

    private static final Logger logger = LoggerFactory.getLogger(MMPositionProvider.class);

    private static final UInt32 NMEA_LOCATION_SOURCE = new UInt32(
            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA.getValue());
    private static final UInt32 RAW_LOCATION_SOURCE = new UInt32(
            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW.getValue());

    private final NMDbusConnector nmDbusConnector;

    LockStatusListener gpsDeviceListener;

    ScheduledExecutorService positionRefreshTask;
    private int refreshRate;

    private final MMLocationParser mmLocationParser = new MMLocationParser();

    public MMPositionProvider() throws DBusException {
        this.nmDbusConnector = NMDbusConnector.getInstance();
    }

    public MMPositionProvider(NMDbusConnector connector) {
        this.nmDbusConnector = connector;
    }

    @Override
    public void start() {

        if (this.refreshRate <= 0) {
            logger.warn("Refresh rate not valid, only positive value are accepted");
            return;
        }

        this.positionRefreshTask = Executors.newSingleThreadScheduledExecutor();
        this.positionRefreshTask.scheduleAtFixedRate(this::getModemManagerLocation, 0, this.refreshRate,
                TimeUnit.SECONDS);
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
        return this.mmLocationParser.getPosition();
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        return this.mmLocationParser.getNmeaPosition();
    }

    @Override
    public String getNmeaTime() {
        return this.mmLocationParser.getNmeaTime();
    }

    @Override
    public String getNmeaDate() {
        return this.mmLocationParser.getNmeaDate();
    }

    @Override
    public LocalDateTime getDateTime() {
        return this.mmLocationParser.getLocalDateTime();
    }

    @Override
    public boolean isLocked() {
        return this.mmLocationParser.isFixed();
    }

    @Override
    public String getLastSentence() {
        throw new UnsupportedOperationException("LastSentence not available on ModemManagaer provider");
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
        return this.mmLocationParser.getGnssTypes();
    }

    private void getModemManagerLocation() {

        List<Location> availableLocations = this.nmDbusConnector.getAvailableMMLocations();

        boolean isLastPositionValid = this.mmLocationParser.isFixed();

        for (Location location : availableLocations) {
            Map<UInt32, Variant<?>> locationMap = location.GetLocation();
            if (locationMap.containsKey(NMEA_LOCATION_SOURCE) && locationMap.containsKey(RAW_LOCATION_SOURCE)) {
                Variant<?> nmeaData = locationMap.get(NMEA_LOCATION_SOURCE);
                this.mmLocationParser.parseNmeaLocation(nmeaData);

                Variant<?> rawData = locationMap.get(RAW_LOCATION_SOURCE);
                this.mmLocationParser.parseRawLocation(rawData);
            }
        }

        boolean isNewPositionValid = this.mmLocationParser.isFixed();

        if (this.gpsDeviceListener != null && isNewPositionValid != isLastPositionValid) {
            this.gpsDeviceListener.onLockStatusChanged(isNewPositionValid);
            logger.info("Lock Status changed: {}", this.mmLocationParser);
        }
    }

}
