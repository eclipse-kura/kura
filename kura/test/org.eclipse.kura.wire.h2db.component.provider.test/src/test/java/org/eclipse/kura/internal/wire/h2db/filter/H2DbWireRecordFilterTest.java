/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.internal.wire.h2db.common.H2DbServiceHelper;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

public class H2DbWireRecordFilterTest {

    private H2DbService createMockH2DbService(final Connection connection) throws SQLException {
        final H2DbService dbServiceMock = mock(H2DbService.class);
        when(dbServiceMock.withConnection(anyObject())).thenAnswer(invocation -> {
            return invocation.getArgumentAt(0, H2DbService.ConnectionCallable.class).call(connection);
        });
        when(dbServiceMock.getConnection()).thenReturn(connection);
        return dbServiceMock;
    }

    @Test
    public void testActivateAndUpdated() throws NoSuchFieldException {
        // activate
        H2DbService mockDbService = mock(H2DbService.class);

        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        AtomicInteger resets = new AtomicInteger(0);
        H2DbWireRecordFilter filter = new H2DbWireRecordFilter() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(mockDbService);

                resets.set(resets.get() + 1);
            }
        };
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter, null)).thenReturn(mockWireSupport);

        int expectedCacheExpirationInterval = 10;
        String expectedSqlView = "view";
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", expectedCacheExpirationInterval);
        properties.put("sql.view", expectedSqlView);

        filter.activate(mock(ComponentContext.class), properties);

        assertEquals(1, resets.get());

        H2DbWireRecordFilterOptions options = (H2DbWireRecordFilterOptions) TestUtil.getFieldValue(filter, "options");

        assertEquals(expectedCacheExpirationInterval, options.getCacheExpirationInterval());
        assertEquals(expectedSqlView, options.getSqlView());

        H2DbServiceHelper dbHelper = (H2DbServiceHelper) TestUtil.getFieldValue(filter, "dbHelper");
        H2DbService dbService = (H2DbService) TestUtil.getFieldValue(dbHelper, "dbService");
        assertEquals(mockDbService, dbService);

        verify(mockWireHelperService).newWireSupport(filter, null);
        WireSupport wireSupport = (WireSupport) TestUtil.getFieldValue(filter, "wireSupport");
        assertEquals(mockWireSupport, wireSupport);

        int cacheExpirationInterval = (int) TestUtil.getFieldValue(filter, "cacheExpirationInterval");
        assertEquals(expectedCacheExpirationInterval, cacheExpirationInterval);

        Calendar lastRefreshedTime = (Calendar) TestUtil.getFieldValue(filter, "lastRefreshedTime");
        Calendar minTime = Calendar.getInstance();
        minTime.add(Calendar.SECOND, -cacheExpirationInterval);
        assertTrue(minTime.compareTo(lastRefreshedTime) >= 0);

        // updated
        expectedCacheExpirationInterval = 20;
        expectedSqlView = "updated view";
        properties.put("cache.expiration.interval", expectedCacheExpirationInterval);
        properties.put("sql.view", expectedSqlView);
        properties.put("db.service.pid", "newdbservicepid"); // trigger tracker reset

        filter.updated(properties);

        assertEquals(2, resets.get());

        options = (H2DbWireRecordFilterOptions) TestUtil.getFieldValue(filter, "options");

        assertEquals(expectedCacheExpirationInterval, options.getCacheExpirationInterval());
        assertEquals(expectedSqlView, options.getSqlView());

        lastRefreshedTime = (Calendar) TestUtil.getFieldValue(filter, "lastRefreshedTime");
        minTime = Calendar.getInstance();
        minTime.add(Calendar.SECOND, -cacheExpirationInterval);
        assertTrue(minTime.compareTo(lastRefreshedTime) >= 0);

        // deactivate
        filter.deactivate(null);
    }

    @Test
    public void testConsumersConnected() {
        H2DbService mockDbService = mock(H2DbService.class);

        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        H2DbWireRecordFilter filter = new H2DbWireRecordFilter() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(mockDbService);
            }
        };
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter, null)).thenReturn(mockWireSupport);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(mock(ComponentContext.class), properties);

        Wire[] wires = new Wire[2];
        wires[0] = mock(Wire.class);
        wires[1] = mock(Wire.class);

        filter.consumersConnected(wires);

        verify(mockWireSupport).consumersConnected(wires);
    }

    @Test
    public void testOnWireReceive() throws SQLException {
        Connection mockConnection = mock(Connection.class);

        H2DbService mockDbService = createMockH2DbService(mockConnection);

        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        H2DbWireRecordFilter filter = new H2DbWireRecordFilter() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(mockDbService);
            }
        };
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter, null)).thenReturn(mockWireSupport);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "sql command");

        filter.activate(mock(ComponentContext.class), properties);

        ResultSetMetaData mockResultSetMetaData = mock(ResultSetMetaData.class);
        when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
        when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("data1");

        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);
        when(mockResultSet.getObject(1)).thenReturn(42);

        Statement mockStatement = mock(Statement.class);
        when(mockStatement.executeQuery("sql command")).thenReturn(mockResultSet);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        WireEnvelope mockWireEnvelope = mock(WireEnvelope.class);
        filter.onWireReceive(mockWireEnvelope);

        verify(mockWireSupport).emit(any());
    }

    @Test
    public void testPolled() {
        H2DbService mockDbService = mock(H2DbService.class);

        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        H2DbWireRecordFilter filter = new H2DbWireRecordFilter() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(mockDbService);
            }
        };
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter, null)).thenReturn(mockWireSupport);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(mock(ComponentContext.class), properties);

        Wire mockWire = mock(Wire.class);
        filter.polled(mockWire);

        verify(mockWireSupport).polled(mockWire);
    }

    @Test
    public void testProducersConnected() {
        H2DbService mockDbService = mock(H2DbService.class);

        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        H2DbWireRecordFilter filter = new H2DbWireRecordFilter() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(mockDbService);
            }
        };
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter, null)).thenReturn(mockWireSupport);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(mock(ComponentContext.class), properties);

        Wire[] wires = new Wire[2];
        wires[0] = mock(Wire.class);
        wires[1] = mock(Wire.class);

        filter.producersConnected(wires);

        verify(mockWireSupport).producersConnected(wires);
    }

    @Test
    public void testUpdatedWireObject() {
        H2DbService mockDbService = mock(H2DbService.class);

        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        H2DbWireRecordFilter filter = new H2DbWireRecordFilter() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(mockDbService);
            }
        };
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter, null)).thenReturn(mockWireSupport);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(mock(ComponentContext.class), properties);

        Wire mockWire = mock(Wire.class);
        filter.updated(mockWire, 42);

        verify(mockWireSupport).updated(mockWire, 42);
    }

}
