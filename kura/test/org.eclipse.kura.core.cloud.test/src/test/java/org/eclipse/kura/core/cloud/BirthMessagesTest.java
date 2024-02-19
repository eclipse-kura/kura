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
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.security.tamper.detection.TamperEvent;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class BirthMessagesTest {

    private static final String BIRTH_TOPIC_PREFIX = "$EDC" + CloudServiceOptions.getTopicSeparator()
            + CloudServiceOptions.getTopicAccountToken() + CloudServiceOptions.getTopicSeparator()
            + CloudServiceOptions.getTopicClientIdToken() + CloudServiceOptions.getTopicSeparator();

    private static final long SEND_DELAY = 25000L;
    private static final long SLACK_DELAY = 10000L;

    private CloudServiceImpl cloudService = new CloudServiceImpl();
    private DataService dataService = mock(DataService.class);
    private Exception occurredException;
    private Event event;

    /*
     * Scenarios
     */

    @Test
    public void shouldNotPublishOnActivationWhenDisconnected() throws KuraException {
        givenDisconnected();

        whenActivate();

        thenNoExceptionOccurred();
        thenNoBirthIsPublished();
    }

    @Test
    public void shouldPublishImmediatelyOnActivationWhenConnected() throws KuraException {
        givenConnected();

        whenActivate();

        thenNoExceptionOccurred();
        thenBirthIsPublishedImmediately(BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldNotPublishOnUpdatedWhenDisconnected() throws KuraException {
        givenDisconnected();

        whenUpdated();

        thenNoExceptionOccurred();
        thenNoBirthIsPublished();
    }

    @Test
    public void shouldPublishWithDelayOnUpdateWhenConnected() throws KuraException {
        givenConnected();

        whenUpdated();

        thenNoExceptionOccurred();
        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldPublishImmediatelyWhenDeactivate() throws KuraException {
        givenConfiguredCloudService();
        givenConnected();

        whenDeactivate();

        thenNoExceptionOccurred();
        thenDisconnectIsPublishedImmediately(BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicDisconnectSuffix());
    }

    @Test
    public void shouldPublishWithDelayOnPositionLockedEvent() throws KuraException {
        givenPositionLockedEvent();
        givenConfiguredCloudService();
        givenConnected();

        whenHandleEvent();

        thenNoExceptionOccurred();
        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldPublishWithDelayOnModemReadyEvent() throws KuraException {
        givenModemReadyEvent();
        givenConfiguredCloudService();
        givenConnected();

        whenHandleEvent();

        thenNoExceptionOccurred();
        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldPublishWithDelayOnTamperEvent() throws KuraException {
        givenTamperEvent();
        givenConfiguredCloudService();
        givenConnected();

        whenHandleEvent();

        thenNoExceptionOccurred();
        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldPublishImmediatelyOnConnectionEstabilished() throws KuraException {
        givenConfiguredCloudService();

        whenOnConnectionEstabilished();

        thenNoExceptionOccurred();
        thenBirthIsPublishedImmediately(BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldPublishImmediatelyOnDisconnecting() throws KuraException {
        givenConfiguredCloudService();
        givenConnected();

        whenOnDisconnecting();

        thenNoExceptionOccurred();
        thenDisconnectIsPublishedImmediately(BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicDisconnectSuffix());
    }

    @Test
    public void shouldPublishWithDelayWhenRegisterRequestHandler() throws KuraException {
        givenConfiguredCloudService();
        givenConnected();

        whenRegisterRequestHandler();

        thenNoExceptionOccurred();
        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicAppsSuffix());
    }

    @Test
    public void shouldPublishWithDelayWhenUnregisterRequestHandler() throws KuraException {
        givenConfiguredCloudService();
        givenConnected();

        whenUnregisterRequestHandler();

        thenNoExceptionOccurred();
        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicAppsSuffix());
    }

    @Test
    public void shouldPublishBirthOnInstalledEvent() throws KuraException {
        givenDeploymentAdminPackageInstallEvent();
        givenConfiguredCloudService();
        givenConnected();

        whenHandleEvent();

        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldPublishBirthOnUninstalledEvent() throws KuraException {
        givenDeploymentAdminPackageUninstallEvent();
        givenConfiguredCloudService();
        givenConnected();

        whenHandleEvent();

        thenBirthIsPublishedAfter(SEND_DELAY, BIRTH_TOPIC_PREFIX + CloudServiceOptions.getTopicBirthSuffix());
    }

    @Test
    public void shouldNotPublishBirthOnInstalledEventIfNotConnected() throws KuraException {
        givenDeploymentAdminPackageInstallEvent();
        givenConfiguredCloudService();
        givenDisconnected();

        whenHandleEvent();

        thenNoBirthIsPublished();
    }

    @Test
    public void shouldNotPublishBirthOnUninstalledEventIfNotConnected() throws KuraException {
        givenDeploymentAdminPackageUninstallEvent();
        givenConfiguredCloudService();
        givenDisconnected();

        whenHandleEvent();

        thenNoBirthIsPublished();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenConfiguredCloudService() {
        this.cloudService.activate(getMockComponentContext(), getDefaultProperties());
    }

    private void givenConnected() {
        when(this.dataService.isConnected()).thenReturn(true);
    }

    private void givenDisconnected() {
        when(this.dataService.isConnected()).thenReturn(false);
    }

    private void givenPositionLockedEvent() {
        this.event = new Event(PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC, new HashMap<String, Object>());
    }

    private void givenModemReadyEvent() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ModemReadyEvent.FW_VERSION, "1.1.1");
        properties.put(ModemReadyEvent.ICCID, "1234");
        properties.put(ModemReadyEvent.IMEI, "4321");
        properties.put(ModemReadyEvent.IMSI, "6789");
        properties.put(ModemReadyEvent.MODEM_DEVICE, "ppp0");
        properties.put(ModemReadyEvent.RSSI, "9876");
        this.event = new ModemReadyEvent(properties);
    }

    private void givenTamperEvent() {
        this.event = new Event(TamperEvent.TAMPER_EVENT_TOPIC, new HashMap<String, Object>());
    }

    private void givenDeploymentAdminPackageInstallEvent() {
        this.event = new Event(CloudServiceImpl.EVENT_TOPIC_DEPLOYMENT_ADMIN_INSTALL, new HashMap<String, Object>());
    }

    private void givenDeploymentAdminPackageUninstallEvent() {
        this.event = new Event(CloudServiceImpl.EVENT_TOPIC_DEPLOYMENT_ADMIN_UNINSTALL, new HashMap<String, Object>());
    }

    /*
     * When
     */

    private void whenActivate() {
        try {
            givenConfiguredCloudService();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenUpdated() {
        try {
            this.cloudService.updated(getDefaultProperties());
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenDeactivate() {
        try {
            this.cloudService.deactivate(getMockComponentContext());
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenHandleEvent() {
        try {
            this.cloudService.handleEvent(this.event);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenOnConnectionEstabilished() {
        try {
            this.cloudService.onConnectionEstablished();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenOnDisconnecting() {
        try {
            this.cloudService.onDisconnecting();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenRegisterRequestHandler() {
        try {
            RequestHandler handler = mock(RequestHandler.class);
            this.cloudService.registerRequestHandler("example.id", handler);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenUnregisterRequestHandler() {
        try {
            this.cloudService.unregister("example.id");
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    private void thenNoBirthIsPublished() throws KuraStoreException {
        verify(this.dataService, times(0)).publish(anyString(), any(), anyInt(), anyBoolean(), anyInt());
    }

    private void thenBirthIsPublishedAfter(long delayMillis, String expectedTopic) throws KuraException {
        verify(this.dataService, after(delayMillis).never()).publish(eq(expectedTopic), any(), eq(0), eq(false),
                eq(0));
        verify(this.dataService, after(delayMillis + SLACK_DELAY).times(1)).publish(eq(expectedTopic), any(), eq(1),
                eq(false), eq(0));
    }

    private void thenBirthIsPublishedImmediately(String expectedTopic) throws KuraException {
        verify(this.dataService, timeout(SLACK_DELAY).times(1)).publish(eq(expectedTopic), any(), eq(1), eq(false),
                eq(0));
    }

    private void thenDisconnectIsPublishedImmediately(String expectedTopic) throws KuraException {
        verify(this.dataService, timeout(SLACK_DELAY).times(1)).publish(eq(expectedTopic), any(), eq(0), eq(false),
                eq(0));
    }

    /*
     * Utilities
     */

    @Before
    public void setup() {
        SystemService systemService = mock(SystemService.class);
        when(systemService.getDeviceName()).thenReturn("test-device");
        when(systemService.getHostname()).thenReturn("localhost");
        when(systemService.getModelName()).thenReturn("test-model");
        when(systemService.getModelId()).thenReturn("test-model-id");
        when(systemService.getPartNumber()).thenReturn("test-part-number");
        when(systemService.getSerialNumber()).thenReturn("test-sn");
        when(systemService.getFirmwareVersion()).thenReturn("test-fm-vers");
        when(systemService.getCpuVersion()).thenReturn("test-cpu-vers");
        when(systemService.getBiosVersion()).thenReturn("test-bios-vers");
        when(systemService.getOsName()).thenReturn("test-os");
        when(systemService.getOsVersion()).thenReturn("test-os-vers");
        when(systemService.getJavaVmName()).thenReturn("test-jvm");
        when(systemService.getJavaVmVersion()).thenReturn("test-jvm-vers");
        when(systemService.getJavaVmInfo()).thenReturn("test-jvm-info");
        when(systemService.getJavaVendor()).thenReturn("test-java-vendor");
        when(systemService.getJavaVersion()).thenReturn("17");
        when(systemService.getKuraVersion()).thenReturn("develop");
        when(systemService.getNumberOfProcessors()).thenReturn(4);
        when(systemService.getTotalMemory()).thenReturn(8000L);
        when(systemService.getOsArch()).thenReturn("x86");
        when(systemService.getOsgiFwName()).thenReturn("test-osgi-fm");
        when(systemService.getOsgiFwVersion()).thenReturn("test-osgi-vers");

        SystemAdminService systemAdminService = mock(SystemAdminService.class);
        when(systemAdminService.getUptime()).thenReturn("1 day");

        Marshaller marshaller = new Marshaller() {

            @Override
            public String marshal(Object object) throws KuraException {
                if (object instanceof KuraPayload) {
                    return "this is a birth message";
                }
                return null;
            }

        };

        EventAdmin eventAdmin = mock(EventAdmin.class);
        doNothing().when(eventAdmin).postEvent(any());

        this.cloudService.setDataService(this.dataService);
        this.cloudService.setSystemService(systemService);
        this.cloudService.setSystemAdminService(systemAdminService);
        this.cloudService.setJsonMarshaller(marshaller);
        this.cloudService.setEventAdmin(eventAdmin);
    }

    private Map<String, Object> getDefaultProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationService.KURA_SERVICE_PID, "test.pid");
        properties.put("payload.encoding", "simple-json");
        properties.put("topic.control-prefix", "$EDC");
        properties.put("republish.mqtt.birth.cert.on.gps.lock", true);
        properties.put("republish.mqtt.birth.cert.on.modem.detect", true);
        properties.put("republish.mqtt.birth.cert.on.tamper.event", true);

        return properties;
    }

    private ComponentContext getMockComponentContext() {
        Dictionary<String, Object> componentContextProperties = new Hashtable<>();
        componentContextProperties.put(ConfigurationService.KURA_SERVICE_PID, "test.pid");

        ServiceRegistration registration = mock(ServiceRegistration.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.registerService(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any(Dictionary.class))).thenReturn(registration);

        ComponentContext componentContext = mock(ComponentContext.class);
        when(componentContext.getProperties()).thenReturn(componentContextProperties);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        return componentContext;
    }

}
