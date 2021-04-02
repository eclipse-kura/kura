/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.tamper.detection.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.tamper.detection.TamperDetectionService;
import org.eclipse.kura.security.tamper.detection.TamperStatus;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@RunWith(Parameterized.class)
public class TamperDetectionRemoteServiceTest {

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(), new MqttTransport());
    }

    private final Transport transport;

    public TamperDetectionRemoteServiceTest(final Transport transport) {
        this.transport = transport;
        transport.init();
    }

    @Test
    public void shouldSupportListingTamperDetectionServices() throws MalformedURLException, IOException {
        assertEquals(0, runRequestAndGetResponse("list", "GET", new TypeToken<ArrayList<TamperDetectionServiceInfo>>() {
        }).size());
    }

    @Test
    public void shouldProvideTamperDetectionServiceInfo() throws MalformedURLException, IOException {
        try (final Fixture fixture = new Fixture()) {
            final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
            Mockito.when(tamperDetectionService.getDisplayName()).thenReturn("foo");

            fixture.registerService(tamperDetectionService, TamperDetectionService.class, "moo");

            final List<TamperDetectionServiceInfo> infos = runRequestAndGetResponse("list", "GET",
                    new TypeToken<ArrayList<TamperDetectionServiceInfo>>() {
                    });

            assertEquals(1, infos.size());
            assertEquals("foo", infos.get(0).getDisplayName());
            assertEquals("moo", infos.get(0).getPid());
        }
    }

    @Test
    public void shouldReportNotFound() throws MalformedURLException, IOException {
        try (final Fixture fixture = new Fixture()) {
            final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
            Mockito.when(tamperDetectionService.getDisplayName()).thenReturn("foo");

            fixture.registerService(tamperDetectionService, TamperDetectionService.class, "moo");

            assertEquals(404, transport.runRequestAndGetStatus("pid/boo", "GET"));
        }
    }

    @Test
    public void shouldProvideMultipleTamperDetectionServiceInfo() throws MalformedURLException, IOException {
        try (final Fixture fixture = new Fixture()) {

            final TamperDetectionService first = Mockito.mock(TamperDetectionService.class);
            Mockito.when(first.getDisplayName()).thenReturn("foo");
            fixture.registerService(first, TamperDetectionService.class, "moo");

            final TamperDetectionService second = Mockito.mock(TamperDetectionService.class);
            Mockito.when(second.getDisplayName()).thenReturn("boo");
            fixture.registerService(second, TamperDetectionService.class, "bar");

            final List<TamperDetectionServiceInfo> infos = runRequestAndGetResponse("list", "GET",
                    new TypeToken<ArrayList<TamperDetectionServiceInfo>>() {
                    });

            assertEquals(2, infos.size());
            assertTrue(infos.stream().anyMatch(p -> p.getPid().equals("moo") && p.getDisplayName().equals("foo")));
            assertTrue(infos.stream().anyMatch(p -> p.getPid().equals("bar") && p.getDisplayName().equals("boo")));
        }
    }

    @Test
    public void shouldReportTamperStatusInfo() throws MalformedURLException, IOException, KuraException {
        try (final Fixture fixture = new Fixture()) {
            final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
            Mockito.when(tamperDetectionService.getDisplayName()).thenReturn("foo");
            Mockito.when(tamperDetectionService.getTamperStatus())
                    .thenReturn(new TamperStatus(false, Collections.emptyMap()));

            fixture.registerService(tamperDetectionService, TamperDetectionService.class, "moo");

            final TamperStatusInfo info = runRequestAndGetResponse("pid/moo", "GET", new TypeToken<TamperStatusInfo>() {
            });

            assertEquals(false, info.isDeviceTampered);
            assertTrue(info.properties.isEmpty());
        }
    }

    @Test
    public void shouldReportTamperStatusInfoTimestamp() throws MalformedURLException, IOException, KuraException {
        try (final Fixture fixture = new Fixture()) {
            final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
            Mockito.when(tamperDetectionService.getDisplayName()).thenReturn("foo");
            Mockito.when(tamperDetectionService.getTamperStatus()).thenReturn(
                    new TamperStatus(true, Collections.singletonMap("timestamp", TypedValues.newLongValue(5000L))));

            fixture.registerService(tamperDetectionService, TamperDetectionService.class, "moo");

            final TamperStatusInfo info = runRequestAndGetResponse("pid/moo", "GET", new TypeToken<TamperStatusInfo>() {
            });

            assertEquals(true, info.isDeviceTampered);
            assertEquals(5000.0d, info.properties.get("timestamp"));
        }
    }

    @Test
    public void shouldSupportTamperStatusReset() throws MalformedURLException, IOException, KuraException {
        try (final Fixture fixture = new Fixture()) {
            final TamperDetectionService tamperDetectionService = new TamperDetectionService() {

                boolean isDeviceTampered = true;

                @Override
                public void resetTamperStatus() throws KuraException {
                    isDeviceTampered = false;
                }

                @Override
                public TamperStatus getTamperStatus() throws KuraException {
                    return new TamperStatus(isDeviceTampered, Collections.emptyMap());
                }

                @Override
                public String getDisplayName() {
                    return "Foo";
                }
            };

            fixture.registerService(tamperDetectionService, TamperDetectionService.class, "moo");

            assertEquals(true, runRequestAndGetResponse("pid/moo", "GET", new TypeToken<TamperStatusInfo>() {
            }).isDeviceTampered);

            assertEquals(1, transport.runRequestAndGetStatus("pid/moo/_reset", "POST") / 200);

            assertEquals(false, runRequestAndGetResponse("pid/moo", "GET", new TypeToken<TamperStatusInfo>() {
            }).isDeviceTampered);
        }
    }

    @Test
    public void shouldSupportTrackingByServicePid() throws MalformedURLException, IOException {
        try (final Fixture fixture = new Fixture()) {
            final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
            Mockito.when(tamperDetectionService.getDisplayName()).thenReturn("foo");

            fixture.registerService(tamperDetectionService, TamperDetectionService.class,
                    Collections.singletonMap("service.pid", "moo"));

            final List<TamperDetectionServiceInfo> infos = runRequestAndGetResponse("list", "GET",
                    new TypeToken<ArrayList<TamperDetectionServiceInfo>>() {
                    });

            assertEquals(1, infos.size());
            assertEquals("foo", infos.get(0).getDisplayName());
            assertEquals("moo", infos.get(0).getPid());
        }
    }

    private static class Fixture implements AutoCloseable {

        private final List<ServiceRegistration<?>> registeredServices = new ArrayList<>();

        public <T> void registerService(final T service, final Class<? super T> providedInterface, final String pid) {
            registerService(service, providedInterface, Collections.singletonMap("kura.service.pid", pid));
        }

        public <T> void registerService(final T service, final Class<? super T> providedInterface,
                final Map<String, Object> properties) {
            final BundleContext bundleContext = FrameworkUtil.getBundle(TamperDetectionRemoteServiceTest.class)
                    .getBundleContext();

            final Dictionary<String, Object> actualProperties = new Hashtable<>();

            for (final Entry<String, Object> e : properties.entrySet()) {
                actualProperties.put(e.getKey(), e.getValue());
            }

            registeredServices.add(bundleContext.registerService(providedInterface, service, actualProperties));
        }

        @Override
        public void close() {
            for (final ServiceRegistration<?> reg : registeredServices) {
                reg.unregister();
            }
        }

    }

    private <T> T runRequestAndGetResponse(final String resource, final String method,
            final TypeToken<T> responseType) {
        final String response = this.transport.runRequestAndGetResponse(resource, method);

        final Gson gson = new Gson();
        return gson.fromJson(response, responseType.getType());
    }

    public class TamperStatusInfo {

        private boolean isDeviceTampered;
        private Map<String, Object> properties;

        public boolean isDeviceTampered() {
            return isDeviceTampered;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }
    }

    public class TamperDetectionServiceInfo {

        private String pid;
        private String displayName;

        public String getPid() {
            return pid;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
