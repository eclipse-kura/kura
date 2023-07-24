package org.eclipse.kura.nm.signal.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import fi.w1.wpa_supplicant1.Interface;

public class WpaScanDoneHandlerTest {

    @Test
    public void WpaScanDoneHandlerShouldTriggerWithExpectedDBusPath() {
        CountDownLatch mockLatch = mock(CountDownLatch.class);

        WPAScanDoneHandler handler = new WPAScanDoneHandler(mockLatch, "/fi/w1/wpa_supplicant1/Interfaces/0");

        Interface.ScanDone scanDone = mock(Interface.ScanDone.class);
        when(scanDone.getPath()).thenReturn("/fi/w1/wpa_supplicant1/Interfaces/0");

        handler.handle(scanDone);

        verify(mockLatch, times(1)).countDown();
    }

    @Test
    public void WpaScanDoneHandlerShouldNotTriggerWithDifferentDBusPath() {
        CountDownLatch mockLatch = mock(CountDownLatch.class);

        WPAScanDoneHandler handler = new WPAScanDoneHandler(mockLatch, "/fi/w1/wpa_supplicant1/Interfaces/0");

        Interface.ScanDone scanDone = mock(Interface.ScanDone.class);
        when(scanDone.getPath()).thenReturn("/fi/w1/wpa_supplicant1/Interfaces/6");

        handler.handle(scanDone);

        verify(mockLatch, never()).countDown();
    }
}
