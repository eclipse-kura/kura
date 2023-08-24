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
package org.eclipse.kura.rest.system.provider.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.ExtendedPropertyGroup;
import org.eclipse.kura.system.SystemService;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class SystemServiceMockDecorator {

    private static final String PROPERTIES_VALUE = "test";
    private static final String PROPERTIES_FALSE_STRING_VALUE = "false";
    private static final String BUNDLE_LOCATION_VALUE = "test.location";
    private static final String EXT_PROPERTIES_VERSION = "1.0.0";

    /**
     * 
     * @param service
     *            the mock SystemService to modify. It adds mock methods that return all the properties specified
     *            in resource {@link PROPERTIES_RESPONSE} and bundles in resource {@link BUNDLE_RESPONSE}.
     */
    public static void addPropertiesMockMethods(SystemService service) {
        List<Bundle> bundles = new ArrayList<>();

        addBundle(bundles, 0, BUNDLE_LOCATION_VALUE, Bundle.ACTIVE, "bundle0", true, 0L, 1, 0, 0, "SNAPSHOT");
        addBundle(bundles, 1, BUNDLE_LOCATION_VALUE, Bundle.INSTALLED, "bundle1", false, 0L, 1, 0, 0, "SNAPSHOT");
        addBundle(bundles, 2, BUNDLE_LOCATION_VALUE, Bundle.RESOLVED, "bundle2", false, 0L, 1, 1, 1, "SNAPSHOT");
        addBundle(bundles, 3, BUNDLE_LOCATION_VALUE, Bundle.STARTING, "bundle3", false, 0L, 2, 0, 1, "");
        addBundle(bundles, 4, BUNDLE_LOCATION_VALUE, Bundle.STOPPING, "bundle4", false, 0L, 0, 1, 0, "alpha");
        addBundle(bundles, 5, BUNDLE_LOCATION_VALUE, Bundle.UNINSTALLED, "bundle5", false, 0L, 0, 1, 1, "beta");

        initBundles(service, bundles);
        initProperties(service);
    }

    /**
     * 
     * @param service
     *            the mock SystemService to modify. It adds mock methods to include all the extended properties
     *            specified in resource {@link EXTENDED_PROPERTIES_RESPONSE}.
     */
    public static void addExtendedPropertiesMockMethods(SystemService service) {
        initExtendedProperties(service);
    }

    /**
     * 
     * @param service
     *            the mock SystemService to modify. Its mock methods are all modified to throw a
     *            {@link RuntimeException}.
     */
    public static void addFailingMockMethods(SystemService service) {
        initExceptions(service);
    }

    private static void addBundle(List<Bundle> bundles, long id, String location, int state, String symbolicName,
            boolean signed, long lastModified, int versionMajor, int versionMinor, int versionMicro, String versionQualifier) {
        Bundle b = mock(Bundle.class);

        Mockito.when(b.getBundleId()).thenReturn(id);
        Mockito.when(b.getLocation()).thenReturn(location);
        Mockito.when(b.getState()).thenReturn(state);
        Mockito.when(b.getSymbolicName()).thenReturn(symbolicName);

        @SuppressWarnings("unchecked")
        Map<X509Certificate, List<X509Certificate>> certs = mock(Map.class);
        Mockito.when(certs.isEmpty()).thenReturn(!signed);
        Mockito.when(b.getSignerCertificates(Bundle.SIGNERS_ALL)).thenReturn(certs);

        Mockito.when(b.getLastModified()).thenReturn(lastModified);

        Version version = mock(Version.class);
        Mockito.when(version.getMajor()).thenReturn(versionMajor);
        Mockito.when(version.getMinor()).thenReturn(versionMinor);
        Mockito.when(version.getMicro()).thenReturn(versionMicro);
        Mockito.when(version.getQualifier()).thenReturn(versionQualifier);

        Mockito.when(b.getVersion()).thenReturn(version);

        bundles.add(b);
    }

    private static void initBundles(SystemService service, List<Bundle> bundles) {
        Mockito.when(service.getBundles()).thenReturn(getBundleArrayFrom(bundles));
    }

    private static Bundle[] getBundleArrayFrom(List<Bundle> bundleList) {
        Bundle[] bundles = new Bundle[bundleList.size()];
        for (int i = 0; i < bundleList.size(); i++) {
            bundles[i] = bundleList.get(i);
        }
        return bundles;
    }

    private static void initProperties(SystemService service) {
        when(service.getBiosVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getCommandUser()).thenReturn(PROPERTIES_VALUE);
        when(service.getCpuVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getDeviceName()).thenReturn(PROPERTIES_VALUE);
        when(service.getFileSeparator()).thenReturn(PROPERTIES_VALUE);
        when(service.getFirmwareVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getHostname()).thenReturn(PROPERTIES_VALUE);
        when(service.getJavaHome()).thenReturn(PROPERTIES_VALUE);
        when(service.getJavaVendor()).thenReturn(PROPERTIES_VALUE);
        when(service.getJavaVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getJavaVmInfo()).thenReturn(PROPERTIES_VALUE);
        when(service.getJavaVmName()).thenReturn(PROPERTIES_VALUE);
        when(service.getJavaVmVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraDataDirectory()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraFrameworkConfigDirectory()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraHome()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraMarketplaceCompatibilityVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraSnapshotsDirectory()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraStyleDirectory()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraTemporaryConfigDirectory()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraUserConfigDirectory()).thenReturn(PROPERTIES_VALUE);
        when(service.getKuraVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getModelId()).thenReturn(PROPERTIES_VALUE);
        when(service.getModelName()).thenReturn(PROPERTIES_VALUE);
        when(service.getNetVirtualDevicesConfig()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsArch()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsDistro()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsDistroVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsgiFwName()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsgiFwVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsName()).thenReturn(PROPERTIES_VALUE);
        when(service.getOsVersion()).thenReturn(PROPERTIES_VALUE);
        when(service.getPartNumber()).thenReturn(PROPERTIES_VALUE);
        when(service.getPlatform()).thenReturn(PROPERTIES_VALUE);
        when(service.getPrimaryMacAddress()).thenReturn(PROPERTIES_VALUE);
        when(service.getPrimaryNetworkInterfaceName()).thenReturn(PROPERTIES_VALUE);
        when(service.getSerialNumber()).thenReturn(PROPERTIES_VALUE);
        when(service.getTotalMemory()).thenReturn(0L);
        when(service.getFreeMemory()).thenReturn(0L);
        when(service.getKuraSnapshotsCount()).thenReturn(0);
        when(service.getKuraWifiTopChannel()).thenReturn(0);
        when(service.getNumberOfProcessors()).thenReturn(0);
        when(service.getFileCommandZipMaxUploadNumber()).thenReturn(0);
        when(service.getFileCommandZipMaxUploadSize()).thenReturn(0);
        when(service.isLegacyBluetoothBeaconScan()).thenReturn(false);
        when(service.isLegacyPPPLoggingEnabled()).thenReturn(false);
        when(service.getKuraWebEnabled()).thenReturn(PROPERTIES_FALSE_STRING_VALUE);
        
        Properties kuraProperties = new Properties();
        kuraProperties.put(SystemService.KEY_KURA_HAVE_NET_ADMIN, false);
        when(service.getProperties()).thenReturn(kuraProperties);
    }

    private static void initExtendedProperties(SystemService service) {
        ExtendedProperties extendedProperties = mock(ExtendedProperties.class);
        
        List<ExtendedPropertyGroup> groups = new ArrayList<>();
        groups.add(createExtendedPropertyGroup("group1"));
        groups.add(createExtendedPropertyGroup("group2"));

        when(extendedProperties.getVersion()).thenReturn(EXT_PROPERTIES_VERSION);
        when(extendedProperties.getPropertyGroups()).thenReturn(groups);
        when(service.getExtendedProperties()).thenReturn(Optional.of(extendedProperties));
    }

    private static ExtendedPropertyGroup createExtendedPropertyGroup(String name) {
        ExtendedPropertyGroup group = mock(ExtendedPropertyGroup.class);

        Map<String, String> propertiesGroup = new HashMap<>();
        propertiesGroup.put("key1", "val1");
        propertiesGroup.put("key2", "val2");

        when(group.getName()).thenReturn(name);
        when(group.getProperties()).thenReturn(propertiesGroup);

        return group;
    }

    private static void initExceptions(SystemService service) {
        Mockito.when(service.getBundles()).thenThrow(RuntimeException.class);

        when(service.getBiosVersion()).thenThrow(RuntimeException.class);
        when(service.getCommandUser()).thenThrow(RuntimeException.class);
        when(service.getCpuVersion()).thenThrow(RuntimeException.class);
        when(service.getDeviceName()).thenThrow(RuntimeException.class);
        when(service.getFileSeparator()).thenThrow(RuntimeException.class);
        when(service.getFirmwareVersion()).thenThrow(RuntimeException.class);
        when(service.getHostname()).thenThrow(RuntimeException.class);
        when(service.getJavaHome()).thenThrow(RuntimeException.class);
        when(service.getJavaVendor()).thenThrow(RuntimeException.class);
        when(service.getJavaVersion()).thenThrow(RuntimeException.class);
        when(service.getJavaVmInfo()).thenThrow(RuntimeException.class);
        when(service.getJavaVmName()).thenThrow(RuntimeException.class);
        when(service.getJavaVmVersion()).thenThrow(RuntimeException.class);
        when(service.getKuraDataDirectory()).thenThrow(RuntimeException.class);
        when(service.getKuraFrameworkConfigDirectory()).thenThrow(RuntimeException.class);
        when(service.getKuraHome()).thenThrow(RuntimeException.class);
        when(service.getKuraMarketplaceCompatibilityVersion()).thenThrow(RuntimeException.class);
        when(service.getKuraSnapshotsDirectory()).thenThrow(RuntimeException.class);
        when(service.getKuraStyleDirectory()).thenThrow(RuntimeException.class);
        when(service.getKuraTemporaryConfigDirectory()).thenThrow(RuntimeException.class);
        when(service.getKuraUserConfigDirectory()).thenThrow(RuntimeException.class);
        when(service.getKuraVersion()).thenThrow(RuntimeException.class);
        when(service.getModelId()).thenThrow(RuntimeException.class);
        when(service.getModelName()).thenThrow(RuntimeException.class);
        when(service.getNetVirtualDevicesConfig()).thenThrow(RuntimeException.class);
        when(service.getOsArch()).thenThrow(RuntimeException.class);
        when(service.getOsDistro()).thenThrow(RuntimeException.class);
        when(service.getOsDistroVersion()).thenThrow(RuntimeException.class);
        when(service.getOsgiFwName()).thenThrow(RuntimeException.class);
        when(service.getOsgiFwVersion()).thenThrow(RuntimeException.class);
        when(service.getOsName()).thenThrow(RuntimeException.class);
        when(service.getOsVersion()).thenThrow(RuntimeException.class);
        when(service.getPartNumber()).thenThrow(RuntimeException.class);
        when(service.getPlatform()).thenThrow(RuntimeException.class);
        when(service.getPrimaryMacAddress()).thenThrow(RuntimeException.class);
        when(service.getPrimaryNetworkInterfaceName()).thenThrow(RuntimeException.class);
        when(service.getSerialNumber()).thenThrow(RuntimeException.class);
        when(service.getTotalMemory()).thenThrow(RuntimeException.class);
        when(service.getFreeMemory()).thenThrow(RuntimeException.class);
        when(service.getKuraSnapshotsCount()).thenThrow(RuntimeException.class);
        when(service.getKuraWifiTopChannel()).thenThrow(RuntimeException.class);
        when(service.getNumberOfProcessors()).thenThrow(RuntimeException.class);
        when(service.getFileCommandZipMaxUploadNumber()).thenThrow(RuntimeException.class);
        when(service.getFileCommandZipMaxUploadSize()).thenThrow(RuntimeException.class);
        when(service.isLegacyBluetoothBeaconScan()).thenThrow(RuntimeException.class);
        when(service.isLegacyPPPLoggingEnabled()).thenThrow(RuntimeException.class);
        when(service.getKuraWebEnabled()).thenThrow(RuntimeException.class);

        when(service.getExtendedProperties()).thenThrow(RuntimeException.class);
    }

}
