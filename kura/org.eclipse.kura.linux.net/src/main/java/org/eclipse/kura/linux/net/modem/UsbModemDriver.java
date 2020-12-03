/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.modem;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.linux.net.modem.GatewayModemDriver;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(UsbModemDriver.class);

    private final String name;
    private final String vendor;
    private final String product;

    public UsbModemDriver(String name, String vendor, String product) {
        this.name = name;
        this.vendor = vendor;
        this.product = product;
    }

    public int install(CommandExecutorService executorService) {
        logger.info("installing driver: {}", this.name);
        return manageDriver(executorService, "modprobe");
    }

    public int remove(CommandExecutorService executorService) {
        logger.info("removing driver: {}", this.name);
        return manageDriver(executorService, "rmmod");
    }

    public String getName() {
        return this.name;
    }

    public String getVendor() {
        return this.vendor;
    }

    public String getProduct() {
        return this.product;
    }

    @Deprecated
    public void enable() throws KuraException {
        BundleContext context = FrameworkUtil.getBundle(UsbModemDriver.class).getBundleContext();
        ServiceReference<GatewayModemDriver>[] serviceReferences = ServiceUtil.getServiceReferences(context,
                GatewayModemDriver.class, null);

        // There will be at maximum one per gateway
        for (ServiceReference<GatewayModemDriver> reference : serviceReferences) {
            GatewayModemDriver gatewayModemDriver = context.getService(reference);
            gatewayModemDriver.enable(this.vendor, this.product);

            context.ungetService(reference);
        }
    }

    public void enable(UsbModemDevice device) throws KuraException {
        BundleContext context = FrameworkUtil.getBundle(UsbModemDriver.class).getBundleContext();
        ServiceReference<GatewayModemDriver>[] serviceReferences = ServiceUtil.getServiceReferences(context,
                GatewayModemDriver.class, null);

        // There will be at maximum one per gateway
        for (ServiceReference<GatewayModemDriver> reference : serviceReferences) {
            GatewayModemDriver gatewayModemDriver = context.getService(reference);
            gatewayModemDriver.enable(device);

            context.ungetService(reference);
        }
    }

    @Deprecated
    public void disable() throws KuraException {
        BundleContext context = FrameworkUtil.getBundle(UsbModemDriver.class).getBundleContext();
        ServiceReference<GatewayModemDriver>[] serviceReferences = ServiceUtil.getServiceReferences(context,
                GatewayModemDriver.class, null);

        // There will be at maximum one per gateway
        for (ServiceReference<GatewayModemDriver> reference : serviceReferences) {
            GatewayModemDriver gatewayModemDriver = context.getService(reference);
            gatewayModemDriver.disable(this.vendor, this.product);

            context.ungetService(reference);
        }
    }

    public void disable(UsbModemDevice device) throws KuraException {
        BundleContext context = FrameworkUtil.getBundle(UsbModemDriver.class).getBundleContext();
        ServiceReference<GatewayModemDriver>[] serviceReferences = ServiceUtil.getServiceReferences(context,
                GatewayModemDriver.class, null);

        // There will be at maximum one per gateway
        for (ServiceReference<GatewayModemDriver> reference : serviceReferences) {
            GatewayModemDriver gatewayModemDriver = context.getService(reference);
            gatewayModemDriver.disable(device);

            context.ungetService(reference);
        }
    }

    @Deprecated
    public void reset() throws KuraException {
        BundleContext context = FrameworkUtil.getBundle(UsbModemDriver.class).getBundleContext();
        ServiceReference<GatewayModemDriver>[] serviceReferences = ServiceUtil.getServiceReferences(context,
                GatewayModemDriver.class, null);

        // There will be at maximum one per gateway
        for (ServiceReference<GatewayModemDriver> reference : serviceReferences) {
            GatewayModemDriver gatewayModemDriver = context.getService(reference);
            gatewayModemDriver.reset(this.vendor, this.product);

            context.ungetService(reference);
        }
    }

    public void reset(UsbModemDevice device) throws KuraException {
        BundleContext context = FrameworkUtil.getBundle(UsbModemDriver.class).getBundleContext();
        ServiceReference<GatewayModemDriver>[] serviceReferences = ServiceUtil.getServiceReferences(context,
                GatewayModemDriver.class, null);

        // There will be at maximum one per gateway
        for (ServiceReference<GatewayModemDriver> reference : serviceReferences) {
            GatewayModemDriver gatewayModemDriver = context.getService(reference);
            gatewayModemDriver.reset(device);

            context.ungetService(reference);
        }
    }

    private Integer manageDriver(CommandExecutorService executorService, String command) {
        return executorService.execute(new Command(new String[] { command, this.name })).getExitStatus().getExitCode();
    }
}
