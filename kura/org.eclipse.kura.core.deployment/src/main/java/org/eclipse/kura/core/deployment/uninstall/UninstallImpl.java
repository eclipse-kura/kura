/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.deployment.uninstall;

import java.io.File;
import java.net.URL;
import java.util.Date;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.UninstallStatus;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UninstallImpl {

    private static final Logger s_logger = LoggerFactory.getLogger(UninstallImpl.class);

    public static final String RESOURCE_UNINSTALL = "uninstall";

    private final CloudDeploymentHandlerV2 callback;
    private final DeploymentAdmin m_deploymentAdmin;

    public UninstallImpl(CloudDeploymentHandlerV2 callback, DeploymentAdmin deploymentAdmin) {
        this.callback = callback;
        this.m_deploymentAdmin = deploymentAdmin;
    }

    private void uninstallCompleteAsync(DeploymentPackageUninstallOptions options, String dpName) throws KuraException {
        KuraUninstallPayload notify = new KuraUninstallPayload(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setUninstallStatus(UninstallStatus.COMPLETED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setUninstallProgress(100);

        this.callback.publishMessage(options, notify, RESOURCE_UNINSTALL);
    }

    public void uninstallFailedAsync(DeploymentPackageUninstallOptions options, String dpName, Exception e)
            throws KuraException {
        KuraUninstallPayload notify = new KuraUninstallPayload(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setUninstallStatus(UninstallStatus.FAILED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setUninstallProgress(0);
        if (e != null) {
            notify.setErrorMessage(e.getMessage());
        }
        this.callback.publishMessage(options, notify, RESOURCE_UNINSTALL);
    }

    public void uninstaller(DeploymentPackageUninstallOptions options, String packageName) throws KuraException {
        try {
            String name = packageName;
            if (name != null) {
                DeploymentPackage dp = this.m_deploymentAdmin.getDeploymentPackage(name);
                if (dp != null) {
                    dp.uninstall();
                    String sUrl = CloudDeploymentHandlerV2.installImplementation.getDeployedPackages()
                            .getProperty(name);
                    File dpFile = new File(new URL(sUrl).getPath());
                    if (!dpFile.delete()) {
                        s_logger.warn("Cannot delete file at URL: {}", sUrl);
                    }
                    CloudDeploymentHandlerV2.installImplementation.removePackageFromConfFile(name);
                }
                uninstallCompleteAsync(options, name);

                // Reboot?
                deviceReboot(options);
            }
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    private static void deviceReboot(DeploymentPackageUninstallOptions options) {
        if (options.isReboot()) {
            s_logger.info("Reboot requested...");
            SafeProcess proc = null;
            try {
                int delay = options.getRebootDelay();
                s_logger.info("Sleeping for {} ms.", delay);
                Thread.sleep(delay);
                s_logger.info("Rebooting...");
                proc = ProcessUtil.exec("reboot");
            } catch (Exception e) {
                s_logger.info("Rebooting... Failure!");
            } finally {
                if (proc != null) {
                    ProcessUtil.destroy(proc);
                }
            }
        }
    }

}
