/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.wire.filter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.internal.wire.common.DbServiceHelper;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.service.wireadmin.Wire;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DbWireRecordFilterTest {

    @Test
    public void testActivateAndUpdated() throws NoSuchFieldException {
        // Activate
        DbService mockDbService = mock(DbService.class);
        
        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        DbWireRecordFilter filter = new DbWireRecordFilter();
        filter.bindDbService(mockDbService);
        filter.bindWireHelperService(mockWireHelperService);
        
        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter)).thenReturn(mockWireSupport);
        
        int expectedCacheExpirationInterval = 10;
        String expectedSqlView = "view";
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", expectedCacheExpirationInterval);
        properties.put("sql.view", expectedSqlView);

        filter.activate(null, properties);
        
        DbWireRecordFilterOptions options = (DbWireRecordFilterOptions) TestUtil.getFieldValue(filter, "options");

        assertEquals(expectedCacheExpirationInterval, options.getCacheExpirationInterval());
        assertEquals(expectedSqlView, options.getSqlView());
        
        DbServiceHelper dbHelper = (DbServiceHelper) TestUtil.getFieldValue(filter, "dbHelper");
        DbService dbService = (DbService) TestUtil.getFieldValue(dbHelper, "dbService");
        assertEquals(mockDbService, dbService);
        
        verify(mockWireHelperService).newWireSupport(filter);
        WireSupport wireSupport = (WireSupport) TestUtil.getFieldValue(filter, "wireSupport");
        assertEquals(mockWireSupport, wireSupport);
        
        int cacheExpirationInterval = (int) TestUtil.getFieldValue(filter, "cacheExpirationInterval");
        assertEquals(expectedCacheExpirationInterval, cacheExpirationInterval);
        
        Calendar lastRefreshedTime = (Calendar) TestUtil.getFieldValue(filter, "lastRefreshedTime");
        Calendar minTime = Calendar.getInstance();
        minTime.add(Calendar.SECOND, -cacheExpirationInterval);
        assertTrue(minTime.compareTo(lastRefreshedTime) >= 0);
        
        // Updated
        expectedCacheExpirationInterval = 20;
        expectedSqlView = "updated view";
        properties.put("cache.expiration.interval", expectedCacheExpirationInterval);
        properties.put("sql.view", expectedSqlView);
        
        filter.updated(properties);
        
        options = (DbWireRecordFilterOptions) TestUtil.getFieldValue(filter, "options");

        assertEquals(expectedCacheExpirationInterval, options.getCacheExpirationInterval());
        assertEquals(expectedSqlView, options.getSqlView());
        
        lastRefreshedTime = (Calendar) TestUtil.getFieldValue(filter, "lastRefreshedTime");
        minTime = Calendar.getInstance();
        minTime.add(Calendar.SECOND, -cacheExpirationInterval);
        assertTrue(minTime.compareTo(lastRefreshedTime) >= 0);
    }

    @Test
    public void testConsumersConnected() {
        DbService mockDbService = mock(DbService.class);
        
        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        DbWireRecordFilter filter = new DbWireRecordFilter();
        filter.bindDbService(mockDbService);
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter)).thenReturn(mockWireSupport);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(null, properties);
        
        Wire[] wires = new Wire[2];
        wires[0] = mock(Wire.class);
        wires[1] = mock(Wire.class);

        filter.consumersConnected(wires);
        
        verify(mockWireSupport).consumersConnected(wires);
    }

    @Test
    public void testOnWireReceive() throws SQLException {
        DbService mockDbService = mock(DbService.class);
        
        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        DbWireRecordFilter filter = new DbWireRecordFilter();
        filter.bindDbService(mockDbService);
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter)).thenReturn(mockWireSupport);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "sql command");

        filter.activate(null, properties);

        ResultSetMetaData mockResultSetMetaData = mock(ResultSetMetaData.class);
        when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
        when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("data1");
        
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);
        when(mockResultSet.getObject(1)).thenReturn(42);
        
        Connection mockConnection = mock(Connection.class);
        when(mockDbService.getConnection()).thenReturn(mockConnection);
        
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.executeQuery("sql command")).thenReturn(mockResultSet);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        
        WireEnvelope mockWireEnvelope = mock(WireEnvelope.class);
        filter.onWireReceive(mockWireEnvelope);
        
        verify(mockWireSupport).emit(any());
    }
    
    @Test
    public void testPolled() {
        DbService mockDbService = mock(DbService.class);
        
        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        DbWireRecordFilter filter = new DbWireRecordFilter();
        filter.bindDbService(mockDbService);
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter)).thenReturn(mockWireSupport);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(null, properties);
        
        Wire mockWire = mock(Wire.class);
        filter.polled(mockWire);
        
        verify(mockWireSupport).polled(mockWire);
    }

    @Test
    public void testProducersConnected() {
        DbService mockDbService = mock(DbService.class);
        
        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        DbWireRecordFilter filter = new DbWireRecordFilter();
        filter.bindDbService(mockDbService);
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter)).thenReturn(mockWireSupport);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(null, properties);
        
        Wire[] wires = new Wire[2];
        wires[0] = mock(Wire.class);
        wires[1] = mock(Wire.class);

        filter.producersConnected(wires);
        
        verify(mockWireSupport).producersConnected(wires);
    }

    @Test
    public void testUpdatedWireObject() {
        DbService mockDbService = mock(DbService.class);
        
        WireHelperService mockWireHelperService = mock(WireHelperService.class);

        DbWireRecordFilter filter = new DbWireRecordFilter();
        filter.bindDbService(mockDbService);
        filter.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(filter)).thenReturn(mockWireSupport);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("cache.expiration.interval", 10);
        properties.put("sql.view", "view");

        filter.activate(null, properties);
        
        Wire mockWire = mock(Wire.class);
        filter.updated(mockWire, 42);
        
        verify(mockWireSupport).updated(mockWire, 42);
    }

}
