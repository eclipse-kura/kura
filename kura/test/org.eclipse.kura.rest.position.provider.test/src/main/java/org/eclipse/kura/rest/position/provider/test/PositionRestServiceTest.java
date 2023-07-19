/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import org.osgi.util.position.Position;
import org.osgi.util.measurement.Measurement;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.internal.rest.position.PositionRestService;
import org.eclipse.kura.rest.position.api.IsLockedDTO;
import org.eclipse.kura.rest.position.api.LocalDateTimeDTO;
import org.eclipse.kura.rest.position.api.PositionDTO;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PositionRestServiceTest {

    PositionService positionService = mock(PositionService.class);
    PositionRestService positionRestService = new PositionRestService();

    private IsLockedDTO isLockedDTO;
    private PositionDTO positionDTO;
    private LocalDateTimeDTO localDateTimeDTO;

    @Test
    public void getPositionTest() throws KuraException {
        givenIsLocked(true);
        givenPosition(1.0, 1.0, 4.5);

        whenGetPosition();

        thenPositionIs(1.0, 2.3, 4.5);
    }

    @Test
    public void getLocalDateTimeTest() throws KuraException {
        givenIsLocked(true);
        givenLocalDateTime("2023-07-19T18:26:38Z");

        whenGetLocalDateTime();

        thenLocalDateTimeIs("2023-07-19T18:26:38Z");
    }

    @Test
    public void getIsLockedTest() throws KuraException {
        givenIsLocked(true);

        whenGetIsLocked();

        thenIsLockedIs(true);
    }

    private void givenPosition(double longitude, double latitude, double altitude){
        Position testPosition = new Position(new Measurement(Math.toRadians(latitude)), new Measurement(Math.toRadians(longitude)), new Measurement(altitude), new Measurement(0.0), new Measurement(0.0));
        when(positionService.getPosition()).thenReturn(testPosition);
    }

    private void givenLocalDateTime(String zonedDateTime){
        LocalDateTime testLocalDateTime = LocalDateTime.parse(zonedDateTime);
        when(positionService.getDateTime()).thenReturn(testLocalDateTime);
    }

    private void givenIsLocked(boolean isLocked){
        when(positionService.isLocked()).thenReturn(isLocked);
    }

    private void whenGetPosition() throws KuraException {
        positionDTO = positionRestService.getPosition();
    }

    private void whenGetLocalDateTime() throws KuraException {
        localDateTimeDTO = positionRestService.getLocalDateTime();
    }

    private void whenGetIsLocked() throws KuraException {
        isLockedDTO = positionRestService.getIsLocked();
    }

    private void thenPositionIs(double longitude, double latitude, double altitude){
        assertEquals(longitude, positionDTO.getLongitude(), 0.0);
        assertEquals(latitude, positionDTO.getLatitude(), 0.0);
        assertEquals(altitude, positionDTO.getAltitude(), 0.0);
    }

    private void thenLocalDateTimeIs(String zonedDateTime){
        assertEquals(zonedDateTime, localDateTimeDTO.getLocalDateTime());
    }

    private void thenIsLockedIs(boolean isLocked){
        assertEquals(isLocked, isLockedDTO.getIsLocked());
    }

}
