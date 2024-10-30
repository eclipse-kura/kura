package org.eclipse.kura.nm.position;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMPositionProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(MMPositionProviderTest.class);

    MMPositionProvider provider;
    NMDbusConnector mockNmDbusConnector;
    Position retrievedPosition;

    @Test
    public void shouldRetrieveCorrectPosition() throws InterruptedException {

        givenModemManagerFakeLocation();
        givenPositionProviderWithMockDbusConnector();
        givenProviderInitAndStart();

        whenServiceAskForPosition();

        thenPositionIsCorrect(0, 0, 0, 0, 0);
    }

    /*
     * The return location is a gps point in Rome retrieved with {@link https://nmeagen.org/}
     */
    private void givenModemManagerFakeLocation() {

        Scanner scanner = new Scanner(MMPositionProviderTest.class.getResourceAsStream("/fakeNmeaSentences.txt"),
                "UTF-8");
        CharSequence locationString = scanner.useDelimiter("\\A").next().replace(" ", "");
        scanner.close();

        Location mockLocation = mock(Location.class);
        Map<UInt32, Variant<?>> variantMap = new HashMap<>();
        variantMap.put(new UInt32(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA.getValue()),
                new Variant<>(locationString));
        when(mockLocation.GetLocation()).thenReturn(variantMap);

        NMDbusConnector dbusConnector = mock(NMDbusConnector.class);
        when(dbusConnector.getAvailableMMLocations()).thenReturn(Arrays.asList(mockLocation));

        this.mockNmDbusConnector = dbusConnector;
    }

    private void givenPositionProviderWithMockDbusConnector() {
        this.provider = new MMPositionProvider(this.mockNmDbusConnector);
    }

    private void givenProviderInitAndStart() throws InterruptedException {
        this.provider.init(new PositionServiceOptions(Collections.emptyMap()), mock(LockStatusListener.class),
                mock(GpsDeviceAvailabilityListener.class));
        this.provider.start();
    }

    private void whenServiceAskForPosition() throws InterruptedException {
        Thread.sleep(100000);
        this.retrievedPosition = this.provider.getPosition();
    }

    private void thenPositionIsCorrect(double lat, double lon, double alt, double speed, double track) {
        logger.error("\n\n{}\n\n", this.retrievedPosition);

        assertEquals(this.retrievedPosition.getLatitude(), new Measurement(lat, Unit.rad));
        assertEquals(this.retrievedPosition.getLongitude(), new Measurement(lon, Unit.rad));
        assertEquals(this.retrievedPosition.getAltitude(), new Measurement(alt, Unit.m));
        assertEquals(this.retrievedPosition.getSpeed(), new Measurement(speed, Unit.m_s));
        assertEquals(this.retrievedPosition.getTrack(), new Measurement(track, Unit.rad));
    }

}
