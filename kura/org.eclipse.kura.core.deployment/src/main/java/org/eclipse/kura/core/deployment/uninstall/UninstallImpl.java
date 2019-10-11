/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
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
import java.util.function.Consumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.UninstallStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UninstallImpl {

    private static final Logger logger = LoggerFactory.getLogger(UninstallImpl.class);

    public static final String RESOURCE_UNINSTALL = "uninstall";

    private final CloudDeploymentHandlerV2 callback;
    private final DeploymentAdmin deploymentAdmin;
    private final CommandExecutorService executorService;

    public UninstallImpl(CloudDeploymentHandlerV2 callback, DeploymentAdmin deploymentAdmin,
            CommandExecutorService executorService) {
        this.callback = callback;
        this.deploymentAdmin = deploymentAdmin;
        this.executorService = executorService;
    }

    private void uninstallCompleteAsync(DeploymentPackageUninstallOptions options, String dpName) {
        KuraUninstallPayload notify = new KuraUninstallPayload(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setUninstallStatus(UninstallStatus.COMPLETED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setUninstallProgress(100);

        this.callback.publishMessage(options, notify, RESOURCE_UNINSTALL);
    }

    public void uninstallFailedAsync(DeploymentPackageUninstallOptions options, String dpName, Exception e) {
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
                DeploymentPackage dp = this.deploymentAdmin.getDeploymentPackage(name);
                if (dp != null) {
                    dp.uninstall();
                    String sUrl = CloudDeploymentHandlerV2.installImplementation.getDeployedPackages()
                            .getProperty(name);
                    File dpFile = new File(new URL(sUrl).getPath());
                    if (!dpFile.delete()) {
                        logger.warn("Cannot delete file at URL: {}", sUrl);
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

    private void deviceReboot(DeploymentPackageUninstallOptions options) {
        if (options.isReboot()) {
            try {
                logger.info("Reboot requested...");
                int delay = options.getRebootDelay();
                logger.info("Sleeping for {} ms.", delay);
                Thread.sleep(delay);
                logger.info("Rebooting...");
                Consumer<CommandStatus> commandCallback = status -> {
                    // Do nothing...
                };
                this.executorService.execute(new Command(new String[] { "reboot" }), commandCallback);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Failed to reboot system", e);
            }
        }
    }

}
