/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.configuration.test;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CfgSvcTestComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(CfgSvcTestComponent.class);

    private void updated(Map<String, Object> properties) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (Entry<String, Object> entry : properties.entrySet()) {
                sb.append("[").append(entry.getKey()).append("=").append(entry.getValue()).append("], ");
            }

            logger.debug("Properties after update: " + sb.toString());
        }
    }
}
