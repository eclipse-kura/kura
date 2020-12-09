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

import org.eclipse.kura.executor.CommandExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ce910ModemDriver extends OptionModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(Ce910ModemDriver.class);
    private static final String VENDOR = "1bc7";
    private static final String PRODUCT = "1011";

    public Ce910ModemDriver() {
        super(VENDOR, PRODUCT);
    }

    @Override
    public int install(CommandExecutorService executorService) {
        logger.info("Installing {} driver for Telit CE910 modem", getName());
        return super.install(executorService);
    }
}
