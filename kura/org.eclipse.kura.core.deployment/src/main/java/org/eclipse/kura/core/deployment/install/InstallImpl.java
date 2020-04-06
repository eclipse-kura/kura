/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.core.deployment.InstallStatus;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallImpl {

    private static final Logger logger = LoggerFactory.getLogger(InstallImpl.class);

    private static final int PROGRESS_COMPLETE = 100;
    private static final String MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED = "Configuration file not specified";

    public static final String RESOURCE_INSTALL = "install";

    public static final String PERSISTANCE_SUFFIX = "_persistance";
    public static final String PERSISTANCE_FOLDER_NAME = "persistance";
    public static final String PERSISTANCE_VERIFICATION_FOLDER_NAME = "verification";
    public static final String PERSISTANCE_FILE_NAME = "persistance.file.name";

    private DeploymentPackageInstallOptions options;
    private final CloudDeploymentHandlerV2 callback;
    private DeploymentAdmin deploymentAdmin;
    private Properties installPersistance;
    private String dpaConfPath;
    private final String installVerifDir;
    private final String installPersistanceDir;
    private String packagesPath;
    private CommandExecutorService executorService;

    public InstallImpl(CloudDeploymentHandlerV2 callback, String kuraDataDir, CommandExecutorService executorService) {
        this.callback = callback;

        StringBuilder pathSB = new StringBuilder();
        pathSB.append(kuraDataDir);
        pathSB.append(File.separator);
        pathSB.append(PERSISTANCE_FOLDER_NAME);
        this.installPersistanceDir = pathSB.toString();
        File installPersistanceDirFile = new File(this.installPersistanceDir);
        if (!installPersistanceDirFile.exists()) {
            installPersistanceDirFile.mkdir();
        }

        pathSB = new StringBuilder();
        pathSB.append(this.installPersistanceDir);
        pathSB.append(File.separator);
        pathSB.append(PERSISTANCE_VERIFICATION_FOLDER_NAME);
        this.installVerifDir = pathSB.toString();
        File installVerificationDir = new File(this.installVerifDir);
        if (!installVerificationDir.exists()) {
            installVerificationDir.mkdir();
        }

        this.executorService = executorService;
    }

    public String getVerificationDirectory() {
        return this.installVerifDir;
    }

    public Properties getDeployedPackages() {
        Properties deployedPackages = new Properties();
        try (FileInputStream fis = new FileInputStream(this.dpaConfPath);) {
            deployedPackages.load(fis);
        } catch (IOException e) {
            logger.error("Error opening package configuration file", e);
        }
        return deployedPackages;
    }

    public void setOptions(DeploymentPackageInstallOptions options) {
        this.options = options;
    }

    public void setPackagesPath(String packagesPath) {
        this.packagesPath = packagesPath;
    }

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    public void setDpaConfPath(String dpaConfPath) {
        this.dpaConfPath = dpaConfPath;
    }

    public void installDp(DeploymentPackageInstallOptions options, File dpFile) {
        try {
            installDeploymentPackageInternal(dpFile);
            installCompleteAsync(options, dpFile.getName());
            logger.info("Install completed!");

            if (options.isReboot()) {
                Thread.sleep(options.getRebootDelay());
                Consumer<CommandStatus> commandCallback = status -> {
                    // Do nothing...
                };
                this.executorService.execute(new Command(new String[] { "reboot" }), commandCallback);
            }
        } catch (Exception e) {
            logger.info("Install failed!");
            installFailedAsync(options, dpFile.getName(), e);
        }
    }

    public void installSh(DeploymentPackageOptions options, File shFile) throws KuraException {

        updateInstallPersistance(shFile.getName(), options);

        // Script Exec
        CommandStatus status;
        String shFilePath;
        try {
            shFilePath = shFile.getCanonicalPath();
        } catch (IOException e) {
            throw new KuraProcessExecutionErrorException(e, "Failed to get script path");
        }

        Command command = new Command(new String[] { "chmod", "700", shFilePath });
        command.setTimeout(60);
        status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraProcessExecutionErrorException("Failed to change file mode");
        }

        status = this.executorService.execute(new Command(new String[] { shFilePath }));
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraProcessExecutionErrorException("Failed to execute script");
        }

    }

    public void installInProgressSyncMessage(KuraResponsePayload respPayload) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(KuraInstallPayload.METRIC_INSTALL_STATUS, InstallStatus.IN_PROGRESS.getStatusString());
        respPayload.addMetric(KuraInstallPayload.METRIC_DP_NAME, this.options.getDpName());
        respPayload.addMetric(KuraInstallPayload.METRIC_DP_VERSION, this.options.getDpVersion());
    }

    public void installIdleSyncMessage(KuraResponsePayload respPayload) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(KuraInstallPayload.METRIC_INSTALL_STATUS, InstallStatus.IDLE.getStatusString());
    }

    public void installCompleteAsync(DeploymentPackageOptions options, String dpName) {
        KuraInstallPayload notify = new KuraInstallPayload(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setInstallStatus(InstallStatus.COMPLETED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setInstallProgress(PROGRESS_COMPLETE);

        this.callback.publishMessage(options, notify, RESOURCE_INSTALL);
    }

    public void installFailedAsync(DeploymentPackageInstallOptions options, String dpName, Exception e) {
        KuraInstallPayload notify = new KuraInstallPayload(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setInstallStatus(InstallStatus.FAILED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setInstallProgress(0);
        if (e != null) {
            notify.setErrorMessage(e.getMessage());
        }

        this.callback.publishMessage(options, notify, RESOURCE_INSTALL);
    }

    public void sendInstallConfirmations(String notificationPublisherPid,
            CloudNotificationPublisher notificationPublisher) {
        logger.info("Ready to send Confirmations");

        File verificationDir = new File(this.installVerifDir);
        if (verificationDir.listFiles() != null) {

            for (File fileEntry : verificationDir.listFiles()) {
                if (fileEntry.isFile() && (fileEntry.getName().endsWith(".sh") || fileEntry.getName().endsWith(".bat"))
                        && isCorrectNotificationPublisher(notificationPublisherPid, fileEntry.getName())) {
                    runScriptAndNotify(notificationPublisher, fileEntry);

                }
            }
        }
    }

    private void runScriptAndNotify(CloudNotificationPublisher notificationPublisher, File fileEntry) {
        logger.info("running verifier {}", fileEntry);

        CommandStatus status;
        String fileEntryPath = "";
        try {
            fileEntryPath = fileEntry.getCanonicalPath();
        } catch (IOException e) {
            logger.error("Failed to get script path");
        }

        Command command = new Command(new String[] { "chmod", "700", fileEntryPath });
        command.setTimeout(60);
        status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            logger.error("Failed to change file mode");
        }

        String[] commandLine = { fileEntryPath };
        try {
            status = this.executorService.execute(new Command(commandLine));
            if (status.getExitStatus().isSuccessful()) {
                sendSysUpdateSuccess(fileEntry.getName(), notificationPublisher);
            } else {
                sendSysUpdateFailure(fileEntry.getName(), notificationPublisher);
            }
        } catch (final Exception e) {
            logger.warn("failed to publish completion status", e);
        } finally {
            this.executorService.kill(commandLine, LinuxSignal.SIGKILL);
            try {
                Files.delete(fileEntry.toPath());
            } catch (IOException e) {
                logger.error("Cannot delete file {}", fileEntry, e);
            }
        }
    }

    private DeploymentPackage installDeploymentPackageInternal(File fileReference)
            throws DeploymentException, IOException {

        DeploymentPackage dp = null;
        File dpPersistentFile = null;
        File downloadedFile = fileReference;

        try (InputStream dpInputStream = new FileInputStream(downloadedFile);) {
            String dpBasename = fileReference.getName();
            StringBuilder pathSB = new StringBuilder();
            pathSB.append(this.packagesPath);
            pathSB.append(File.separator);
            pathSB.append(dpBasename);
            String dpPersistentFilePath = pathSB.toString();
            dpPersistentFile = new File(dpPersistentFilePath);

            dp = this.deploymentAdmin.installDeploymentPackage(dpInputStream);

            // Now we need to copy the deployment package file to the Kura
            // packages directory unless it's already there.

            if (!downloadedFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                logger.debug("dpFile.getCanonicalPath(): {}", downloadedFile.getCanonicalPath());
                logger.debug("dpPersistentFile.getCanonicalPath(): {}", dpPersistentFile.getCanonicalPath());
                FileUtils.copyFile(downloadedFile, dpPersistentFile);
                addPackageToConfFile(dp.getName(), "file:" + dpPersistentFilePath);
            }
        } catch (IOException ex) {

        } finally {
            // The file from which we have installed the deployment package will be deleted
            // unless it's a persistent deployment package file.
            if (dpPersistentFile != null
                    && !downloadedFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                downloadedFile.delete();
            }
        }

        return dp;
    }

    private void updateInstallPersistance(String fileName, DeploymentPackageOptions options) {
        this.installPersistance = new Properties();
        this.installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_CLIENT_ID, options.getClientId());
        this.installPersistance.setProperty(DeploymentPackageOptions.METRIC_JOB_ID, Long.toString(options.getJobId()));
        this.installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_NAME, fileName);
        this.installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_VERSION, options.getDpVersion());
        this.installPersistance.setProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID,
                options.getRequestClientId());
        this.installPersistance.setProperty(PERSISTANCE_FILE_NAME, fileName);
        this.installPersistance.setProperty(DeploymentPackageOptions.NOTIFICATION_PUBLISHER_PID_KEY,
                options.getNotificationPublisherPid());

        if (this.installPersistanceDir == null) {
            logger.warn(MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED);
            return;
        }

        StringBuilder pathSB = new StringBuilder();
        pathSB.append(this.installPersistanceDir);
        pathSB.append(File.separator);
        pathSB.append(fileName);
        pathSB.append(PERSISTANCE_SUFFIX);
        String persistanceFile = pathSB.toString();
        try (FileOutputStream fos = new FileOutputStream(persistanceFile);) {
            this.installPersistance.store(fos, null);
            fos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            logger.error("Error writing remote install configuration file", e);
        }
    }

    private void addPackageToConfFile(String packageName, String packageUrl) {
        Properties deployedPackages = getDeployedPackages();
        deployedPackages.setProperty(packageName, packageUrl);

        if (this.dpaConfPath == null) {
            logger.warn(MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED);
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(this.dpaConfPath);) {
            deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            logger.error("Error writing package configuration file", e);
        }
    }

    public void removePackageFromConfFile(String packageName) {
        Properties deployedPackages = getDeployedPackages();
        deployedPackages.remove(packageName);

        if (this.dpaConfPath == null) {
            logger.warn(MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED);
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(this.dpaConfPath)) {
            deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            logger.error("Error writing package configuration file", e);
        }
    }

    private void sendSysUpdateSuccess(String verificationFileName, CloudNotificationPublisher notificationPublisher)
            throws IOException {
        logger.info("Publishing success notification for verifier {}...", verificationFileName);

        final File fileEntry = findPersistenceFileFromVerificationFile(verificationFileName);

        Properties downloadProperties = loadInstallPersistance(fileEntry);
        String deployUrl = downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI);
        String dpName = downloadProperties.getProperty(DeploymentPackageOptions.METRIC_DP_NAME);
        String dpVersion = downloadProperties.getProperty(DeploymentPackageOptions.METRIC_DP_VERSION);
        String clientId = downloadProperties.getProperty(DeploymentPackageOptions.METRIC_DP_CLIENT_ID);
        Long jobId = Long.valueOf(downloadProperties.getProperty(DeploymentPackageOptions.METRIC_JOB_ID));
        String fileSystemFileName = downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
        String requestClientId = downloadProperties.getProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID);

        String notificationPid = downloadProperties
                .getProperty(DeploymentPackageOptions.NOTIFICATION_PUBLISHER_PID_KEY);

        DeploymentPackageDownloadOptions deployOptions = new DeploymentPackageDownloadOptions(deployUrl, dpName,
                dpVersion);
        deployOptions.setClientId(clientId);
        deployOptions.setJobId(jobId);
        deployOptions.setRequestClientId(requestClientId);
        deployOptions.setNotificationPublisherPid(notificationPid);
        deployOptions.setNotificationPublisher(notificationPublisher);

        installCompleteAsync(deployOptions, fileSystemFileName);
        Files.delete(fileEntry.toPath());

        logger.info("Publishing success notification for verifier {}...done", verificationFileName);
    }

    private void sendSysUpdateFailure(String verificationFileName, CloudNotificationPublisher notificationPublisher)
            throws IOException {
        logger.info("Publishing failure notification for verifier {}...", verificationFileName);

        final File fileEntry = findPersistenceFileFromVerificationFile(verificationFileName);

        Properties downloadProperties = loadInstallPersistance(fileEntry);
        String deployUrl = downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI);
        String dpName = downloadProperties.getProperty(DeploymentPackageOptions.METRIC_DP_NAME);
        String dpVersion = downloadProperties.getProperty(DeploymentPackageOptions.METRIC_DP_VERSION);
        String clientId = downloadProperties.getProperty(DeploymentPackageOptions.METRIC_DP_CLIENT_ID);
        Long jobId = Long.valueOf(downloadProperties.getProperty(DeploymentPackageOptions.METRIC_JOB_ID));
        String fileSystemFileName = downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
        String requestClientId = downloadProperties.getProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID);

        String notificationPid = downloadProperties
                .getProperty(DeploymentPackageOptions.NOTIFICATION_PUBLISHER_PID_KEY);

        DeploymentPackageDownloadOptions deployOptions = new DeploymentPackageDownloadOptions(deployUrl, dpName,
                dpVersion);
        deployOptions.setClientId(clientId);
        deployOptions.setJobId(jobId);
        deployOptions.setRequestClientId(requestClientId);
        deployOptions.setNotificationPublisherPid(notificationPid);
        deployOptions.setNotificationPublisher(notificationPublisher);

        installFailedAsync(deployOptions, fileSystemFileName, new KuraException(KuraErrorCode.INTERNAL_ERROR));
        Files.delete(fileEntry.toPath());

        logger.info("Publishing failure notification for verifier {}...done", verificationFileName);
    }

    private File findPersistenceFileFromVerificationFile(final String verificationFileName)
            throws FileNotFoundException {
        String executableName = verificationFileName.split("_verifier")[0];
        File installDir = new File(this.installPersistanceDir);

        for (File fileEntry : installDir.listFiles()) {

            if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)
                    && fileEntry.getName().contains(executableName)) {
                return fileEntry;
            }
        }

        throw new FileNotFoundException("Cannot find persistance file for verification file " + verificationFileName);
    }

    private Properties loadInstallPersistance(File installedDpPersistance) {
        Properties downloadProperies = new Properties();
        try (FileReader fr = new FileReader(installedDpPersistance);) {
            downloadProperies.load(fr);
        } catch (IOException e) {
            logger.error("Exception loading install configuration file", e);
        }
        return downloadProperies;
    }

    public boolean isCorrectNotificationPublisher(String pid, String verificationFileName) {

        try (Stream<Path> filesStream = Files.list(Paths.get(this.installPersistanceDir))) {
            List<Path> availableFiles = filesStream.filter(Files::isRegularFile).filter(filePath -> {
                boolean isPersistanceFile = filePath.toFile().getName().endsWith(PERSISTANCE_SUFFIX);
                if (isPersistanceFile) {
                    Properties downloadProperties = loadInstallPersistance(filePath.toFile());
                    String notificationPid = downloadProperties
                            .getProperty(DeploymentPackageOptions.NOTIFICATION_PUBLISHER_PID_KEY);
                    String executableFileName = downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
                    if (pid.equals(notificationPid) && verificationFileName.contains(executableFileName)) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());

            boolean result = false;
            if (!availableFiles.isEmpty()) {
                result = true;
            }
            return result;
        } catch (IOException e) {
            logger.info("Unable to parse persistance dir");
        }
        return false;
    }
}
