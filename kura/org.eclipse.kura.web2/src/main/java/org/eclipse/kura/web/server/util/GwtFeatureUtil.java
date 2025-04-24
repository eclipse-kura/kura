/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.util;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.command.PasswordCommandService;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.driver.descriptor.DriverDescriptorService;
import org.eclipse.kura.web.shared.model.GwtSupportedFeatures;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWiring;

public class GwtFeatureUtil {

    private GwtFeatureUtil() {
    }

    public static GwtSupportedFeatures getSupportedFeatures() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(GwtFeatureUtil.class).getBundleContext();

        final GwtSupportedFeatures result = new GwtSupportedFeatures();

        final Set<String> wiredPackages = getWiredPackages();

        result.setDriverServicesAvailable(isProviderServiceAvailable(DriverService.class, bundleContext)
                && isProviderServiceAvailable(DriverDescriptorService.class, bundleContext));

        result.setWiresServicesAvailable(isProviderServiceAvailable(WireGraphService.class, bundleContext)
                && isProviderServiceAvailable(WireComponentDefinitionService.class, bundleContext));

        result.setAssetAvailable(wiredPackages.contains("org.eclipse.kura.asset.provider")
                && wiredPackages.contains("org.eclipse.kura.internal.wire.asset"));

        result.setCommandServiceAvailable(isProviderServiceAvailable(PasswordCommandService.class, bundleContext));

        return result;
    }

    private static final Set<String> getWiredPackages() {
        final BundleWiring bundleWiring = FrameworkUtil.getBundle(GwtFeatureUtil.class).adapt(BundleWiring.class);

        return bundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE).stream()
                .map(w -> w.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))
                .filter(String.class::isInstance).map(String.class::cast).collect(Collectors.toSet());
    }

    private static boolean isProviderServiceAvailable(final Class<?> serviceInterface,
            final BundleContext bundleContext) {

        return bundleContext.getServiceReference(serviceInterface) != null;
    }

}
