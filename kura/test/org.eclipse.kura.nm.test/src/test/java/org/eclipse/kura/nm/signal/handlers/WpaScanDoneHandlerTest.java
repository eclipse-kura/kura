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
 *******************************************************************************/
package org.eclipse.kura.nm.signal.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;

import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.Test;

import fi.w1.wpa_supplicant1.Interface;

public class WpaScanDoneHandlerTest {

    private CountDownLatch mockLatch = mock(CountDownLatch.class);
    private WPAScanDoneHandler handler;

    @Test
    public void WpaScanDoneHandlerShouldTriggerWithExpectedDBusPath() throws DBusException {

        givenWpaScanDoneHandlerWithDBusPath("/fi/w1/wpa_supplicant1/Interfaces/0");

        whenHandlerReceivesScanDoneSignalWith("/fi/w1/wpa_supplicant1/Interfaces/0");

        thenLatchShouldBeCountedDown();
    }

    @Test
    public void WpaScanDoneHandlerShouldNotTriggerWithDifferentDBusPath() throws DBusException {
        givenWpaScanDoneHandlerWithDBusPath("/fi/w1/wpa_supplicant1/Interfaces/0");

        whenHandlerReceivesScanDoneSignalWith("/fi/w1/wpa_supplicant1/Interfaces/6");

        thenLatchShouldNotBeUpdated();
    }

    /*
     * Given
     */

    private void givenWpaScanDoneHandlerWithDBusPath(String dbusPath) {
        this.handler = new WPAScanDoneHandler(this.mockLatch, dbusPath);
    }

    /*
     * When
     */

    private void whenHandlerReceivesScanDoneSignalWith(String dbusPath) throws DBusException {
        Interface.ScanDone scanDoneSignal = new Interface.ScanDone(dbusPath, true);
        handler.handle(scanDoneSignal);
    }

    /*
     * Then
     */
    private void thenLatchShouldBeCountedDown() {
        verify(this.mockLatch, times(1)).countDown();
    }

    private void thenLatchShouldNotBeUpdated() {
        verify(this.mockLatch, never()).countDown();
    }
}
