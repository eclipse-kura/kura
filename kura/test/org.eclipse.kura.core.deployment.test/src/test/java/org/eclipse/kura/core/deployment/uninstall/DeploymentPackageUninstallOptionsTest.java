/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment.uninstall;

import static org.junit.Assert.assertEquals;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class DeploymentPackageUninstallOptionsTest {

    @Test
    public void testCreateOKJobId() throws KuraException {
        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 123L);

        DeploymentPackageUninstallOptions options = new DeploymentPackageUninstallOptions(request);

        assertEquals(123L, (long) options.getJobId());
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullJobId() throws KuraException {
        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, null);

        new DeploymentPackageUninstallOptions(request);
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNoJobId() throws KuraException {
        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");

        new DeploymentPackageUninstallOptions(request);
    }
}
