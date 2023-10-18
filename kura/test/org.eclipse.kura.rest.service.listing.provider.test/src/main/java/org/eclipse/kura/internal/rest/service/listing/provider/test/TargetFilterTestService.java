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
package org.eclipse.kura.internal.rest.service.listing.provider.test;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetFilterTestService implements ConfigurableComponent, TestInterface, OtherTestInterface {

    private static final Logger logger = LoggerFactory.getLogger(TargetFilterTestService.class);

    public void activate() {
        logger.info("TargetFilterTestService activated");
    }

    public void update() {
    }

    public void deactivate() {
    }
}
