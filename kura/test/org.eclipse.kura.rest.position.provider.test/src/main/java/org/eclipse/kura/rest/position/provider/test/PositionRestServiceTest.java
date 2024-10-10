/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.position.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.internal.rest.position.PositionRestService;
import org.eclipse.kura.position.GNSSType;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.rest.position.api.DateTimeDTO;
import org.eclipse.kura.rest.position.api.IsLockedDTO;
import org.eclipse.kura.rest.position.api.PositionDTO;
import org.junit.Test;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;

public class PositionRestServiceTest {

    PositionService positionService = mock(PositionService.class);
    PositionRestService positionRestService = new PositionRestService();

    private IsLockedDTO isLockedDTO;
    private PositionDTO positionDTO;
    private DateTimeDTO localDateTimeDTO;

    private Exception testException;

    @Test
    public void getPositionTest() {
        givenMockedPositionService();
        givenIsLocked(true);
        givenPosition(0.1, 0.2, 4.5, null, null, null);

        whenGetPosition();

        thenPositionIs(0.1, 0.2, 4.5, null, null, null);
        thenNoExceptionIsThrown();
    }

    @Test
    public void getPositionTestWithFullParameters() {
        givenMockedPositionService();
        givenIsLocked(true);
        givenPosition(0.1, 0.2, 4.5, 50.6, 9.8, new HashSet<>(Arrays.asList(GNSSType.GPS)));

        whenGetPosition();

        thenPositionIs(0.1, 0.2, 4.5, 50.6, 9.8, "Gps");
        thenNoExceptionIsThrown();
    }

    @Test
    public void getPositionTestWithFullParametersAndMultipleGNSSTypes() {
        givenMockedPositionService();
        givenIsLocked(true);
        givenPosition(0.1, 0.2, 4.5, 50.6, 9.8, new HashSet<>(Arrays.asList(GNSSType.GPS, GNSSType.GLONASS)));

        whenGetPosition();

        thenPositionIs(0.1, 0.2, 4.5, 50.6, 9.8, "MixedGNSSTypes");
        thenNoExceptionIsThrown();
    }

    @Test
    public void getLocalDateTimeTest() {
        givenMockedPositionService();
        givenIsLocked(true);
        givenLocalDateTime("2023-07-19T18:26:38");

        whenGetLocalDateTime();

        thenLocalDateTimeIs("2023-07-19T18:26:38Z");
        thenNoExceptionIsThrown();
    }

    @Test
    public void getIsLockedTest() {
        givenMockedPositionService();
        givenIsLocked(true);

        whenGetIsLocked();

        thenIsLockedIs(true);
        thenNoExceptionIsThrown();
    }

    @Test
    public void getIsLockedTestFalse() {
        givenMockedPositionService();
        givenIsLocked(false);

        whenGetIsLocked();

        thenIsLockedIs(false);
        thenNoExceptionIsThrown();
    }

    @Test
    public void getPositionNoLockTest() {
        givenMockedPositionService();
        givenIsLocked(false);

        whenGetPosition();

        thenExceptionIsThrown();
    }

    @Test
    public void getLocalDateTimeNoLockTest() {
        givenMockedPositionService();
        givenIsLocked(false);

        whenGetLocalDateTime();

        thenExceptionIsThrown();
    }

    private void givenMockedPositionService() {
        positionRestService.setPositionServiceImpl(positionService);
    }

    private void givenPosition(Double longitude, Double latitude, Double altitude, Double speed, Double track,
            Set<GNSSType> gnssTypeSet) {

        Measurement latitudeMesurment = latitude != null ? new Measurement(Math.toRadians(latitude), Unit.rad) : null;
        Measurement longitudeMesurment = longitude != null ? new Measurement(Math.toRadians(longitude), Unit.rad)
                : null;
        Measurement altitudeMesurment = altitude != null ? new Measurement(altitude, Unit.m) : null;
        Measurement speedMesurment = speed != null ? new Measurement(speed, Unit.m_s) : null;
        Measurement trackMesurment = track != null ? new Measurement(Math.toRadians(track), Unit.rad) : null;

        Position testPosition = new Position(latitudeMesurment, longitudeMesurment, altitudeMesurment, speedMesurment,
                trackMesurment);

        when(positionService.getPosition()).thenReturn(testPosition);
        when(positionService.getGnssType()).thenReturn(gnssTypeSet);
    }

    private void givenLocalDateTime(String zonedDateTime) {
        LocalDateTime testLocalDateTime = LocalDateTime.parse(zonedDateTime);
        when(positionService.getDateTime()).thenReturn(testLocalDateTime);
    }

    private void givenIsLocked(boolean isLocked) {
        when(positionService.isLocked()).thenReturn(isLocked);
    }

    private void whenGetPosition() {
        try {
            positionDTO = positionRestService.getPosition();
        } catch (Exception e) {
            testException = e;
        }
    }

    private void whenGetLocalDateTime() {
        try {
            localDateTimeDTO = positionRestService.getLocalDateTime();
        } catch (Exception e) {
            testException = e;
        }
    }

    private void whenGetIsLocked() {
        try {
            isLockedDTO = positionRestService.getIsLocked();
        } catch (Exception e) {
            testException = e;
        }
    }

    private void thenPositionIs(Double longitude, Double latitude, Double altitude, Double speed, Double track,
            String gnssType) {

        if (longitude != null) {
            assertEquals(longitude, positionDTO.getLongitude(), 0.0);
        } else {
            assertNull(positionDTO.getLongitude());
        }

        if (latitude != null) {
            assertEquals(latitude, positionDTO.getLatitude(), 0.0);
        } else {
            assertNull(positionDTO.getLatitude());
        }

        if (altitude != null) {
            assertEquals(altitude, positionDTO.getAltitude(), 0.0);
        } else {
            assertNull(positionDTO.getAltitude());
        }

        if (speed != null) {
            assertEquals(speed, positionDTO.getSpeed(), 0.0);
        } else {
            assertNull(positionDTO.getSpeed());
        }

        if (track != null) {
            assertEquals(track, positionDTO.getTrack(), 0.0);
        } else {
            assertNull(positionDTO.getTrack());
        }

        if (gnssType != null) {
            assertEquals(gnssType, positionDTO.getGnssType());
        } else {
            assertNull(positionDTO.getGnssType());
        }
    }

    private void thenLocalDateTimeIs(String zonedDateTime) {
        assertEquals(zonedDateTime, localDateTimeDTO.getDateTime());
    }

    private void thenIsLockedIs(boolean isLocked) {
        assertEquals(isLocked, isLockedDTO.getIsLocked());
    }

    private void thenNoExceptionIsThrown() {
        assertEquals(null, testException);
    }

    private void thenExceptionIsThrown() {
        assertEquals(WebApplicationException.class, testException.getClass());
    }
}
