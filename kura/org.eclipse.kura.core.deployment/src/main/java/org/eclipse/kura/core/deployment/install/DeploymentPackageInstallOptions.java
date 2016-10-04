/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.deployment.install;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;

public class DeploymentPackageInstallOptions extends DeploymentPackageOptions {

    public static final String METRIC_DP_INSTALL_SYSTEM_UPDATE = "dp.install.system.update";
    public static final String METRIC_INSTALL_VERIFIER_URI = "dp.install.verifier.uri";

    private Boolean systemUpdate = false;
    private String verifierURI = null;

    // Metrics in RESOURCE_INSTALL
    public DeploymentPackageInstallOptions(String dpName, String dpVersion) {
        super(dpName, dpVersion);
    }

    public DeploymentPackageInstallOptions(KuraPayload request) throws KuraException {

        super(null, null);

        super.setDpName((String) request.getMetric(METRIC_DP_NAME));
        if (super.getDpName() == null) {
            throw new KuraInvalidMessageException("Missing deployment package name!");
        }

        super.setDpVersion((String) request.getMetric(METRIC_DP_VERSION));
        if (super.getDpVersion() == null) {
            throw new KuraInvalidMessageException("Missing deployment package version!");
        }

        super.setJobId((Long) request.getMetric(METRIC_JOB_ID));
        if (super.getJobId() == null) {
            throw new KuraInvalidMessageException("Missing jobId!");
        }

        setSystemUpdate((Boolean) request.getMetric(METRIC_DP_INSTALL_SYSTEM_UPDATE));
        if (getSystemUpdate() == null) {
            throw new KuraInvalidMessageException("Missing System Update!");
        }

        try {
            Object metric = request.getMetric(METRIC_DP_REBOOT);
            if (metric != null) {
                super.setReboot((Boolean) metric);
            }
            metric = request.getMetric(METRIC_DP_REBOOT_DELAY);
            if (metric != null) {
                super.setRebootDelay((Integer) metric);
            }

            metric = request.getMetric(KuraRequestPayload.REQUESTER_CLIENT_ID);
            if (metric != null) {
                super.setRequestClientId((String) metric);
            }

            metric = request.getMetric(METRIC_INSTALL_VERIFIER_URI);
            if (metric != null) {
                setVerifierURI((String) metric);
            }

        } catch (Exception ex) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
        }
    }

    public void setSystemUpdate(Boolean systemUpdate) {
        this.systemUpdate = systemUpdate;
    }

    public Boolean getSystemUpdate() {
        return this.systemUpdate;
    }

    public void setVerifierURI(String verifierURI) {
        this.verifierURI = verifierURI;
    }

    public String getVerifierURL() {
        return this.verifierURI;
    }
}
