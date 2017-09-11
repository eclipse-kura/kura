/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbModemDriver extends ModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(UsbModemDriver.class);

    private final String name;
    private final String vendor;
    private final String product;

    public UsbModemDriver(String name, String vendor, String product) {
        this.name = name;
        this.vendor = vendor;
        this.product = product;
    }

    public int install() throws Exception {
        logger.info("installing driver: {}", this.name);
        return LinuxProcessUtil.start("modprobe " + this.name, true);
    }

    public int remove() throws Exception {
        logger.info("removing driver: {}", this.name);
        return LinuxProcessUtil.start("rmmod " + this.name, true);
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
}
