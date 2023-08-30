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
package org.eclipse.kura.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.util.osgi.BundleUtil;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class BundleUtilTest {

    private Map<String, String> keyValueMap = new HashMap<>();
    private Class<?>[] servicesClasses;
    private Set<Bundle> bundles;
    private BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @Test
    public void shouldReturnGPIOBundle() {

        givenServicesPropertiesFilter("service.pid", "org.eclipse.kura.gpio.GPIOService");
        givenServiceClasses(new Class<?>[] { org.eclipse.kura.gpio.GPIOService.class });

        whenBundleListIsRequested();

        thenBundlesAre(1);
        thenBundleSymbolicNamesAre("org.eclipse.kura.emulator.gpio");
    }

    @Test
    public void shouldReturnWatchdogAndGPIOBundles() {
        givenServiceClasses(new Class<?>[] { org.eclipse.kura.gpio.GPIOService.class,
                org.eclipse.kura.watchdog.WatchdogService.class });

        whenBundleListIsRequested();

        thenBundlesAre(2);

        thenBundleSymbolicNamesAre("org.eclipse.kura.emulator.gpio", "org.eclipse.kura.watchdog");
    }

    @Test
    public void shouldReturnNoBundleWithWrongProperty() {
        givenServicesPropertiesFilter("service.pid", "wrongServicePid");
        givenServiceClasses(new Class<?>[] { org.eclipse.kura.gpio.GPIOService.class });

        whenBundleListIsRequested();

        thenBundlesAre(0);
    }

    @Test
    public void shouldReturnOnlyBundleWithRightProperty() {
        givenServicesPropertiesFilter("service.pid", "org.eclipse.kura.gpio.GPIOService");
        givenServiceClasses(new Class<?>[] { org.eclipse.kura.gpio.GPIOService.class,
                org.eclipse.kura.watchdog.WatchdogService.class });

        whenBundleListIsRequested();

        thenBundlesAre(1);
        thenBundleSymbolicNamesAre("org.eclipse.kura.emulator.gpio");
    }

    @Test
    public void shouldReturnGPIOBundleBySingleHeader() {
        givenBundleHeaders("Bundle-Name", "org.eclipse.kura.emulator.gpio");

        whenBundleListIsRequestByHeaders();

        thenBundlesAre(1);
        thenBundleSymbolicNamesAre("org.eclipse.kura.emulator.gpio");
    }

    @Test
    public void shouldReturnGPIOBundleByMultipleHeaders() {
        givenBundleHeaders("Bundle-Name", "org.eclipse.kura.emulator.gpio", "Bundle-Vendor", "Eclipse Kura");

        whenBundleListIsRequestByHeaders();

        thenBundlesAre(1);
        thenBundleSymbolicNamesAre("org.eclipse.kura.emulator.gpio");
    }

    @Test
    public void shouldReturnManyBundlesByHeader() {
        givenBundleHeaders("Bundle-Vendor", "Eclipse Kura");

        whenBundleListIsRequestByHeaders();

        thenBundlesAreMany();
    }

    private void givenBundleHeaders(String... headers) {
        for (int i = 0; i < headers.length; i = i + 2) {
            this.keyValueMap.put(headers[i], headers[i + 1]);
        }
    }

    private void givenServicesPropertiesFilter(String... properties) {
        for (int i = 0; i < properties.length; i = i + 2) {
            this.keyValueMap.put(properties[i], properties[i + 1]);
        }
    }

    private void givenServiceClasses(Class<?>[] classes) {
        this.servicesClasses = classes;

    }

    private void whenBundleListIsRequested() {
        this.bundles = BundleUtil.getBundles(this.bundleContext, this.servicesClasses, this.keyValueMap);

        printBundlesSymbolicName(this.bundles);
    }

    private void whenBundleListIsRequestByHeaders() {
        this.bundles = BundleUtil.getBundles(this.bundleContext, this.keyValueMap);

        printBundlesSymbolicName(this.bundles);
    }

    private void thenBundlesAre(int expectedNumberOfBundles) {
        assertEquals(expectedNumberOfBundles, this.bundles.size());

    }

    private void thenBundlesAreMany() {
        assertTrue("Too few Bundles", this.bundles.size() > 1);

    }

    private void thenBundleSymbolicNamesAre(String... symbolicNames) {
        List<String> bundleSymbolicNames = this.bundles.stream().map(Bundle::getSymbolicName)
                .collect(Collectors.toList());

        assertTrue("Not found all bundles", bundleSymbolicNames.containsAll(bundleSymbolicNames));
    }

    private void printBundlesSymbolicName(Set<Bundle> bundle) {
        String bundlesSymbolicName = bundles.stream().map(Bundle::getSymbolicName)
                .collect(Collectors.joining("", ",", ""));

        System.out.println("Bundles Found: " + bundlesSymbolicName);
    }
}
