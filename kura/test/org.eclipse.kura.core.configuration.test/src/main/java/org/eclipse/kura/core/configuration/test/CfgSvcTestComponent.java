/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
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
