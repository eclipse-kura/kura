/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment.install;

import static org.junit.Assert.assertEquals;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.core.deployment.hook.DeploymentHookManager;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.junit.Test;

public class DeploymentPackageInstallOptionsTest {


    @Test
    public void testCreateOKJobId() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "ver");
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 123L);

        DeploymentPackageInstallOptions options = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");

        assertEquals(123L, (long) options.getJobId());
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNullJobId() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "ver");
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, null);

        new DeploymentPackageInstallOptions(request, deploymentHookManager, "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testCreateNoJobId() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "ver");
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        new DeploymentPackageInstallOptions(request, deploymentHookManager, "/tmp");
    }

    @Test
    public void testConstructorOK() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "name");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "ver");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        request.addMetric(DeploymentPackageOptions.METRIC_DP_REBOOT, false);
        request.addMetric(DeploymentPackageOptions.METRIC_DP_REBOOT_DELAY, 10);
        request.addMetric(KuraRequestPayload.REQUESTER_CLIENT_ID, "client");
        request.addMetric(DeploymentPackageInstallOptions.METRIC_INSTALL_VERIFIER_URI, "url");

        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");

        assertEquals("name", opt.getDpName());
        assertEquals("ver", opt.getDpVersion());
        assertEquals(1234, (long) opt.getJobId());
        assertEquals(false, opt.getSystemUpdate());
        assertEquals(false, opt.isReboot());
        assertEquals(10, opt.getRebootDelay());
        assertEquals("client", opt.getRequestClientId());
        assertEquals("url", opt.getVerifierURL());
    }

    @Test
    public void testConstructorOKMini() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testConstructorError() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();
        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testConstructorErrorName() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testConstructorErrorVersion() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testConstructorErrorJob() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");
    }

    @Test(expected = KuraInvalidMessageException.class)
    public void testConstructorErrorUpdate() throws KuraException {
        final DeploymentHookManager deploymentHookManager = new DeploymentHookManager();

        KuraPayload request = new KuraPayload();

        request.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        request.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        request.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);

        DeploymentPackageInstallOptions opt = new DeploymentPackageInstallOptions(request, deploymentHookManager,
                "/tmp");
    }
}
