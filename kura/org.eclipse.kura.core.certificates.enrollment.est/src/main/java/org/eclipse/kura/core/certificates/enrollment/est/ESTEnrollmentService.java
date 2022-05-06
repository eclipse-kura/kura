/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.certificates.enrollment.est;

import java.security.cert.Certificate;
import java.util.Map;

import org.eclipse.kura.certificate.enrollment.EnrollmentService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESTEnrollmentService implements EnrollmentService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ESTEnrollmentService.class);

    protected void activate(Map<String, Object> properties) {
        logger.info("activating...");
    }

    protected void deactivate() {
        logger.info("deactivating...");
    }

    protected void updated(Map<String, Object> properties) {
        logger.info("updating...");
    }

    @Override
    public void enroll() {
        // TODO Auto-generated method stub

    }

    @Override
    public void renew() {
        // TODO Auto-generated method stub

    }

    @Override
    public Certificate getCARoot() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void forceCARootRollover() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEnrolled() {
        // TODO Auto-generated method stub
        return false;
    }

}
