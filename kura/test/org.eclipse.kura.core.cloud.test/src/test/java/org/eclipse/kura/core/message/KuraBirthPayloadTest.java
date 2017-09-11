/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.kura.core.message.KuraBirthPayload.KuraBirthPayloadBuilder;
import org.eclipse.kura.message.KuraPosition;
import org.junit.Test;


public class KuraBirthPayloadTest {

    @Test
    public void testBuild() {
        // tests build and toString

        String encoding = "UTF-8";
        String pEncoding = "pUTF-8";
        String framework = "Kura";
        String frameworkVersion = "3.1.0";
        String identifiers = "kura-3.1.0";
        String processors = "4";
        String biosVersion = "1.1";
        String connectionInterface = "eth0";
        String connectionIp = "10.10.10.15";
        String displayName = "displayname";
        String firmwareVersion = "0.99";
        String jvmName = "Java HotSpot(TM) Client VM";
        String jvmProfile = "C2";
        String jvmVersion = "25.65-b01";
        String modelId = "Raspberry-Pi";
        String modelName = "Raspberry-Pi";
        String modemIccid = "iccid";
        String imei = "imei";
        String imsi = "imsi";
        String rssi = "rssi";
        String os = "Linux";
        String arch = "arm";
        String osgiFramework = "Eclipse";
        String osgiFrameworkVersion = "1.8.0";
        String osVersion = "4.1.8";
        String partNumber = "pn";
        double lat = 46.0;
        double lon = 14.0;
        String sn = "SN:123456";
        String memory = "506880 kB";
        String uptime = "0 days 1:2:34 hms";

        KuraBirthPayloadBuilder builder = new KuraBirthPayloadBuilder();
        builder.withAcceptEncoding(encoding);
        builder.withApplicationFramework(framework);
        builder.withApplicationFrameworkVersion(frameworkVersion);
        builder.withApplicationIdentifiers(identifiers);
        builder.withAvailableProcessors(processors);
        builder.withBiosVersion(biosVersion);
        builder.withConnectionInterface(connectionInterface);
        builder.withConnectionIp(connectionIp);
        builder.withDisplayName(displayName);
        builder.withFirmwareVersion(firmwareVersion);
        builder.withJvmName(jvmName);
        builder.withJvmProfile(jvmProfile);
        builder.withJvmVersion(jvmVersion);
        builder.withModelId(modelId);
        builder.withModelName(modelName);
        builder.withModemIccid(modemIccid);
        builder.withModemImei(imei);
        builder.withModemImsi(imsi);
        builder.withModemRssi(rssi);
        builder.withOs(os);
        builder.withOsArch(arch);
        builder.withOsgiFramework(osgiFramework);
        builder.withOsgiFrameworkVersion(osgiFrameworkVersion);
        builder.withOsVersion(osVersion);
        builder.withPartNumber(partNumber);
        builder.withPayloadEncoding(pEncoding);
        KuraPosition position = new KuraPosition();
        position.setLatitude(lat);
        position.setLongitude(lon);
        builder.withPosition(position);
        builder.withSerialNumber(sn);
        builder.withTotalMemory(memory);
        builder.withUptime(uptime);

        KuraBirthPayload payload = builder.build();

        double eps = 0.000001;
        assertEquals(encoding, payload.getAcceptEncoding());
        assertEquals(framework, payload.getApplicationFramework());
        assertEquals(frameworkVersion, payload.getApplicationFrameworkVersion());
        assertEquals(identifiers, payload.getApplicationIdentifiers());
        assertEquals(processors, payload.getAvailableProcessors());
        assertEquals(biosVersion, payload.getBiosVersion());
        assertEquals(connectionInterface, payload.getConnectionInterface());
        assertEquals(connectionIp, payload.getConnectionIp());
        assertEquals(displayName, payload.getDisplayName());
        assertEquals(firmwareVersion, payload.getFirmwareVersion());
        assertEquals(jvmName, payload.getJvmName());
        assertEquals(jvmProfile, payload.getJvmProfile());
        assertEquals(jvmVersion, payload.getJvmVersion());
        assertEquals(modelId, payload.getModelId());
        assertEquals(modelName, payload.getModelName());
        assertEquals(modemIccid, payload.getModemIccid());
        assertEquals(imei, payload.getModemImei());
        assertEquals(imsi, payload.getModemImsi());
        assertEquals(rssi, payload.getModemRssi());
        assertEquals(os, payload.getOs());
        assertEquals(arch, payload.getOsArch());
        assertEquals(osgiFramework, payload.getOsgiFramework());
        assertEquals(osgiFrameworkVersion, payload.getOsgiFrameworkVersion());
        assertEquals(osVersion, payload.getOsVersion());
        assertEquals(partNumber, payload.getPartNumber());
        assertEquals(pEncoding, payload.getPayloadEncoding());
        assertEquals(lat, payload.getPosition().getLatitude(), eps);
        assertEquals(lon, payload.getPosition().getLongitude(), eps);
        assertEquals(sn, payload.getSerialNumber());
        assertEquals(memory, payload.getTotalMemory());
        assertEquals(uptime, payload.getUptime());

        String str = payload.toString();

        assertTrue(str.contains("=" + encoding));
        assertTrue(str.contains("=" + pEncoding));
        assertTrue(str.contains("=" + framework));
        assertTrue(str.contains("=" + frameworkVersion));
        assertTrue(str.contains("=" + identifiers));
        assertTrue(str.contains("=" + processors));
        assertTrue(str.contains("=" + biosVersion));
        assertTrue(str.contains("=" + connectionInterface));
        assertTrue(str.contains("=" + connectionIp));
        assertTrue(str.contains("=" + displayName));
        assertTrue(str.contains("=" + firmwareVersion));
        assertTrue(str.contains("=" + jvmName));
        assertTrue(str.contains("=" + jvmProfile));
        assertTrue(str.contains("=" + jvmVersion));
        assertTrue(str.contains("=" + modelId));
        assertTrue(str.contains("=" + modelName));
        assertFalse(str.contains(modemIccid));
        assertFalse(str.contains(imei));
        assertFalse(str.contains(imsi));
        assertFalse(str.contains(rssi));
        assertTrue(str.contains("=" + os));
        assertTrue(str.contains("=" + arch));
        assertTrue(str.contains("=" + osgiFramework));
        assertTrue(str.contains("=" + osgiFrameworkVersion));
        assertTrue(str.contains("=" + osVersion));
        assertTrue(str.contains("=" + partNumber));
        assertTrue(str.contains("=" + sn));
        assertTrue(str.contains("=" + memory));
        assertTrue(str.contains("=" + uptime));
    }

}
