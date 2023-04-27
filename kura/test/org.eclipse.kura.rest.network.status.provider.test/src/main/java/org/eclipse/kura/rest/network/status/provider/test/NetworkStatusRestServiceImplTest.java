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
package org.eclipse.kura.rest.network.status.provider.test;

import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus;
import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.eclipse.kura.net.status.modem.Bearer;
import org.eclipse.kura.net.status.modem.BearerIpType;
import org.eclipse.kura.net.status.modem.ESimStatus;
import org.eclipse.kura.net.status.modem.ModemBand;
import org.eclipse.kura.net.status.modem.ModemCapability;
import org.eclipse.kura.net.status.modem.ModemConnectionStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemMode;
import org.eclipse.kura.net.status.modem.ModemModePair;
import org.eclipse.kura.net.status.modem.ModemPortType;
import org.eclipse.kura.net.status.modem.ModemPowerState;
import org.eclipse.kura.net.status.modem.RegistrationStatus;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.modem.SimType;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiChannel;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

@RunWith(Parameterized.class)
public class NetworkStatusRestServiceImplTest extends AbstractRequestHandlerTest {

    @Test
    public void shouldRejectRequestWithMissingInterfeceIdField() {
        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH, "{}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestWithNullInterfeceIdField() {
        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH, "{\"interfaceIds\":null}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestWithEmptyInterfeceIdField() {
        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH, "{\"interfaceIds\":[]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestWithNullInterfaceId() {
        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"dfoo\",null]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestWithEmptyInterfaceId() {
        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"dfoo\",\"\"]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestWithInterfaceIdConteiningOnlySpaces() {
        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"dfoo\",\"  \"]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRportGeneralExceptionGettingInterfaceList() {
        givenExceptionThrownByNetworkStatusServiceMethods(new IllegalStateException("exception message"));

        whenRequestIsPerformed(new MethodSpec("GET"), INTERFACE_IDS_PATH);

        thenResponseCodeIs(500);
        thenResponseBodyEqualsJson("{\"message\":\"exception message\"}");
    }

    @Test
    public void shouldRportGeneralExceptionGettingAllInterfacesStatus() {
        givenExceptionThrownByNetworkStatusServiceMethods(new IllegalStateException("exception message"));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenResponseCodeIs(500);
        thenResponseBodyEqualsJson("{\"message\":\"exception message\"}");
    }

    @Test
    public void shouldReturnNonEmptyInterfaceList() {
        givenInterfaceIds("foo", "bar");

        whenRequestIsPerformed(new MethodSpec("GET"), INTERFACE_IDS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaceIds\": [\"foo\",\"bar\"]}");
    }

    @Test
    public void shouldReturnEmptyInterfaceListIfNoInterfacesArePresent() {
        givenInterfaceIds();

        whenRequestIsPerformed(new MethodSpec("GET"), INTERFACE_IDS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaceIds\": []}");
    }

