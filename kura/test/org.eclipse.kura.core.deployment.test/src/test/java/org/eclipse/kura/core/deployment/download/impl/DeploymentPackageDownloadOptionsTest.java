/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment.download.impl;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.hook.DeploymentHookManager;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class DeploymentPackageDownloadOptionsTest {

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullDeployUri() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullName() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullVersion() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullProtocol() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullJobId() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, null);

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNoJobId() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullUpdate() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 123L);

        new DeploymentPackageDownloadOptions(request, deploymentHookManager, "/tmp");
    }
}
