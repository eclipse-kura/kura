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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import org.osgi.util.position.Position;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.internal.rest.position.PositionRestService;
import org.eclipse.kura.rest.position.api.IsLockedDTO;
import org.eclipse.kura.rest.position.api.DateTimeDTO;
import org.eclipse.kura.rest.position.api.PositionDTO;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
        givenPosition(0.1, 0.2, 4.5);

        whenGetPosition();

        thenPositionIs(0.1, 0.2, 4.5);
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
    public void getPositionNoLockTest(){
        givenMockedPositionService();
        givenIsLocked(false);

        whenGetPosition();

        thenExceptionIsThrown();
    }

    @Test
    public void getLocalDateTimeNoLockTest(){
        givenMockedPositionService();
        givenIsLocked(false);

        whenGetLocalDateTime();

        thenExceptionIsThrown();
    }


    private void givenMockedPositionService(){
        positionRestService.setPositionServiceImpl(positionService);
    }

    private void givenPosition(double longitude, double latitude, double altitude) {
        Position testPosition = new Position(new Measurement(Math.toRadians(latitude), Unit.rad),
                new Measurement(Math.toRadians(longitude), Unit.rad), new Measurement(altitude, Unit.m), null, null);
        when(positionService.getPosition()).thenReturn(testPosition);
    }

    private void givenLocalDateTime(String zonedDateTime) {
        LocalDateTime testLocalDateTime = LocalDateTime.parse(zonedDateTime);
        when(positionService.getDateTime()).thenReturn(testLocalDateTime);
    }

    private void givenIsLocked(boolean isLocked){
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
        try{
            localDateTimeDTO = positionRestService.getLocalDateTime();
        } catch (Exception e) {
            testException = e;
        }
    }

    private void whenGetIsLocked() {
        try{
            isLockedDTO = positionRestService.getIsLocked();
        } catch (Exception e) {
            testException = e;
        }
    }

    private void thenPositionIs(double longitude, double latitude, double altitude) {
        assertEquals(longitude, positionDTO.getLongitude(), 0.0);
        assertEquals(latitude, positionDTO.getLatitude(), 0.0);
        assertEquals(altitude, positionDTO.getAltitude(), 0.0);
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
