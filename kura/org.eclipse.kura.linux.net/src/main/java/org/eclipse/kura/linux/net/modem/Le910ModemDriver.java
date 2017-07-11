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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Le910ModemDriver extends OptionModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(Le910ModemDriver.class);
    private static final String VENDOR = "1bc7";
    private static final String PRODUCT = "1201";

    public Le910ModemDriver() {
        super(VENDOR, PRODUCT);
    }

    @Override
    public int install() throws Exception {
        logger.info("Installing {} driver for Telit LE910 modem", getName());
        return super.install();
    }
}
