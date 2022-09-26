/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.configuration.change.manager.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.change.manager.ComponentsServiceTracker;
import org.eclipse.kura.configuration.change.manager.ServiceTrackerListener;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public class ComponentsServiceTrackerTest {

    private ComponentsServiceTracker tracker;
    private ServiceTrackerListener listener;

    /*
     * Scenarios
     */

    @Test
    public void addingServiceWithKuraPidShouldNotifyListener() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();

        whenAddingServiceWithKuraPid("example1");

        thenListenerIsNotified("example1");
    }

    @Test
    public void addingServiceWithServiceFactoryPidShouldNotifyListener() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();

        whenAddingServiceWithServiceFactoryPid("example1");

        thenListenerIsNotified("example1");
    }

    @Test
    public void addingServiceWithServicePidShouldNotifyListener() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();

        whenAddingServiceWithServicePid("example1");

        thenListenerIsNotified("example1");
    }

    @Test
    public void addingServiceWithWithUnknownPropertyShouldNotNotifyListener() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();

        whenAddingServiceWithUnknownProperty();

        thenListenerIsNotNotified();
    }

    @Test
    public void modifiedServiceShouldNotifyListener() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();

        whenModifiedService("modified-pid");

        thenListenerIsNotified("modified-pid");
    }

    @Test
    public void removedServiceShouldNotifyListener() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();

        whenRemovedService("removed-pid");

        thenListenerIsNotified("removed-pid");
    }

    @Test
    public void notRegisteredListenerShouldNotBeNotified() throws InvalidSyntaxException {
        givenComponentsServiceTracker();
        givenServiceTrackerListener();
        givenRemoveServiceTrackerListener();

        whenRemovedService("removed-pid2");

        thenListenerIsNotNotified();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenComponentsServiceTracker() throws InvalidSyntaxException {
        BundleContext mockContext = mock(BundleContext.class);
        Filter mockFilter = mock(Filter.class);

        when(mockContext.createFilter(Mockito.anyString())).thenReturn(mockFilter);
        when(mockContext.getService(Mockito.any())).thenReturn(new Object());
        when(mockContext.ungetService(Mockito.any())).thenReturn(true);
        this.tracker = new ComponentsServiceTracker(mockContext);
    }

    private void givenServiceTrackerListener() {
        this.listener = mock(ServiceTrackerListener.class);
        this.tracker.addServiceTrackerListener(this.listener);
    }

    private void givenRemoveServiceTrackerListener() {
        this.tracker.removeServiceTrackerListener(this.listener);
    }

    /*
     * When
     */

    private void whenAddingServiceWithKuraPid(String kuraPid) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> ref = (ServiceReference<Object>) mock(ServiceReference.class);
        when(ref.getProperty(ConfigurationService.KURA_SERVICE_PID)).thenReturn(kuraPid);

        callAddingService(ref);
    }

    private void whenAddingServiceWithServiceFactoryPid(String serviceFactoryPid) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> ref = (ServiceReference<Object>) mock(ServiceReference.class);
        when(ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID)).thenReturn(serviceFactoryPid);
        
        callAddingService(ref);
    }

    private void whenAddingServiceWithServicePid(String servicePid) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> ref = (ServiceReference<Object>) mock(ServiceReference.class);
        when(ref.getProperty(Constants.SERVICE_PID)).thenReturn(servicePid);

        callAddingService(ref);
    }

    private void whenAddingServiceWithUnknownProperty() {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> ref = (ServiceReference<Object>) mock(ServiceReference.class);
        when(ref.getProperty("unknwonKey")).thenReturn("unknownValue");

        callAddingService(ref);
    }

    private void whenModifiedService(String kuraPid) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> ref = (ServiceReference<Object>) mock(ServiceReference.class);
        when(ref.getProperty(ConfigurationService.KURA_SERVICE_PID)).thenReturn(kuraPid);

        this.tracker.modifiedService(ref, new Object());
    }

    private void whenRemovedService(String kuraPid) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> ref = (ServiceReference<Object>) mock(ServiceReference.class);
        when(ref.getProperty(ConfigurationService.KURA_SERVICE_PID)).thenReturn(kuraPid);

        this.tracker.removedService(ref, new Object());
    }

    private void callAddingService(ServiceReference<Object> reference) {
        this.tracker.addingService(reference);
    }

    /*
     * Then
     */

    private void thenListenerIsNotified(String changedPid) {
        verify(this.listener, times(1)).onConfigurationChanged(changedPid);
    }

    private void thenListenerIsNotNotified() {
        verify(this.listener, times(0)).onConfigurationChanged(Mockito.any());
    }

}
