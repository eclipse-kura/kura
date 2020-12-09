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
 *******************************************************************************/
package org.eclipse.kura.core.db;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.H2DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DbHelper {

    private static final Logger logger = LoggerFactory.getLogger(H2DbHelper.class);

    private static final String H2_DB_SERVICE_FACTORY_PID = "org.eclipse.kura.core.db.H2DbService";

    private ConfigurationService configurationService;

    protected void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    private void startDefaultDbServiceInstance() {
        try {
            if (this.configurationService.getComponentConfiguration(H2DbService.DEFAULT_INSTANCE_PID) == null) {
                logger.info("Default H2DbService instance configuration not found, creating new instance...");
                this.configurationService.createFactoryConfiguration(H2_DB_SERVICE_FACTORY_PID,
                        H2DbService.DEFAULT_INSTANCE_PID, null, true);
            } else {
                logger.info("Default H2DbService instance configuration found");
            }
        } catch (KuraException e) {
            logger.error("Failed to retrieve or create default H2DbService factory configuration", e);
        }
    }

    private void stopDefaultDbServiceInstance() {
        try {
            if (this.configurationService.getComponentConfiguration(H2DbService.DEFAULT_INSTANCE_PID) != null) {
                this.configurationService.deleteFactoryConfiguration(H2DbService.DEFAULT_INSTANCE_PID, false);
            }
        } catch (KuraException e) {
            logger.error("Failed to remove default H2DbService instance", e);
        }
    }

    protected void activate() {
        logger.info("activating...");
        startDefaultDbServiceInstance();
        logger.info("activating...done");
    }

    protected void deactivate() {
        stopDefaultDbServiceInstance();
    }
}