    @Test
    public void shouldReportExceptionMessageGettingAllInterfaces() {
        givenExceptionRetrievingInterfaceStatus("foo", new IllegalArgumentException("exception message"));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"interfaces\":[],\"failures\":[{\"interfaceId\":\"foo\",\"reason\":\"exception message\"}]}");
    }

    @Test
    public void shouldReportExceptionMessageByInterfaceId() {
        givenExceptionRetrievingInterfaceStatus("foo", new IllegalArgumentException("exception message"));

        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"interfaces\":[],\"failures\":[{\"interfaceId\":\"foo\",\"reason\":\"exception message\"}]}");
    }

    @Test
    public void shouldReportDefaultExceptionMessageGettingAllInterfaces() {
        givenExceptionRetrievingInterfaceStatus("foo", new IllegalArgumentException());

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"interfaces\":[],\"failures\":[{\"interfaceId\":\"foo\",\"reason\":\"Unknown error\"}]}");
    }

    @Test
    public void shouldReportDefaultExceptionMessageByInterfaceId() {
        givenExceptionRetrievingInterfaceStatus("foo", new IllegalArgumentException());

        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"interfaces\":[],\"failures\":[{\"interfaceId\":\"foo\",\"reason\":\"Unknown error\"}]}");
    }

    @Test
    public void shouldEncodeLoopbackInterfaceStatusWithDefaults() {
        givenNetworkStatus(LoopbackInterfaceStatus.builder());

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\": \"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeLoopbackInterfaceStatusWithDefaultsById() {
        givenNetworkStatus(LoopbackInterfaceStatus.builder().withInterfaceId("lo0"));

        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"lo0\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\": \"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldNotReturnMissingInterfaces() {
        givenNetworkStatus(LoopbackInterfaceStatus.builder().withInterfaceId("lo0"));

        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"lo0\",\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\": \"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}],\"failures\":[{" //
                + "\"interfaceId\":\"foo\"," //
                + "\"reason\":\"Not found.\""//
                + "}]}");
    }

    @Test
    public void shouldReturnEmptyListIfNoInterfacesMatch() {
        givenNetworkStatus(LoopbackInterfaceStatus.builder().withInterfaceId("lo0"));

        whenRequestIsPerformed(new MethodSpec("POST"), NETWORK_STATUS_BY_INTERFACE_ID_PATH,
                "{\"interfaceIds\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"interfaces\":[],\"failures\":[{\"interfaceId\":\"foo\",\"reason\":\"Not found.\"}]}");
    }

    @Test
    public void shouldEncodeLoopbackInterfaceStatusWithCustomParams() {
        givenNetworkStatus(LoopbackInterfaceStatus.builder() //
                .withInterfaceId("lo0") //
                .withInterfaceName("lo1") //
                .withHardwareAddress(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, (byte) 0xff }) //
                .withDriver("fooDriver") //
                .withDriverVersion("fooDriverVersion") //
                .withFirmwareVersion("fooFirmwareVersion") //
                .withVirtual(true) //
                .withState(NetworkInterfaceState.PREPARE) //
                .withAutoConnect(false) //
                .withMtu(1500) //
        );

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"lo1\"," //
                + "\"hardwareAddress\":\"01:02:03:04:05:FF\"," //
                + "\"type\":\"LOOPBACK\"," //
                + "\"driver\":\"fooDriver\"," //
                + "\"driverVersion\":\"fooDriverVersion\"," //
                + "\"firmwareVersion\":\"fooFirmwareVersion\"," //
                + "\"virtual\":true," //
                + "\"state\":\"PREPARE\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":1500" //
                + "}]," + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeFilledIpV4Addresses() throws UnknownHostException {

        givenLoopbackInterfaceWithFilledIP4Address("lo0");

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"" //
                + ",\"driver\":\"N/A\"" //
                + ",\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0," //
                + "\"interfaceIp4Addresses\":{" //
                + "\"addresses\":[{" //
                + "\"address\":\"1.2.3.4\",\"prefix\":16}," //
                + "{\"address\":\"5.6.7.255\",\"prefix\":32}]" //
                + ",\"gateway\":\"5.6.7.255\"," //
                + "\"dnsServerAddresses\":[\"1.2.3.4\",\"5.6.7.255\"" //
                + "]}}]," //
                + "\"failures\":[]" //
                + "}");

    }

    @Test
    public void shouldEncodeUnFilledIpV4Addresses() throws UnknownHostException {

        givenLoopbackInterfaceWithUnFilledIP4Address("lo0");

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0," //
                + "\"interfaceIp4Addresses\":{" //
                + "\"addresses\":[]" //
                + ",\"dnsServerAddresses\":[]" //
                + "}}]," //
                + "\"failures\":[]" //
                + "}");

    }

    @Test
    public void shouldEncodeFilledIpV6Addresses() throws UnknownHostException {

        givenLoopbackInterfaceWithFilledIP6Address("lo0");

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"" //
                + ",\"driver\":\"N/A\"" //
                + ",\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0," //
                + "\"interfaceIp6Addresses\":{" //
                + "\"addresses\":[" //
                + "{\"address\":\"102:304:500:0:0:0:0:0\",\"prefix\":16}," //
                + "{\"address\":\"506:7ff:500:0:0:0:0:0\",\"prefix\":32}]," //
                + "\"gateway\":\"506:7ff:500:0:0:0:0:0\"," //
                + "\"dnsServerAddresses\":[\"102:304:500:0:0:0:0:0\",\"506:7ff:500:0:0:0:0:0\"" //
                + "]}}]," //
                + "\"failures\":[]" //
                + "}");

    }

    @Test
    public void shouldEncodeUnFilledIpV6Addresses() throws UnknownHostException {

        givenLoopbackInterfaceWithUnFilledIP6Address("lo0");

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"id\":\"lo0\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"LOOPBACK\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0," //
                + "\"interfaceIp6Addresses\":{" //
                + "\"addresses\":[]" //
                + ",\"dnsServerAddresses\":[]" //
                + "}}]," //
                + "\"failures\":[]" //
                + "}");

    }

    @Test
    public void shouldEncodeEthernetInterfaceStatusWithDefaults() {
        givenNetworkStatus(EthernetInterfaceStatus.builder());

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"linkUp\":false," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"ETHERNET\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeEthernetInterfaceStatusWithCustomParams() {
        givenNetworkStatus(EthernetInterfaceStatus.builder().withIsLinkUp(true));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"linkUp\":true," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"ETHERNET\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeWifiInterfaceStatusWithDefaults() {
        givenNetworkStatus(WifiInterfaceStatus.builder());

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"capabilities\":[\"NONE\"]," //
                + "\"channels\":[]," //
                + "\"countryCode\":\"00\"," //
                + "\"mode\":\"UNKNOWN\"," //
                + "\"availableWifiAccessPoints\":[]," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"WIFI\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeWifiInterfaceStatusWithCustomParams() {
        givenNetworkStatus(
                WifiInterfaceStatus.builder().withCapabilities(EnumSet.of(WifiCapability.ADHOC, WifiCapability.AP)) //
                        .withCountryCode("foo") //
                        .withMode(WifiMode.MASTER) //
        );

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"capabilities\":[\"AP\",\"ADHOC\"]," //
                + "\"channels\":[]," //
                + "\"countryCode\":\"foo\"," //
                + "\"mode\":\"MASTER\"," //
                + "\"availableWifiAccessPoints\":[]," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"WIFI\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeWifiChannelWithEmptyFields() {
        givenNetworkStatus(WifiInterfaceStatus.builder()
                .withWifiChannels(Collections.singletonList(WifiChannel.builder(1, 2).build())));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"capabilities\":[\"NONE\"]," //
                + "\"channels\":" //
                + "[{\"channel\":1,\"frequency\":2}]," //
                + "\"countryCode\":\"00\"," //
                + "\"mode\":\"UNKNOWN\"," //
                + "\"availableWifiAccessPoints\":[]," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"WIFI\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeWifiChannelWithFilledFields() {
        givenNetworkStatus(WifiInterfaceStatus.builder()
                .withWifiChannels(Collections.singletonList(WifiChannel.builder(1, 2) //
                        .withDisabled(true) //
                        .withAttenuation(12.9f) //
                        .withNoInitiatingRadiation(true) //
                        .withRadarDetection(true) //
                        .build())));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"capabilities\":[\"NONE\"]," //
                + "\"channels\":" //
                + "[{\"channel\":1," //
                + "\"frequency\":2," //
                + "\"disabled\":true," //
                + "\"attenuation\":12.9," //
                + "\"noInitiatingRadiation\":true," //
                + "\"radarDetection\":true}]," //
                + "\"countryCode\":\"00\"," //
                + "\"mode\":\"UNKNOWN\"," //
                + "\"availableWifiAccessPoints\":[]," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"WIFI\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeActiveWifiAccessPoint() {
        givenNetworkStatus(WifiInterfaceStatus.builder().withActiveWifiAccessPoint(Optional.of(WifiAccessPoint.builder() //
                .withSsid("foo") //
                .withHardwareAddress(new byte[] { 1, 2, 3, 4, 5, (byte) 0xff }) //
                .withChannel(WifiChannel.builder(1, 2).build()) //
                .withMaxBitrate(123) //
                .withSignalQuality(12) //
                .withSignalStrength(-94) //
                .withWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.KEY_MGMT_SAE)) //
                .withRsnSecurity(EnumSet.of(WifiSecurity.KEY_MGMT_EAP_SUITE_B_192)) //
                .build())) //
        );

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"capabilities\":[\"NONE\"]," //
                + "\"channels\":[]," //
                + "\"countryCode\":\"00\"," //
                + "\"mode\":\"UNKNOWN\"," //
                + "\"activeWifiAccessPoint\":{" //
                + "\"ssid\":\"foo\"," //
                + "\"hardwareAddress\":\"01:02:03:04:05:FF\"," //
                + "\"channel\":{\"channel\":1,\"frequency\":2}," //
                + "\"maxBitrate\":123," //
                + "\"signalQuality\":12," //
                + "\"signalStrength\":-94," //
                + "\"wpaSecurity\":[\"GROUP_CCMP\",\"KEY_MGMT_SAE\"]," //
                + "\"rsnSecurity\":[\"KEY_MGMT_EAP_SUITE_B_192\"]}," //
                + "\"availableWifiAccessPoints\":[]," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"WIFI\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0}" //
                + "]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeWifiAccessPointInAvailableAccessPointList() {
        givenNetworkStatus(WifiInterfaceStatus.builder() //
                .withAvailableWifiAccessPoints(Collections.singletonList(WifiAccessPoint.builder() //
                        .withSsid("foo") //
                        .withHardwareAddress(new byte[] { 1, 2, 3, 4, 5, (byte) 0xff }) //
                        .withChannel(WifiChannel.builder(1, 2).build()) //
                        .withMaxBitrate(123) //
                        .withSignalQuality(12) //
                        .withSignalStrength(-94) //
                        .withWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.KEY_MGMT_SAE)) //
                        .withRsnSecurity(EnumSet.of(WifiSecurity.KEY_MGMT_EAP_SUITE_B_192)) //
                        .build())));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"capabilities\":[\"NONE\"]," //
                + "\"channels\":[]," //
                + "\"countryCode\":\"00\"," //
                + "\"mode\":\"UNKNOWN\"," //
                + "\"availableWifiAccessPoints\":[{" //
                + "\"ssid\":\"foo\"," //
                + "\"hardwareAddress\":\"01:02:03:04:05:FF\"," //
                + "\"channel\":{\"channel\":1,\"frequency\":2}," //
                + "\"maxBitrate\":123," //
                + "\"signalQuality\":12," //
                + "\"signalStrength\":-94," //
                + "\"wpaSecurity\":[\"GROUP_CCMP\",\"KEY_MGMT_SAE\"]," //
                + "\"rsnSecurity\":[\"KEY_MGMT_EAP_SUITE_B_192\"]}]," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"WIFI\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeModemInterfaceStatusWithDefaults() {
        givenNetworkStatus(ModemInterfaceStatus.builder());

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"model\":\"N/A\"," //
                + "\"manufacturer\":\"N/A\"," //
                + "\"serialNumber\":\"N/A\"," //
                + "\"softwareRevision\":\"N/A\"," //
                + "\"hardwareRevision\":\"N/A\"," //
                + "\"primaryPort\":\"N/A\"," //
                + "\"ports\":{}," //
                + "\"supportedModemCapabilities\":[\"NONE\"]," //
                + "\"currentModemCapabilities\":[\"NONE\"]," //
                + "\"powerState\":\"UNKNOWN\"," //
                + "\"supportedModes\":[]," //
                + "\"currentModes\":{\"modes\":[],\"preferredMode\":\"NONE\"}," //
                + "\"supportedBands\":[\"UNKNOWN\"]," //
                + "\"currentBands\":[\"UNKNOWN\"]," //
                + "\"gpsSupported\":false," //
                + "\"availableSims\":[]," //
                + "\"simLocked\":false," //
                + "\"bearers\":[]," //
                + "\"connectionType\":\"DirectIP\"," //
                + "\"connectionStatus\":\"UNKNOWN\"," //
                + "\"accessTechnologies\":[\"UNKNOWN\"]," //
                + "\"signalQuality\":0," //
                + "\"signalStrength\":-113," //
                + "\"registrationStatus\":\"UNKNOWN\"," //
                + "\"operatorName\":\"N/A\"," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"MODEM\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeModemInterfaceStatusWithCustomParams() {
        givenNetworkStatus(ModemInterfaceStatus.builder() //
                .withModel("foo") //
                .withManufacturer("bar") //
                .withSerialNumber("sernum") //
                .withSoftwareRevision("rev") //
                .withHardwareRevision("hwrev") //
                .withPrimaryPort("port") //
                .withPorts(Collections.singletonMap("foo", ModemPortType.AUDIO)) //
                .withSupportedModemCapabilities(EnumSet.of(ModemCapability.EVDO, ModemCapability.GSM_UMTS)) //
                .withCurrentModemCapabilities(EnumSet.of(ModemCapability.IRIDIUM, ModemCapability.LTE)) //
                .withPowerState(ModemPowerState.OFF) //
                .withSupportedModes(Collections.singleton(
                        new ModemModePair(EnumSet.of(ModemMode.MODE_3G, ModemMode.MODE_4G), ModemMode.MODE_3G))) //
                .withCurrentModes(new ModemModePair(EnumSet.of(ModemMode.MODE_3G), ModemMode.MODE_3G)) //
                .withSupportedBands(EnumSet.of(ModemBand.CDMA_BC1)) //
                .withCurrentBands(EnumSet.of(ModemBand.UTRAN_9, ModemBand.CDMA_BC0)) //
                .withGpsSupported(true) //
                .withSimLocked(true) //
                .withConnectionType(ModemConnectionType.DirectIP) //
                .withConnectionStatus(ModemConnectionStatus.DISCONNECTING) //
                .withAccessTechnologies(EnumSet.of(AccessTechnology.EVDO0, AccessTechnology.GPRS)) //
                .withSignalQuality(12) //
                .withSignalStrength(-94) //
                .withRegistrationStatus(RegistrationStatus.ROAMING) //
                .withOperatorName("oper") //
        );

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"model\":\"foo\"," //
                + "\"manufacturer\":\"bar\"," //
                + "\"serialNumber\":\"sernum\"," //
                + "\"softwareRevision\":\"rev\"," //
                + "\"hardwareRevision\":\"hwrev\"," //
                + "\"primaryPort\":\"port\"," //
                + "\"ports\":{\"foo\":\"AUDIO\"}," //
                + "\"supportedModemCapabilities\":[\"EVDO\",\"GSM_UMTS\"]," //
                + "\"currentModemCapabilities\":[\"LTE\",\"IRIDIUM\"]," //
                + "\"powerState\":\"OFF\"," //
                + "\"supportedModes\":[" //
                + "{\"modes\":[\"MODE_3G\",\"MODE_4G\"]," //
                + "\"preferredMode\":\"MODE_3G\"}]," //
                + "\"currentModes\":{\"modes\":[\"MODE_3G\"],\"preferredMode\":\"MODE_3G\"}," //
                + "\"supportedBands\":[\"CDMA_BC1\"]," //
                + "\"currentBands\":[\"UTRAN_9\",\"CDMA_BC0\"]," //
                + "\"gpsSupported\":true,\"availableSims\":[]," //
                + "\"simLocked\":true," //
                + "\"bearers\":[]," //
                + "\"connectionType\":\"DirectIP\"," //
                + "\"connectionStatus\":\"DISCONNECTING\"," //
                + "\"accessTechnologies\":[\"GPRS\",\"EVDO0\"]," //
                + "\"signalQuality\":12," //
                + "\"signalStrength\":-94," //
                + "\"registrationStatus\":\"ROAMING\"," //
                + "\"operatorName\":\"oper\"," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"MODEM\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeBearer() {
        givenNetworkStatus(ModemInterfaceStatus.builder()
                .withBearers(Arrays.asList(new Bearer("foo", true, "bar", Collections.emptySet(), 1000, 1000),
                        new Bearer("bar", true, "baz", EnumSet.of(BearerIpType.ANY, BearerIpType.IPV4V6), 100, 1010))));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"model\":\"N/A\"," //
                + "\"manufacturer\":\"N/A\"," //
                + "\"serialNumber\":\"N/A\"," //
                + "\"softwareRevision\":\"N/A\"," //
                + "\"hardwareRevision\":\"N/A\"," //
                + "\"primaryPort\":\"N/A\"," //
                + "\"ports\":{}," //
                + "\"supportedModemCapabilities\":[\"NONE\"]," //
                + "\"currentModemCapabilities\":[\"NONE\"]," //
                + "\"powerState\":\"UNKNOWN\"," //
                + "\"supportedModes\":[]," //
                + "\"currentModes\":{\"modes\":[],\"preferredMode\":\"NONE\"}," //
                + "\"supportedBands\":[\"UNKNOWN\"]," //
                + "\"currentBands\":[\"UNKNOWN\"]," //
                + "\"gpsSupported\":false," //
                + "\"availableSims\":[]," //
                + "\"simLocked\":false," //
                + "\"bearers\":[{" //
                + "\"name\":\"foo\"," //
                + "\"connected\":true," //
                + "\"apn\":\"bar\"," //
                + "\"ipTypes\":[]," //
                + "\"bytesTransmitted\":1000," //
                + "\"bytesReceived\":1000},{" //
                + "\"name\":\"bar\"," //
                + "\"connected\":true," //
                + "\"apn\":\"baz\"," //
                + "\"ipTypes\":[\"IPV4V6\",\"ANY\"]," //
                + "\"bytesTransmitted\":100," //
                + "\"bytesReceived\":1010}]," //
                + "\"connectionType\":\"DirectIP\"," //
                + "\"connectionStatus\":\"UNKNOWN\"," //
                + "\"accessTechnologies\":[\"UNKNOWN\"]," //
                + "\"signalQuality\":0," //
                + "\"signalStrength\":-113," //
                + "\"registrationStatus\":\"UNKNOWN\"," //
                + "\"operatorName\":\"N/A\"," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"MODEM\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    @Test
    public void shouldEncodeSim() {
        givenNetworkStatus(ModemInterfaceStatus.builder().withAvailableSims(Arrays.asList( //
                Sim.builder() //
                        .withActive(true) //
                        .withPrimary(true) //
                        .withIccid("abc") //
                        .withImsi("imsi") //
                        .withEid("sed") //
                        .withOperatorName("op") //
                        .withSimType(SimType.PHYSICAL) //
                        .withESimStatus(ESimStatus.UNKNOWN).build(), //
                Sim.builder() //
                        .withActive(false) //
                        .withPrimary(false) //
                        .withIccid("ac") //
                        .withImsi("isi") //
                        .withEid("se") //
                        .withOperatorName("opp") //
                        .withSimType(SimType.ESIM) //
                        .withESimStatus(ESimStatus.WITH_PROFILES) //
                        .build())));

        whenRequestIsPerformed(new MethodSpec("GET"), NETWORK_STATUS_PATH);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"interfaces\":[{" //
                + "\"model\":\"N/A\"," //
                + "\"manufacturer\":\"N/A\"," //
                + "\"serialNumber\":\"N/A\"," //
                + "\"softwareRevision\":\"N/A\"," //
                + "\"hardwareRevision\":\"N/A\"," //
                + "\"primaryPort\":\"N/A\"," //
                + "\"ports\":{}," //
                + "\"supportedModemCapabilities\":[\"NONE\"]," //
                + "\"currentModemCapabilities\":[\"NONE\"]," //
                + "\"powerState\":\"UNKNOWN\"," //
                + "\"supportedModes\":[]," //
                + "\"currentModes\":{\"modes\":[],\"preferredMode\":\"NONE\"}," //
                + "\"supportedBands\":[\"UNKNOWN\"]," //
                + "\"currentBands\":[\"UNKNOWN\"]," //
                + "\"gpsSupported\":false," //
                + "\"availableSims\":[{" //
                + "\"active\":true," //
                + "\"primary\":true," //
                + "\"iccid\":\"abc\"," //
                + "\"imsi\":\"imsi\"," //
                + "\"eid\":\"sed\"," //
                + "\"operatorName\":\"op\"," //
                + "\"simType\":\"PHYSICAL\"," //
                + "\"eSimStatus\":\"UNKNOWN\"}," //
                + "{\"active\":false," //
                + "\"primary\":false," //
                + "\"iccid\":\"ac\"," //
                + "\"imsi\":\"isi\"," //
                + "\"eid\":\"se\"," //
                + "\"operatorName\":\"opp\"," //
                + "\"simType\":\"ESIM\"," //
                + "\"eSimStatus\":\"WITH_PROFILES\"}]," //
                + "\"simLocked\":false," //
                + "\"bearers\":[]," //
                + "\"connectionType\":\"DirectIP\"," //
                + "\"connectionStatus\":\"UNKNOWN\"," //
                + "\"accessTechnologies\":[\"UNKNOWN\"]," //
                + "\"signalQuality\":0," //
                + "\"signalStrength\":-113," //
                + "\"registrationStatus\":\"UNKNOWN\"," //
                + "\"operatorName\":\"N/A\"," //
                + "\"id\":\"N/A\"," //
                + "\"interfaceName\":\"N/A\"," //
                + "\"hardwareAddress\":\"00:00:00:00:00:00\"," //
                + "\"type\":\"MODEM\"," //
                + "\"driver\":\"N/A\"," //
                + "\"driverVersion\":\"N/A\"," //
                + "\"firmwareVersion\":\"N/A\"," //
                + "\"virtual\":false," //
                + "\"state\":\"UNKNOWN\"," //
                + "\"autoConnect\":false," //
                + "\"mtu\":0" //
                + "}]," //
                + "\"failures\":[]" //
                + "}");
    }

    private static final String INTERFACE_IDS_PATH = "/interfaceIds";
    private static final String NETWORK_STATUS_PATH = "/status";
    private static final String NETWORK_STATUS_BY_INTERFACE_ID_PATH = "/status/byInterfaceId";

    private static final NetworkStatusService networkStatusService = Mockito.mock(NetworkStatusService.class);
    private static ServiceRegistration<NetworkStatusService> reg;
    private final Map<String, Result> currentStatus = new LinkedHashMap<>();
    private Optional<Exception> generalFailure = Optional.empty();

    public NetworkStatusRestServiceImplTest(Transport transport)
            throws InterruptedException, ExecutionException, TimeoutException {
        super(transport);

        try {
            Mockito.when(networkStatusService.getNetworkStatus(ArgumentMatchers.anyString())).thenAnswer(i -> {
                if (this.generalFailure.isPresent()) {
                    throw this.generalFailure.get();
                }

                final String id = i.getArgument(0);
                final Result result = this.currentStatus.get(id);

                if (result instanceof Success) {
                    return Optional.of(((Success) result).status);
                } else if (result instanceof Failure) {
                    throw ((Failure) result).exception;
                }

                return Optional.empty();
            });

            Mockito.when(networkStatusService.getInterfaceIds()).thenAnswer(i -> {
                if (this.generalFailure.isPresent()) {
                    throw this.generalFailure.get();
                }

                return new ArrayList<>(this.currentStatus.keySet());
            });
        } catch (final Exception e) {
            fail("Unexpected exception");
        }
    }

    @BeforeClass
    public static void registerNetworkStatusService()
            throws InterruptedException, ExecutionException, TimeoutException {
        reg = FrameworkUtil.getBundle(NetworkStatusRestServiceImplTest.class).getBundleContext()
                .registerService(NetworkStatusService.class, networkStatusService, new Hashtable<>());

        ServiceUtil.trackService("org.eclipse.kura.internal.network.status.provider.NetworkStatusRestServiceImpl",
                Optional.empty()).get(30, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void unregisterNetworkStatusService() {
        reg.unregister();
    }

    @After
    public void cleanup() {
        Mockito.reset(networkStatusService);
    }

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport("networkStatus/v1"), new MqttTransport("NET-STATUS-V1"));
    }

    private void givenInterfaceIds(final String... interfaceIds) {
        try {
            Mockito.when(networkStatusService.getInterfaceIds()).thenReturn(Arrays.asList(interfaceIds));
        } catch (final Exception e) {
            fail("Unexpected exception");
        }
    }

    private void givenNetworkStatus(final NetworkInterfaceStatus.NetworkInterfaceStatusBuilder<?> builder) {
        final NetworkInterfaceStatus status = builder.build();

        this.currentStatus.put(status.getInterfaceId(), new Success(status));
    }

    private void givenExceptionRetrievingInterfaceStatus(final String interfaceId, final Exception exception) {

        this.currentStatus.put(interfaceId, new Failure(exception));
    }

    private void givenLoopbackInterfaceWithFilledIP4Address(final String id) throws UnknownHostException {
        givenNetworkStatus(
                LoopbackInterfaceStatus.builder().withInterfaceId(id)
                        .withInterfaceIp4Addresses(Optional.of(NetworkInterfaceIpAddressStatus.<IP4Address>builder()
                                .withAddresses(Arrays.asList(
                                        new NetworkInterfaceIpAddress<>(ipV4Address(1, 2, 3, 4), (short) 16),
                                        new NetworkInterfaceIpAddress<>(ipV4Address(5, 6, 7, 0xff), (short) 32)))
                                .withGateway(Optional.of(ipV4Address(5, 6, 7, 0xff))).withDnsServerAddresses(
                                        Arrays.asList(ipV4Address(1, 2, 3, 4), ipV4Address(5, 6, 7, 0xff)))
                                .build())));
    }

    private void givenLoopbackInterfaceWithFilledIP6Address(final String id) throws UnknownHostException {
        givenNetworkStatus(LoopbackInterfaceStatus.builder().withInterfaceId(id)
                .withInterfaceIp6Addresses(Optional.of(NetworkInterfaceIpAddressStatus.<IP6Address>builder()
                        .withAddresses(Arrays.asList(
                                new NetworkInterfaceIpAddress<>(ipV6Address(1, 2, 3, 4, 5, 0), (short) 16),
                                new NetworkInterfaceIpAddress<>(ipV6Address(5, 6, 7, 0xff, 5, 0), (short) 32)))
                        .withGateway(Optional.of(ipV6Address(5, 6, 7, 0xff, 5, 0)))
                        .withDnsServerAddresses(
                                Arrays.asList(ipV6Address(1, 2, 3, 4, 5, 0), ipV6Address(5, 6, 7, 0xff, 5, 0)))
                        .build())));
    }

    private void givenLoopbackInterfaceWithUnFilledIP4Address(final String id) throws UnknownHostException {
        givenNetworkStatus(LoopbackInterfaceStatus.builder().withInterfaceId(id)
                .withInterfaceIp4Addresses(Optional.of(NetworkInterfaceIpAddressStatus.<IP4Address>builder().build())));
    }

    private void givenLoopbackInterfaceWithUnFilledIP6Address(final String id) throws UnknownHostException {
        givenNetworkStatus(LoopbackInterfaceStatus.builder().withInterfaceId(id)
                .withInterfaceIp6Addresses(Optional.of(NetworkInterfaceIpAddressStatus.<IP6Address>builder().build())));
    }

    private void givenExceptionThrownByNetworkStatusServiceMethods(final Exception exception) {
        this.generalFailure = Optional.of(exception);
    }

    private IP4Address ipV4Address(final int a, final int b, final int c, final int d) throws UnknownHostException {
        return (IP4Address) IPAddress.getByAddress(new byte[] { (byte) a, (byte) b, (byte) c, (byte) d });
    }

    private IP6Address ipV6Address(final int... bytes) throws UnknownHostException {
        final byte[] address = new byte[16];

        for (int i = 0; i < bytes.length; i++) {
            address[i] = (byte) (bytes[i] & 0xff);
        }

        return (IP6Address) IPAddress.getByAddress(address);
    }

    private interface Result {
    }

    private static class Success implements Result {

        private final NetworkInterfaceStatus status;

        public Success(NetworkInterfaceStatus status) {
            this.status = status;
        }
    }

    private static class Failure implements Result {

        private final Exception exception;

        public Failure(final Exception exception) {
            this.exception = exception;
        }
    }

}
