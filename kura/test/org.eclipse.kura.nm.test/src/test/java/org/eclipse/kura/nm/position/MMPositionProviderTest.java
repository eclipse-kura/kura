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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.kura.linux.position.options.PositionServiceOptions;
import org.eclipse.kura.linux.position.provider.GpsDeviceAvailabilityListener;
import org.eclipse.kura.linux.position.provider.LockStatusListener;
import org.eclipse.kura.nm.NMDbusConnector;
import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.modem.Location;
import org.junit.Test;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;

public class MMPositionProviderTest {

    MMPositionProvider provider;
    NMDbusConnector mockNmDbusConnector;
    Position retrievedPosition;
    LocalDateTime retrieveDateTime;

    @Test
    public void shouldRetrieveCorrectPosition() throws InterruptedException {

        givenModemManagerFakeLocation(39.5, 9.7, 4.5);
        givenPositionProviderWithMockDbusConnector();
        givenProviderInitAndStart();

        whenServiceAskForPosition();

        thenPositionIsCorrect(39.5, 9.7, 4.5, 0, 10.2);
        thenDateTimeIsCorrect("2024-10-29T10:33:55");
    }

    /*
     * The return location is a gps point in Rome retrieved with {@link https://nmeagen.org/}
     */
    private void givenModemManagerFakeLocation(double lat, double lon, double alt) {

        Scanner scanner = new Scanner(MMPositionProviderTest.class.getResourceAsStream("/fakeNmeaSentences.txt"),
                "UTF-8");
        CharSequence locationString = scanner.useDelimiter("\\A").next().replace(" ", "");
        scanner.close();

        Location mockLocation = mock(Location.class);
        Map<UInt32, Variant<?>> variantMap = new HashMap<>();
        variantMap.put(new UInt32(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA.getValue()),
                new Variant<>(locationString));

        Map<String, Variant<?>> rawLocationMap = new HashMap<>();
        rawLocationMap.put("latitude", new Variant<>(lat, Double.class));
        rawLocationMap.put("longitude", new Variant<>(lon, Double.class));
        rawLocationMap.put("altitude", new Variant<>(alt, Double.class));
        rawLocationMap.put("utc-time", new Variant<>("103355", String.class));

        variantMap.put(new UInt32(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW.getValue()),
                new Variant<>(rawLocationMap, "a{sv}"));

        when(mockLocation.GetLocation()).thenReturn(variantMap);

        NMDbusConnector dbusConnector = mock(NMDbusConnector.class);
        when(dbusConnector.getAvailableMMLocations()).thenReturn(Arrays.asList(mockLocation));

        this.mockNmDbusConnector = dbusConnector;
    }

    private void givenPositionProviderWithMockDbusConnector() {
        this.provider = new MMPositionProvider(this.mockNmDbusConnector);
    }

    private void givenProviderInitAndStart() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("modem.manager.refresh.rate.seconds", 1);
        PositionServiceOptions options = new PositionServiceOptions(properties);

        this.provider.init(options, mock(LockStatusListener.class), mock(GpsDeviceAvailabilityListener.class));
        this.provider.start();
    }

    private void whenServiceAskForPosition() throws InterruptedException {
        Thread.sleep(3000);
        this.retrievedPosition = this.provider.getPosition();
        this.retrieveDateTime = this.provider.getDateTime();
    }

    private void thenPositionIsCorrect(double lat, double lon, double alt, double speed, double track) {
        assertEquals(new Measurement(Math.toRadians(lat), Unit.rad), this.retrievedPosition.getLatitude());
        assertEquals(new Measurement(Math.toRadians(lon), Unit.rad), this.retrievedPosition.getLongitude());
        assertEquals(new Measurement(alt, Unit.m), this.retrievedPosition.getAltitude());
        assertEquals(new Measurement(speed, Unit.m_s), this.retrievedPosition.getSpeed());
        assertEquals(new Measurement(Math.toRadians(track), Unit.rad), this.retrievedPosition.getTrack());
    }

    private void thenDateTimeIsCorrect(String expectedDateTime) {
        assertEquals(expectedDateTime, this.retrieveDateTime.toString());
    }

}
